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
package com.aliyun.polardbx.binlog.canal.core.ddl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yanfenglin
 */
public class ThreadRecorder {

    private long start;
    private long tid;
    private String tname;
    private volatile boolean complete;
    private volatile long rt;
    private STATE state;
    private String binlogFile;
    private long logPos;
    private long when;
    private long queuedTransSizeInSorter;
    private String firstTransInSorter;
    private String firstTransXidInSorter;
    private String storageInstanceId;
    private long mergeSourceQueueSize;
    private long mergeSourcePassCount;
    private long mergeSourcePollCount;
    private volatile long netIn = 0;

    private static final Map<Long, ThreadRecorder> ALL = new ConcurrentHashMap<>();

    public static void registerRecorder(ThreadRecorder threadRecorder) {
        ALL.put(threadRecorder.getTid(), threadRecorder);
    }

    public static Map<Long, ThreadRecorder> getRecorderMap() {
        return ALL;
    }

    public static void removeRecord(ThreadRecorder threadRecorder) {
        if (threadRecorder == null || threadRecorder.getTname() == null) {
            return;
        }
        ALL.remove(threadRecorder.getTid());
    }

    public ThreadRecorder(String storageInstanceId) {
        this.state = STATE.SEARCH;
        this.storageInstanceId = storageInstanceId;
    }

    public void init() {
        this.start = System.currentTimeMillis();
        this.complete = false;
        this.tid = Thread.currentThread().getId();
        this.tname = Thread.currentThread().getName();
    }

    public void dump() {
        this.state = STATE.DUMP;
    }

    public void stop() {
        this.state = STATE.STOP;
    }

    public boolean isStop() {
        return state == STATE.STOP;
    }

    public void doRecord(CallbackFunction callbackFunction) throws Exception {
        start = System.currentTimeMillis();
        complete = false;
        callbackFunction.call();
        rt = System.currentTimeMillis() - start;
        complete = true;
    }

    public String getStorageInstanceId() {
        return storageInstanceId;
    }

    public void setStorageInstanceId(String storageInstanceId) {
        this.storageInstanceId = storageInstanceId;
    }

    public long getStart() {
        return start;
    }

    public boolean isComplete() {
        return complete;
    }

    public long getTid() {
        return tid;
    }

    public STATE getState() {
        return state;
    }

    public long getRt() {
        if (complete) {
            return rt;
        } else {
            return System.currentTimeMillis() - start;
        }
    }

    public String getPosition() {
        return binlogFile + ":" + logPos + "#" + when;
    }

    public String getTname() {
        return tname;
    }

    public void setBinlogFile(String binlogFile) {
        this.binlogFile = binlogFile;
    }

    public void setLogPos(long logPos) {
        this.logPos = logPos;
    }

    public long getWhen() {
        return when;
    }

    public void setWhen(long when) {
        this.when = when;
    }

    public long getNetIn() {
        return netIn;
    }

    public void addNetIn(long netIn) {
        this.netIn += netIn;
    }

    public long getQueuedTransSizeInSorter() {
        return queuedTransSizeInSorter;
    }

    public void setQueuedTransSizeInSorter(long queuedTransSizeInSorter) {
        this.queuedTransSizeInSorter = queuedTransSizeInSorter;
    }

    public String getFirstTransInSorter() {
        return firstTransInSorter;
    }

    public void setFirstTransInSorter(String firstTransInSorter) {
        this.firstTransInSorter = firstTransInSorter;
    }

    public String getFirstTransXidInSorter() {
        return firstTransXidInSorter;
    }

    public void setFirstTransXidInSorter(String firstTransXidInSorter) {
        this.firstTransXidInSorter = firstTransXidInSorter;
    }

    public long getMergeSourceQueueSize() {
        return mergeSourceQueueSize;
    }

    public void setMergeSourceQueueSize(long mergeSourceQueueSize) {
        this.mergeSourceQueueSize = mergeSourceQueueSize;
    }

    public long getMergeSourcePassCount() {
        return mergeSourcePassCount;
    }

    public void setMergeSourcePassCount(long mergeSourcePassCount) {
        this.mergeSourcePassCount = mergeSourcePassCount;
    }

    public long getMergeSourcePollCount() {
        return mergeSourcePollCount;
    }

    public void setMergeSourcePollCount(long mergeSourcePollCount) {
        this.mergeSourcePollCount = mergeSourcePollCount;
    }

    @Override
    public String toString() {
        switch (state) {
        case SEARCH:
            return tname + " search position now, please wait...";
        case DUMP:
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format("%8d, %10d, %s, %s, %d",
                getTid(),
                getRt(),
                isComplete() ? "complete" : "unComplete",
                getPosition(),
                Math.max(System.currentTimeMillis() / 1000 - when, 0)));
            return stringBuilder.toString();
        case STOP:
            return tname + "  stop parser now , please wait...!";
        }
        return "unknow error!";
    }

    private enum STATE {
        SEARCH, DUMP, STOP
    }

    public interface CallbackFunction {

        void call() throws Exception;
    }

}
