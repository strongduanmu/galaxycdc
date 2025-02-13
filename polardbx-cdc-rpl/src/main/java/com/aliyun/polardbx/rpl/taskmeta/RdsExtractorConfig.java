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
package com.aliyun.polardbx.rpl.taskmeta;

import lombok.Data;
import java.util.Set;

/**
 * @author shicai.xsc 2020/12/8 14:30
 * @since 5.0.0.0
 */

@Data
public class RdsExtractorConfig extends ExtractorConfig {
    private String rdsInstanceId;
    private String uid;
    private String bid;
    private Long fixHostInstanceId;
}
