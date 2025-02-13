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
package com.aliyun.polardbx.binlog.cdc.meta;

import com.alibaba.polardbx.druid.DbType;
import com.alibaba.polardbx.druid.sql.SQLUtils;
import com.alibaba.polardbx.druid.sql.ast.SQLStatement;
import com.alibaba.polardbx.druid.sql.ast.statement.SQLAlterTableStatement;
import com.alibaba.polardbx.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.polardbx.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.polardbx.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.polardbx.druid.sql.dialect.mysql.ast.statement.MySqlRenameTableStatement;
import com.alibaba.polardbx.druid.sql.parser.SQLStatementParser;
import com.aliyun.polardbx.binlog.CommonUtils;
import com.aliyun.polardbx.binlog.ConfigKeys;
import com.aliyun.polardbx.binlog.DynamicApplicationConfig;
import com.aliyun.polardbx.binlog.SpringContextHolder;
import com.aliyun.polardbx.binlog.canal.core.ddl.tsdb.MemoryTableMeta;
import com.aliyun.polardbx.binlog.canal.core.model.BinlogPosition;
import com.aliyun.polardbx.binlog.cdc.repository.CdcSchemaStoreProvider;
import com.aliyun.polardbx.binlog.cdc.topology.LogicBasicInfo;
import com.aliyun.polardbx.binlog.cdc.topology.LogicMetaTopology.PhyTableTopology;
import com.aliyun.polardbx.binlog.cdc.topology.TopologyManager;
import com.aliyun.polardbx.binlog.dao.BinlogPhyDdlHistoryDynamicSqlSupport;
import com.aliyun.polardbx.binlog.dao.BinlogPhyDdlHistoryMapper;
import com.aliyun.polardbx.binlog.domain.po.BinlogPhyDdlHistory;
import com.aliyun.polardbx.binlog.jvm.JvmSnapshot;
import com.aliyun.polardbx.binlog.jvm.JvmUtils;
import com.aliyun.polardbx.binlog.util.FastSQLConstant;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.alibaba.polardbx.druid.sql.parser.SQLParserUtils.createSQLStatementParser;
import static com.aliyun.polardbx.binlog.ConfigKeys.META_DDL_IGNORE_APPLY_ERROR;
import static com.aliyun.polardbx.binlog.ConfigKeys.META_TABLE_CACHE_EXPIRE_TIME_MINUTES;
import static com.aliyun.polardbx.binlog.ConfigKeys.META_TABLE_MAX_CACHE_SIZE;

/**
 * Created by Shuguang & ziyang.lb
 */
public class PolarDbXStorageTableMeta extends MemoryTableMeta implements ICdcTableMeta {
    private static final Logger logger = LoggerFactory.getLogger(PolarDbXStorageTableMeta.class);
    private static final int PAGE_SIZE = 200;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String storageInstId;
    private final PolarDbXLogicTableMeta polarDbXLogicTableMeta;
    private final TopologyManager topologyManager;
    private final BinlogPhyDdlHistoryMapper binlogPhyDdlHistoryMapper = SpringContextHolder.getObject(
        BinlogPhyDdlHistoryMapper.class);
    private String maxTsoWithInit = "";
    private long applySnapshotCostTime = -1;
    private long applyHistoryCostTime = -1;
    private long queryDdlHistoryCostTime = -1;
    private long queryDdlHistoryCount = -1;

    public PolarDbXStorageTableMeta(String storageInstId, PolarDbXLogicTableMeta polarDbXLogicTableMeta,
                                    TopologyManager topologyManager) {
        super(logger, CdcSchemaStoreProvider.getInstance(),
            DynamicApplicationConfig.getInt(META_TABLE_MAX_CACHE_SIZE),
            DynamicApplicationConfig.getInt(META_TABLE_CACHE_EXPIRE_TIME_MINUTES),
            DynamicApplicationConfig.getBoolean(META_DDL_IGNORE_APPLY_ERROR));
        this.storageInstId = storageInstId;
        this.polarDbXLogicTableMeta = polarDbXLogicTableMeta;
        this.topologyManager = topologyManager;
    }

    @Override
    public boolean init(final String destination) {
        //this.initMaxTso();暂时先不支持
        return true;
    }

    public void applyBase(BinlogPosition position) {
        applySnapshot(position.getRtso());
    }

    @Override
    public void applySnapshot(String snapshotTso) {
        // log before apply snapshot
        long applyCount = 0;
        long startTime = System.currentTimeMillis();
        JvmSnapshot jvmSnapshot = JvmUtils.buildJvmSnapshot();
        logger.info("build physical meta snapshot started, current used memory -> young:{}, old:{}",
            jvmSnapshot.getYoungUsed(), jvmSnapshot.getOldUsed());

        // 获取Logic Snapshot
        Map<String, String> snapshot = polarDbXLogicTableMeta.snapshot();
        snapshot.forEach((k, v) -> super.apply(null, k, v, null));
        //用于订正逻辑表和物理表结构不一致的情况
        Map<String, String> snapshotToFix = polarDbXLogicTableMeta.distinctPhySnapshot();
        if (snapshotToFix != null && !snapshotToFix.isEmpty()) {
            snapshotToFix.forEach((k, v) -> super.apply(null, k, v, null));
        } else {
            logger.info("All logical and physical tables is compatible for snapshot tso:{}...", snapshotTso);
        }

        // 根据逻辑MetaSnapshot构建物理
        topologyManager.initPhyLogicMapping(storageInstId);
        List<PhyTableTopology> phyTables =
            topologyManager.getPhyTables(storageInstId, Sets.newHashSet(), Sets.newHashSet());
        for (PhyTableTopology phyTable : phyTables) {
            final List<String> tables = phyTable.getPhyTables();
            if (tables != null) {
                for (String table : tables) {
                    LogicBasicInfo logicBasicInfo = topologyManager.getLogicBasicInfo(phyTable.getSchema(), table);
                    checkLogicBasicInfo(logicBasicInfo, phyTable, table);
                    String tableName = logicBasicInfo.getTableName();
                    String createTableSql = "create table `" + CommonUtils.escape(table) + "` like `" +
                        CommonUtils.escape(logicBasicInfo.getSchemaName()) + "`.`" + CommonUtils.escape(tableName)
                        + "`";
                    super.apply(null, phyTable.getSchema(), createTableSql, null);
                    applyCount++;

                    if (logger.isDebugEnabled()) {
                        logger.debug("apply from logic table, phy:{}.{}, logic:{}.{} [{}] ...", phyTable.getSchema(),
                            table, logicBasicInfo.getSchemaName(), tableName, createTableSql);
                    }
                }
            }
        }
        //drop 逻辑库
        snapshot.forEach((k, v) -> super.apply(null, k, "drop database `" + k + "`", null));

        //log after apply snapshot
        long costTime = System.currentTimeMillis() - startTime;
        applySnapshotCostTime += costTime;
        jvmSnapshot = JvmUtils.buildJvmSnapshot();
        logger.info("build physical meta snapshot finished, applyCount {}, cost time {}(ms), current used memory -> "
            + "young:{}, old:{}", costTime, applyCount, jvmSnapshot.getYoungUsed(), jvmSnapshot.getOldUsed());
    }

    @Override
    public void applyHistory(String snapshotTso, String rollbackTso) {
        // log before apply
        final String snapshotTsoInput = snapshotTso;
        long applyCount = 0;
        long startTime = System.currentTimeMillis();
        JvmSnapshot jvmSnapshot = JvmUtils.buildJvmSnapshot();
        logger.info("apply phy ddl history started, current used memory -> young:{}, old:{}",
            jvmSnapshot.getYoungUsed(), jvmSnapshot.getOldUsed());

        // apply history
        while (true) {
            final String snapshotTsoCondition = snapshotTso;
            long queryStartTime = System.currentTimeMillis();
            List<BinlogPhyDdlHistory> ddlHistories = binlogPhyDdlHistoryMapper.select(
                s -> s.where(BinlogPhyDdlHistoryDynamicSqlSupport.storageInstId, SqlBuilder.isEqualTo(storageInstId))
                    .and(BinlogPhyDdlHistoryDynamicSqlSupport.tso, SqlBuilder.isGreaterThan(snapshotTsoCondition))
                    .and(BinlogPhyDdlHistoryDynamicSqlSupport.tso, SqlBuilder.isLessThanOrEqualTo(rollbackTso))
                    .and(BinlogPhyDdlHistoryDynamicSqlSupport.clusterId,
                        SqlBuilder.isEqualTo(DynamicApplicationConfig.getString(ConfigKeys.CLUSTER_ID)))
                    .orderBy(BinlogPhyDdlHistoryDynamicSqlSupport.tso).limit(PAGE_SIZE)
            );
            queryDdlHistoryCostTime += (System.currentTimeMillis() - queryStartTime);
            queryDdlHistoryCount += ddlHistories.size();

            for (BinlogPhyDdlHistory ddlHistory : ddlHistories) {
                toLowerCase(ddlHistory);
                BinlogPosition position = new BinlogPosition(null, ddlHistory.getTso());
                String ddl = ddlHistory.getDdl();
                if (DynamicApplicationConfig.getBoolean(ConfigKeys.TASK_DDL_REMOVEHINTS_SUPPORT)) {
                    ddl = com.aliyun.polardbx.binlog.canal.core.ddl.SQLUtils.removeDDLHints(ddlHistory.getDdl());
                }
                super.apply(position, ddlHistory.getDbName(), ddl, ddlHistory.getExtra());
                if (logger.isDebugEnabled()) {
                    logger.debug("apply one physical phy ddl: [id={}, dbName={}, tso={}]", ddlHistory.getId(),
                        ddlHistory.getDbName(), ddlHistory.getTso());
                }
                tryPrint(position, ddlHistory.getDbName(), ddlHistory.getDdl());
            }

            applyCount += ddlHistories.size();
            if (ddlHistories.size() == PAGE_SIZE) {
                snapshotTso = ddlHistories.get(PAGE_SIZE - 1).getTso();
            } else {
                break;
            }
        }

        //log after apply
        long costTime = System.currentTimeMillis() - startTime;
        applyHistoryCostTime += costTime;
        jvmSnapshot = JvmUtils.buildJvmSnapshot();
        logger.info(
            "apply phy ddl history finished, snapshot tso {}, rollback tso {}, cost time {}(ms), applyCount {}, "
                + "current used memory -> young:{}, old:{}", snapshotTsoInput, rollbackTso, costTime, applyCount,
            jvmSnapshot.getYoungUsed(), jvmSnapshot.getOldUsed());
    }

    @Override
    public boolean apply(BinlogPosition position, String schema, String ddl, String extra) {
        // 首先记录到内存结构
        lock.writeLock().lock();
        try {
            if (super.apply(position, schema, ddl, extra)) {
                // 同步每次变更给远程做历史记录，只记录ddl，不记录快照
                applyHistoryToDb(position, schema, ddl, extra);
                tryPrint(position, schema, ddl);
                return true;
            } else {
                throw new RuntimeException("apply to memory is failed");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean apply(String schema, String ddl) {
        return super.apply(null, schema, ddl, null);
    }

    private void checkLogicBasicInfo(LogicBasicInfo logicBasicInfo, PhyTableTopology phyTable, String table) {
        Preconditions.checkNotNull(logicBasicInfo,
            "phyTable " + phyTable.getSchema() + "." + table + "'s logicBasicInfo should not be null!");
        Preconditions.checkNotNull(logicBasicInfo.getSchemaName(),
            "phyTable " + phyTable.getSchema() + "." + table + "'s logicSchemaName should not be null!");
        Preconditions.checkNotNull(logicBasicInfo.getTableName(),
            "phyTable " + phyTable.getSchema() + "." + table + "'s logicTableName should not be null!");
    }

    private void applyHistoryToDb(BinlogPosition position, String schema, String ddl, String extra) {
        try {
            if (position.getRtso().compareTo(maxTsoWithInit) <= 0) {
                return;
            }

            binlogPhyDdlHistoryMapper.insert(BinlogPhyDdlHistory.builder().storageInstId(storageInstId)
                .binlogFile(position.getFileName())
                .tso(position.getRtso())
                .dbName(schema)
                .ddl(ddl)
                .clusterId(DynamicApplicationConfig.getString(ConfigKeys.CLUSTER_ID))
                .extra(extra).build());
        } catch (DuplicateKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("already applyHistoryToDB, ignore this time, position is : {}, schema is {}, tso is {},"
                    + " extra is {}", position, schema, position.getRtso(), extra);
            }
        }
    }

    private void toLowerCase(BinlogPhyDdlHistory phyDdlHistory) {
        phyDdlHistory.setDbName(StringUtils.lowerCase(phyDdlHistory.getDbName()));
        phyDdlHistory.setDdl(StringUtils.lowerCase(phyDdlHistory.getDdl()));
    }

    private void tryPrint(BinlogPosition position, String schema, String ddl) {
        if (Printer.isSupportPrint()) {
            String tableName = parseTableName(ddl);
            if (StringUtils.isNotBlank(tableName)) {
                Printer.tryPrint(position, schema, tableName, this);
            }
        }
    }

    private String parseTableName(String ddl) {
        try {
            if (StringUtils.isBlank(ddl)) {
                return "";
            }

            SQLStatementParser parser = createSQLStatementParser(ddl, DbType.mysql, FastSQLConstant.FEATURES);
            List<SQLStatement> statementList = parser.parseStatementList();
            SQLStatement sqlStatement = statementList.get(0);

            if (sqlStatement instanceof SQLCreateTableStatement) {
                SQLCreateTableStatement sqlCreateTableStatement = (SQLCreateTableStatement) sqlStatement;
                return SQLUtils.normalize(sqlCreateTableStatement.getTableName());
            } else if (sqlStatement instanceof SQLDropTableStatement) {
                SQLDropTableStatement sqlDropTableStatement = (SQLDropTableStatement) sqlStatement;
                for (SQLExprTableSource tableSource : sqlDropTableStatement.getTableSources()) {
                    //CN只支持一次drop一张表，直接返回即可
                    return tableSource.getTableName(true);
                }
            } else if (sqlStatement instanceof MySqlRenameTableStatement) {
                MySqlRenameTableStatement renameTableStatement = (MySqlRenameTableStatement) sqlStatement;
                for (MySqlRenameTableStatement.Item item : renameTableStatement.getItems()) {
                    //CN只支持一次Rename一张表，直接返回即可
                    return SQLUtils.normalize(item.getName().getSimpleName());
                }
            } else if (sqlStatement instanceof SQLAlterTableStatement) {
                SQLAlterTableStatement sqlAlterTableStatement = (SQLAlterTableStatement) sqlStatement;
                return SQLUtils.normalize(sqlAlterTableStatement.getTableName());
            }
        } catch (Throwable t) {
            logger.error("parse table from ddl sql failed.", t);
        }
        return "";
    }

    private void initMaxTso() {
        JdbcTemplate metaJdbcTemplate = SpringContextHolder.getObject("metaJdbcTemplate");
        String sql = "select max(tso) tso from binlog_phy_ddl_history where storage_inst_id = '" + storageInstId + "'";
        String result = metaJdbcTemplate.queryForObject(sql, String.class);
        this.maxTsoWithInit = result == null ? "" : result;
    }

    public long getApplySnapshotCostTime() {
        return applySnapshotCostTime;
    }

    public long getApplyHistoryCostTime() {
        return applyHistoryCostTime;
    }

    public long getQueryDdlHistoryCostTime() {
        return queryDdlHistoryCostTime;
    }

    public long getQueryDdlHistoryCount() {
        return queryDdlHistoryCount;
    }
}
