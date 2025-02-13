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
package com.aliyun.polardbx.binlog.task;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.polardbx.binlog.ConfigKeys;
import com.aliyun.polardbx.binlog.DynamicApplicationConfig;
import com.aliyun.polardbx.binlog.SpringContextHolder;
import com.aliyun.polardbx.binlog.dao.BinlogTaskInfoDynamicSqlSupport;
import com.aliyun.polardbx.binlog.dao.BinlogTaskInfoMapper;
import com.aliyun.polardbx.binlog.dao.DumperInfoDynamicSqlSupport;
import com.aliyun.polardbx.binlog.dao.DumperInfoMapper;
import com.aliyun.polardbx.binlog.dao.NodeInfoDynamicSqlSupport;
import com.aliyun.polardbx.binlog.dao.NodeInfoMapper;
import com.aliyun.polardbx.binlog.dao.TaskHeartbeatMapper;
import com.aliyun.polardbx.binlog.dao.XStreamDynamicSqlSupport;
import com.aliyun.polardbx.binlog.dao.XStreamMapper;
import com.aliyun.polardbx.binlog.domain.Cursor;
import com.aliyun.polardbx.binlog.domain.DumperType;
import com.aliyun.polardbx.binlog.domain.TaskType;
import com.aliyun.polardbx.binlog.domain.po.BinlogTaskConfig;
import com.aliyun.polardbx.binlog.leader.RuntimeLeaderElector;
import com.aliyun.polardbx.binlog.scheduler.ClusterSnapshot;
import com.aliyun.polardbx.binlog.scheduler.model.ExecutionConfig;
import com.aliyun.polardbx.binlog.util.SystemDbConfig;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.dynamic.sql.SqlBuilder;

import java.util.Map;

import static com.aliyun.polardbx.binlog.ConfigKeys.CLUSTER_SNAPSHOT_VERSION_KEY;
import static com.aliyun.polardbx.binlog.Constants.STREAM_NAME_GLOBAL;
import static com.aliyun.polardbx.binlog.ConfigKeys.GLOBAL_BINLOG_LATEST_CURSOR;
import static com.aliyun.polardbx.binlog.Constants.STREAM_NAME_GLOBAL;

/**
 * Created by ziyang.lb
 */
@Slf4j
public class TaskHeartbeat extends AbstractBinlogTimerTask {
    private final BinlogTaskConfig config;
    private final long version;
    private final DumperInfoMapper dumperInfoMapper = SpringContextHolder.getObject(DumperInfoMapper.class);
    private final NodeInfoMapper nodeInfoMapper = SpringContextHolder.getObject(NodeInfoMapper.class);
    private final XStreamMapper xStreamMapper = SpringContextHolder.getObject(XStreamMapper.class);
    private Map<String, ICursorProvider> cursorProviderMap;

    public TaskHeartbeat(String clusterId, String clusterType, String name, int interval, BinlogTaskConfig config) {
        super(clusterId, clusterType, name, interval);
        this.config = config;
        this.version = config.getVersion();
    }

    @Override
    public void exec() {
        String snapshotConfigStr = SystemDbConfig.getSystemDbConfig(CLUSTER_SNAPSHOT_VERSION_KEY);
        ClusterSnapshot clusterSnapshot = JSONObject.parseObject(snapshotConfigStr, ClusterSnapshot.class);

        // 判断一下拓扑版本是否已经晋升到了更高的版本，如果是的话，本进程已经没有存在的必要了，直接退出即可
        if (clusterSnapshot != null && version < clusterSnapshot.getVersion()) {
            log.warn("Cluster topology has been migrated to new version, "
                + "this process will exit,  stale old version is {},"
                + "latest new version is {}", version, clusterSnapshot.getVersion());
            Runtime.getRuntime().halt(1);
        }

        TaskHeartbeatMapper taskHeartbeatMapper = SpringContextHolder.getObject(TaskHeartbeatMapper.class);
        String role = this.config.getRole();
        if (role.equals(TaskType.Dumper.name())) {
            Cursor cursor = cursorProviderMap.get(STREAM_NAME_GLOBAL).getLatestFileCursor();
            final boolean dumperLeader = RuntimeLeaderElector.isDumperLeader(name);

            //更新心跳
            int result = taskHeartbeatMapper.updateDumperHeartbeat(name,
                dumperLeader ? DumperType.MASTER.getName() : DumperType.SLAVE.getName(), clusterId);
            if (result == 0) {
                log.error("Dumper info has been removed from database, this process will exit");
                Runtime.getRuntime().halt(1);
            }

            // 类似贪心算法，强制把其它dumper的状态设置为S的角色，因为在分布式环境下，相同名字的Dumper是可能存在短暂共存状态的，需要进行矫正
            if (dumperLeader) {
                dumperInfoMapper.update(
                    s -> s
                        .set(DumperInfoDynamicSqlSupport.role)
                        .equalTo(DumperType.SLAVE.getName())
                        .where(DumperInfoDynamicSqlSupport.clusterId,
                            SqlBuilder.isEqualTo(DynamicApplicationConfig.getString(ConfigKeys.CLUSTER_ID)))
                        .and(DumperInfoDynamicSqlSupport.taskName, SqlBuilder.isNotEqualTo(name)));
                SystemDbConfig.updateSystemDbConfig(GLOBAL_BINLOG_LATEST_CURSOR, JSONObject.toJSONString(cursor));
            }

            // 一个Node只会运行一个Dumper，将cursor信息记录到Node，方便Daemon调度时进行参考(选Cursor最大的Node上的Dumper为Master)
            if (cursor != null) {
                nodeInfoMapper.update(
                    u -> u
                        .set(NodeInfoDynamicSqlSupport.latestCursor)
                        .equalTo(JSONObject.toJSONString(cursor))
                        .where(NodeInfoDynamicSqlSupport.clusterId, SqlBuilder.isEqualTo(clusterId))
                        .and(NodeInfoDynamicSqlSupport.containerId,
                            SqlBuilder.isEqualTo(DynamicApplicationConfig.getString(ConfigKeys.INST_ID)))
                );
            }
        } else if (role.equals(TaskType.Final.name()) || role.equals(TaskType.Relay.name()) || role
            .equals(TaskType.Dispatcher.name())) {
            int result = taskHeartbeatMapper.updateTaskHeartbeat(name, clusterId);
            if (result == 0) {
                log.error("Task info has been removed from database, this process will exit");
                Runtime.getRuntime().halt(1);
            }
        } else if (role.equals(TaskType.DumperX.name())) {
            //更新心跳
            int result = taskHeartbeatMapper.updateDumperHeartbeat(name, DumperType.XSTREAM.getName(), clusterId);
            if (result == 0) {
                log.error("Dumper info has been removed from database, this process will exit");
                Runtime.getRuntime().halt(1);
            }

            ExecutionConfig executionConfig = new Gson().fromJson(config.getConfig(), ExecutionConfig.class);
            executionConfig.getStreamNameSet().forEach(streamName -> {
                Cursor cursor = cursorProviderMap.get(streamName).getLatestFileCursor();
                if (cursor != null) {
                    xStreamMapper.update(
                        u -> u.set(XStreamDynamicSqlSupport.latestCursor).equalTo(JSONObject.toJSONString(cursor))
                            .where(XStreamDynamicSqlSupport.streamName, SqlBuilder.isEqualTo(streamName)));
                }
            });
        }
    }

    public void setCursorProviderMap(
        Map<String, ICursorProvider> cursorProviderMap) {
        this.cursorProviderMap = cursorProviderMap;
    }
}
