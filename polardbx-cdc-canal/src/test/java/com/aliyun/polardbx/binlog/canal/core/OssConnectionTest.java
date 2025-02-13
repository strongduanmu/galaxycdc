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
package com.aliyun.polardbx.binlog.canal.core;

import com.aliyun.polardbx.binlog.SpringContextBootStrap;
import com.aliyun.polardbx.binlog.api.RdsApi;
import com.aliyun.polardbx.binlog.api.rds.BinlogFile;
import com.aliyun.polardbx.binlog.canal.binlog.LogContext;
import com.aliyun.polardbx.binlog.canal.binlog.LogDecoder;
import com.aliyun.polardbx.binlog.canal.binlog.LogEvent;
import com.aliyun.polardbx.binlog.canal.binlog.LogFetcher;
import com.aliyun.polardbx.binlog.canal.binlog.LogPosition;
import com.aliyun.polardbx.binlog.canal.core.dump.OssConnection;
import com.aliyun.polardbx.binlog.canal.core.model.ServerCharactorSet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class OssConnectionTest {

    @Test
    public void testUTC() throws ParseException {
        String utfHost = RdsApi.formatUTCTZ(new Date(1651782957000L));
        String utcBegin = RdsApi.formatUTCTZ(new Date(1651569048284L));
        System.out.println(utcBegin);
        System.out.println(BinlogFile.format(utcBegin));
        System.out.println(utfHost);
        System.out.println(BinlogFile.format(utfHost));

    }

    @Before
    public void before() {
        final SpringContextBootStrap appContextBootStrap = new SpringContextBootStrap("spring/spring.xml");
        appContextBootStrap.boot();
    }

    @Test
    public void testMulti() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                test();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                test();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.start();
        t2.start();
        t.join();
        t2.join();
    }

    @Test
    public void test() throws IOException {
        System.setProperty("RDS_API", "");
        System.setProperty("taskName", "Final");
        OssConnection ossConnection =
            new OssConnection("", "", "", "", 100242035L, 5, null, 0L);
        ossConnection.setTest(true);
        ossConnection.connect();
        ossConnection.printBinlogQueue();
        LogFetcher fetcher = ossConnection.providerFetcher("mysql-bin.000765", 0, true);
        LogDecoder decoder = new LogDecoder();
        decoder.handle(LogEvent.ROTATE_EVENT);
        decoder.handle(LogEvent.FORMAT_DESCRIPTION_EVENT);
        decoder.handle(LogEvent.QUERY_EVENT);
        decoder.handle(LogEvent.XID_EVENT);
        decoder.handle(LogEvent.SEQUENCE_EVENT);
        decoder.handle(LogEvent.GCN_EVENT);
        decoder.handle(LogEvent.XA_PREPARE_LOG_EVENT);
        decoder.handle(LogEvent.WRITE_ROWS_EVENT_V1);
        decoder.handle(LogEvent.WRITE_ROWS_EVENT);
        decoder.handle(LogEvent.TABLE_MAP_EVENT);
        LogContext lc = new LogContext();
        lc.setServerCharactorSet(new ServerCharactorSet("utf8", "utf8", "utf8", "utf8"));
        lc.setLogPosition(new LogPosition("", 0));

        while (fetcher.fetch()) {
            LogEvent event = decoder.decode(fetcher.buffer(), lc);
            if (event == null) {
                continue;
            }
        }
    }
}
