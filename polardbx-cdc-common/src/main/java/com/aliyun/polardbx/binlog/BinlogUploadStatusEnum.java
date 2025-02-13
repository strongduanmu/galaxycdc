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
package com.aliyun.polardbx.binlog;

public enum BinlogUploadStatusEnum {
    /**
     * 文件刚创建好，还没有进行上传
     */
    CREATE(0),
    /**
     * 文件正在上传中
     */
    UPLOADING(1),
    /**
     * 文件上传成功
     */
    SUCCESS(2),
    /**
     * 文件不进行上传
     */
    IGNORE(3);

    private final int value;

    BinlogUploadStatusEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
