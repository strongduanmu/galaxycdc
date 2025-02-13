/**
 * Copyright (c) 2013-2022, Alibaba Group Holding Limited;
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * </p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aliyun.polardbx.binlog.backup;

import com.aliyun.polardbx.binlog.BinlogUploadStatusEnum;
import com.aliyun.polardbx.binlog.CommonUtils;
import com.aliyun.polardbx.binlog.ConfigKeys;
import com.aliyun.polardbx.binlog.DynamicApplicationConfig;
import com.aliyun.polardbx.binlog.SpringContextHolder;
import com.aliyun.polardbx.binlog.dao.BinlogOssRecordDynamicSqlSupport;
import com.aliyun.polardbx.binlog.dao.BinlogOssRecordMapper;
import com.aliyun.polardbx.binlog.domain.po.BinlogOssRecord;
import com.aliyun.polardbx.binlog.BinlogFileUtil;
import com.aliyun.polardbx.binlog.filesys.CdcFile;
import com.aliyun.polardbx.binlog.filesys.LocalFileSystem;
import com.aliyun.polardbx.binlog.leader.RuntimeLeaderElector;
import com.aliyun.polardbx.binlog.monitor.MonitorManager;
import com.aliyun.polardbx.binlog.monitor.MonitorType;
import com.aliyun.polardbx.binlog.remote.RemoteBinlogProxy;
import com.aliyun.polardbx.binlog.remote.io.BinlogFetcher;
import com.aliyun.polardbx.binlog.remote.io.DataFileChecker;
import com.aliyun.polardbx.binlog.remote.io.IDataFetcher;
import com.aliyun.polardbx.binlog.remote.io.IFileCursorProvider;
import com.aliyun.polardbx.binlog.service.BinlogOssRecordService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.aliyun.polardbx.binlog.ConfigKeys.BINLOG_BACKUP_UPLOAD_MAX_THREAD_NUM;
import static com.aliyun.polardbx.binlog.SpringContextHolder.getObject;

/**
 * @author yudong
 * @since 2023/1/11
 *
 * 负责扫描binlog_oss_record表，确定需要上传的binlog文件列表
 * 采用多线程上传binlog文件（调用BinlogUploader进行实际上传动作）
 */
public class BinlogUploadManager implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(BinlogUploadManager.class);
    private final BinlogOssRecordMapper ossRecordMapper;
    /**
     * 提供流的最新cursor信息
     */
    private final IFileCursorProvider provider;
    /**
     * 本地产生的binlog文件的保存路径（不带group和stream）
     */
    private final String binlogRootPath;
    /**
     * dumper的名称，用于单流场景下判断该dumper是否是leader
     */
    private final String taskName;
    /**
     * group name
     */
    private final String group;
    /**
     * 这个dumper负责的所有流
     */
    private final List<String> streamList;
    /**
     * 用于保存各个流对应的LocalFileSystem
     */
    private final Map<String, LocalFileSystem> fileSystemMap;
    /**
     * 扫描线程，负责扫描binlog_oss_record，获得所有需要上传的本地文件列表
     */
    private final ExecutorService scanExecutor;
    /**
     * 上传线程池，负责多线程上传binlog文件
     */
    private final ThreadPoolExecutor uploadExecutor;
    /**
     * 正在上传中的文件列表，用于避免重复上传
     */
    private final Set<String> uploadingFiles;
    /**
     * key: stream name, value: MetricsObserver object
     */
    private final Map<String, MetricsObserver> metricsObserverMap;
    /**
     * BinlogUploader的状态
     */
    private volatile boolean runnable = true;

    public BinlogUploadManager(String binlogRootPath,
                               String taskName, String group, List<String> streamList,
                               Map<String, MetricsObserver> metricsObserverMap) {
        this.ossRecordMapper = SpringContextHolder.getObject(BinlogOssRecordMapper.class);
        this.provider = SpringContextHolder.getObject(IFileCursorProvider.class);
        this.binlogRootPath = binlogRootPath;
        this.taskName = taskName;
        this.uploadingFiles = new HashSet<>();
        this.group = group;
        this.streamList = streamList;
        this.metricsObserverMap = metricsObserverMap;

        fileSystemMap = new HashMap<>();
        for (String stream : streamList) {
            String fullPath = BinlogFileUtil.getBinlogFileFullPath(binlogRootPath, group, stream);
            LocalFileSystem localFileSystem = new LocalFileSystem(fullPath, group, stream);
            fileSystemMap.put(stream, localFileSystem);
        }

        this.scanExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "binlog-backup-upload-scan-thread");
            t.setDaemon(true);
            return t;
        });

        int maxThreadNum = DynamicApplicationConfig.getInt(BINLOG_BACKUP_UPLOAD_MAX_THREAD_NUM);
        this.uploadExecutor = new ThreadPoolExecutor(maxThreadNum, maxThreadNum, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("binlog-backup-upload-thread-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());
        this.uploadExecutor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void run() {
        logger.info("binlog uploader start to run");
        while (runnable) {
            try {
                // 每隔一段时间进行一轮扫描上传
                Thread.sleep(1000);
                if (noNeedToUpload()) {
                    continue;
                }
                List<BinlogOssRecord> recordList = getFilesToBeUploaded();
                if (logger.isDebugEnabled()) {
                    logger.debug("files that need to be uploaded:{}",
                        recordList.stream().map(BinlogOssRecord::getBinlogFile).collect(Collectors.toList()));
                }
                dispatchUploadJobs(recordList);
            } catch (Throwable e) {
                logger.error("binlog uploader meet an exception", e);
                alert("Scan Thread");
            }
        }
    }

    /**
     * 单流场景，slave不需要上传binlog，因为有mater干
     */
    private boolean noNeedToUpload() {
        return CommonUtils.isGlobalBinlog(group, streamList.get(0))
            && !RuntimeLeaderElector.isDumperLeader(taskName);
    }

    private void alert(String fileName) {
        MonitorManager.getInstance().triggerAlarm(MonitorType.BINLOG_BACKUP_UPLOAD_ERROR, fileName);
    }

    /**
     * 上传本地文件目录中有，并且binlog_oss_record表中状态非success的文件
     */
    private List<BinlogOssRecord> getFilesToBeUploaded() {
        List<BinlogOssRecord> result = new ArrayList<>();
        BinlogOssRecordService ossRecordService = getObject(BinlogOssRecordService.class);
        for (String stream : streamList) {
            LocalFileSystem fileSystem = fileSystemMap.get(stream);
            List<CdcFile> cdcFiles = fileSystem.listFiles();
            Set<String> localFiles =
                cdcFiles.stream().map(CdcFile::getName).collect(Collectors.toSet());
            List<BinlogOssRecord> recordsForUpload =
                ossRecordService.getRecordsForUpload(group, stream);
            List<BinlogOssRecord> partResult =
                recordsForUpload.stream().filter(r -> localFiles.contains(r.getBinlogFile())).collect(
                    Collectors.toList());
            result.addAll(partResult);
        }

        return result;
    }

    private void dispatchUploadJobs(List<BinlogOssRecord> records) {
        for (final BinlogOssRecord record : records) {
            //如果文件正在上传中，则忽略
            if (uploadingFiles.contains(record.getBinlogFile())) {
                continue;
            }

            //double check，如果文件已经上传成功，则忽略
            Optional<BinlogOssRecord> latestRecord = ossRecordMapper.selectOne(s -> s.
                where(BinlogOssRecordDynamicSqlSupport.id, SqlBuilder.isEqualTo(record.getId())));
            if (latestRecord.isPresent() &&
                latestRecord.get().getUploadStatus() == BinlogUploadStatusEnum.SUCCESS.getValue()) {
                continue;
            }

            uploadExecutor.submit(() -> {
                try {
                    logger.info("begin to upload binlog file: " + record.getBinlogFile());
                    processUpload(record);
                    logger.info("binlog file " + record.getBinlogFile() + " is successfully uploaded.");
                } catch (Throwable e) {
                    logger.error("upload file failed!", e);
                    alert(record.getBinlogFile());
                } finally {
                    // 如果上传中发生异常，将这个record从uploadingFiles中移除，下次扫描线程又能够扫到这个record没有上传成功
                    // 如果不从uploadingFiles中移除，则会误认为这个文件正在上传中，会导致这个文件之后永远也得不到上传
                    uploadingFiles.remove(record.getBinlogFile());
                }
            });
            uploadingFiles.add(record.getBinlogFile());
        }
    }

    private void processUpload(BinlogOssRecord record) throws IOException {
        setUploadStatusToUploading(record);
        deleteFileOnRemote(record);
        doUpload(record);
        setUploadStatusToSuccess(record);
    }

    private void doUpload(BinlogOssRecord record) throws IOException {
        IDataFetcher fetcher = new BinlogFetcher(record.getBinlogFile(),
            BinlogFileUtil.getBinlogFileFullPath(binlogRootPath, record.getGroupId(), record.getStreamId()),
            new DataFileChecker(provider, record.getStreamId()));
        String remoteFileName = BinlogFileUtil.buildRemoteFilePartName(
            record.getBinlogFile(), record.getGroupId(), record.getStreamId());
        BinlogUploader binlogUploader =
            new BinlogUploader(fetcher, remoteFileName, metricsObserverMap.get(record.getStreamId()));
        binlogUploader.upload();
    }

    private void deleteFileOnRemote(BinlogOssRecord record) {
        String remoteFileName = BinlogFileUtil.buildRemoteFilePartName(
            record.getBinlogFile(), record.getGroupId(), record.getStreamId());
        RemoteBinlogProxy.getInstance().deleteFile(remoteFileName);
    }

    private void setUploadStatusToSuccess(BinlogOssRecord record) {
        ossRecordMapper.update(u -> u.set(BinlogOssRecordDynamicSqlSupport.uploadStatus)
            .equalTo(BinlogUploadStatusEnum.SUCCESS.getValue()).set(BinlogOssRecordDynamicSqlSupport.uploadHost)
            .equalTo(DynamicApplicationConfig.getString(ConfigKeys.INST_IP))
            .where(BinlogOssRecordDynamicSqlSupport.id, SqlBuilder.isEqualTo(record.getId())));
    }

    private void setUploadStatusToUploading(BinlogOssRecord record) {
        ossRecordMapper.update(u -> u.set(BinlogOssRecordDynamicSqlSupport.uploadStatus)
            .equalTo(BinlogUploadStatusEnum.UPLOADING.getValue())
            .where(BinlogOssRecordDynamicSqlSupport.id, SqlBuilder.isEqualTo(record.getId())));
    }

    public void start() {
        scanExecutor.execute(this);
    }

    public void shutdown() {
        this.runnable = false;
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
        }
        this.uploadingFiles.clear();
    }
}
