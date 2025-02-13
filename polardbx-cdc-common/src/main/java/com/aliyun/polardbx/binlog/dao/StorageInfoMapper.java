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
package com.aliyun.polardbx.binlog.dao;

import com.aliyun.polardbx.binlog.domain.po.StorageInfo;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.delete.DeleteDSLCompleter;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.insert.render.MultiRowInsertStatementProvider;
import org.mybatis.dynamic.sql.select.CountDSLCompleter;
import org.mybatis.dynamic.sql.select.SelectDSLCompleter;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateDSLCompleter;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;

import javax.annotation.Generated;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.azoneId;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.cpuCore;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.extras;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.gmtCreated;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.gmtModified;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.id;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.idcId;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.instId;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.instKind;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.ip;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.isVip;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.maxConn;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.memSize;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.passwdEnc;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.port;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.regionId;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.status;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.storageInfo;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.storageInstId;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.storageMasterInstId;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.storageType;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.user;
import static com.aliyun.polardbx.binlog.dao.StorageInfoDynamicSqlSupport.xport;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Mapper
public interface StorageInfoMapper {
    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    BasicColumn[] selectList = BasicColumn
        .columnList(id, gmtCreated, gmtModified, instId, storageInstId, storageMasterInstId, ip, port, xport, user,
            storageType, instKind, status, regionId, azoneId, idcId, maxConn, cpuCore, memSize, isVip, passwdEnc,
            extras);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.418+08:00",
        comments = "Source Table: storage_info")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.418+08:00",
        comments = "Source Table: storage_info")
    @DeleteProvider(type = SqlProviderAdapter.class, method = "delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.418+08:00",
        comments = "Source Table: storage_info")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insert(InsertStatementProvider<StorageInfo> insertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.418+08:00",
        comments = "Source Table: storage_info")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insertMultiple")
    int insertMultiple(MultiRowInsertStatementProvider<StorageInfo> multipleInsertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.418+08:00",
        comments = "Source Table: storage_info")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ConstructorArgs({
        @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT, id = true),
        @Arg(column = "gmt_created", javaType = Date.class, jdbcType = JdbcType.TIMESTAMP),
        @Arg(column = "gmt_modified", javaType = Date.class, jdbcType = JdbcType.TIMESTAMP),
        @Arg(column = "inst_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "storage_inst_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "storage_master_inst_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "ip", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "port", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "xport", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "user", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "storage_type", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "inst_kind", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "status", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "region_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "azone_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "idc_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "max_conn", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "cpu_core", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "mem_size", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "is_vip", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "passwd_enc", javaType = String.class, jdbcType = JdbcType.LONGVARCHAR),
        @Arg(column = "extras", javaType = String.class, jdbcType = JdbcType.LONGVARCHAR)
    })
    Optional<StorageInfo> selectOne(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.418+08:00",
        comments = "Source Table: storage_info")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ConstructorArgs({
        @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT, id = true),
        @Arg(column = "gmt_created", javaType = Date.class, jdbcType = JdbcType.TIMESTAMP),
        @Arg(column = "gmt_modified", javaType = Date.class, jdbcType = JdbcType.TIMESTAMP),
        @Arg(column = "inst_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "storage_inst_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "storage_master_inst_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "ip", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "port", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "xport", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "user", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "storage_type", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "inst_kind", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "status", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "region_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "azone_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "idc_id", javaType = String.class, jdbcType = JdbcType.VARCHAR),
        @Arg(column = "max_conn", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "cpu_core", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "mem_size", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "is_vip", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
        @Arg(column = "passwd_enc", javaType = String.class, jdbcType = JdbcType.LONGVARCHAR),
        @Arg(column = "extras", javaType = String.class, jdbcType = JdbcType.LONGVARCHAR)
    })
    List<StorageInfo> selectMany(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, storageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, storageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    default int deleteByPrimaryKey(Long id_) {
        return delete(c ->
            c.where(id, isEqualTo(id_))
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    default int insert(StorageInfo record) {
        return MyBatis3Utils.insert(this::insert, record, storageInfo, c ->
            c.map(id).toProperty("id")
                .map(gmtCreated).toProperty("gmtCreated")
                .map(gmtModified).toProperty("gmtModified")
                .map(instId).toProperty("instId")
                .map(storageInstId).toProperty("storageInstId")
                .map(storageMasterInstId).toProperty("storageMasterInstId")
                .map(ip).toProperty("ip")
                .map(port).toProperty("port")
                .map(xport).toProperty("xport")
                .map(user).toProperty("user")
                .map(storageType).toProperty("storageType")
                .map(instKind).toProperty("instKind")
                .map(status).toProperty("status")
                .map(regionId).toProperty("regionId")
                .map(azoneId).toProperty("azoneId")
                .map(idcId).toProperty("idcId")
                .map(maxConn).toProperty("maxConn")
                .map(cpuCore).toProperty("cpuCore")
                .map(memSize).toProperty("memSize")
                .map(isVip).toProperty("isVip")
                .map(passwdEnc).toProperty("passwdEnc")
                .map(extras).toProperty("extras")
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    default int insertMultiple(Collection<StorageInfo> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, storageInfo, c ->
            c.map(id).toProperty("id")
                .map(gmtCreated).toProperty("gmtCreated")
                .map(gmtModified).toProperty("gmtModified")
                .map(instId).toProperty("instId")
                .map(storageInstId).toProperty("storageInstId")
                .map(storageMasterInstId).toProperty("storageMasterInstId")
                .map(ip).toProperty("ip")
                .map(port).toProperty("port")
                .map(xport).toProperty("xport")
                .map(user).toProperty("user")
                .map(storageType).toProperty("storageType")
                .map(instKind).toProperty("instKind")
                .map(status).toProperty("status")
                .map(regionId).toProperty("regionId")
                .map(azoneId).toProperty("azoneId")
                .map(idcId).toProperty("idcId")
                .map(maxConn).toProperty("maxConn")
                .map(cpuCore).toProperty("cpuCore")
                .map(memSize).toProperty("memSize")
                .map(isVip).toProperty("isVip")
                .map(passwdEnc).toProperty("passwdEnc")
                .map(extras).toProperty("extras")
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.419+08:00",
        comments = "Source Table: storage_info")
    default int insertSelective(StorageInfo record) {
        return MyBatis3Utils.insert(this::insert, record, storageInfo, c ->
            c.map(id).toPropertyWhenPresent("id", record::getId)
                .map(gmtCreated).toPropertyWhenPresent("gmtCreated", record::getGmtCreated)
                .map(gmtModified).toPropertyWhenPresent("gmtModified", record::getGmtModified)
                .map(instId).toPropertyWhenPresent("instId", record::getInstId)
                .map(storageInstId).toPropertyWhenPresent("storageInstId", record::getStorageInstId)
                .map(storageMasterInstId).toPropertyWhenPresent("storageMasterInstId", record::getStorageMasterInstId)
                .map(ip).toPropertyWhenPresent("ip", record::getIp)
                .map(port).toPropertyWhenPresent("port", record::getPort)
                .map(xport).toPropertyWhenPresent("xport", record::getXport)
                .map(user).toPropertyWhenPresent("user", record::getUser)
                .map(storageType).toPropertyWhenPresent("storageType", record::getStorageType)
                .map(instKind).toPropertyWhenPresent("instKind", record::getInstKind)
                .map(status).toPropertyWhenPresent("status", record::getStatus)
                .map(regionId).toPropertyWhenPresent("regionId", record::getRegionId)
                .map(azoneId).toPropertyWhenPresent("azoneId", record::getAzoneId)
                .map(idcId).toPropertyWhenPresent("idcId", record::getIdcId)
                .map(maxConn).toPropertyWhenPresent("maxConn", record::getMaxConn)
                .map(cpuCore).toPropertyWhenPresent("cpuCore", record::getCpuCore)
                .map(memSize).toPropertyWhenPresent("memSize", record::getMemSize)
                .map(isVip).toPropertyWhenPresent("isVip", record::getIsVip)
                .map(passwdEnc).toPropertyWhenPresent("passwdEnc", record::getPasswdEnc)
                .map(extras).toPropertyWhenPresent("extras", record::getExtras)
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    default Optional<StorageInfo> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, storageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    default List<StorageInfo> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, storageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    default List<StorageInfo> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, storageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    default Optional<StorageInfo> selectByPrimaryKey(Long id_) {
        return selectOne(c ->
            c.where(id, isEqualTo(id_))
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, storageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    static UpdateDSL<UpdateModel> updateAllColumns(StorageInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(id).equalTo(record::getId)
            .set(gmtCreated).equalTo(record::getGmtCreated)
            .set(gmtModified).equalTo(record::getGmtModified)
            .set(instId).equalTo(record::getInstId)
            .set(storageInstId).equalTo(record::getStorageInstId)
            .set(storageMasterInstId).equalTo(record::getStorageMasterInstId)
            .set(ip).equalTo(record::getIp)
            .set(port).equalTo(record::getPort)
            .set(xport).equalTo(record::getXport)
            .set(user).equalTo(record::getUser)
            .set(storageType).equalTo(record::getStorageType)
            .set(instKind).equalTo(record::getInstKind)
            .set(status).equalTo(record::getStatus)
            .set(regionId).equalTo(record::getRegionId)
            .set(azoneId).equalTo(record::getAzoneId)
            .set(idcId).equalTo(record::getIdcId)
            .set(maxConn).equalTo(record::getMaxConn)
            .set(cpuCore).equalTo(record::getCpuCore)
            .set(memSize).equalTo(record::getMemSize)
            .set(isVip).equalTo(record::getIsVip)
            .set(passwdEnc).equalTo(record::getPasswdEnc)
            .set(extras).equalTo(record::getExtras);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(StorageInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(id).equalToWhenPresent(record::getId)
            .set(gmtCreated).equalToWhenPresent(record::getGmtCreated)
            .set(gmtModified).equalToWhenPresent(record::getGmtModified)
            .set(instId).equalToWhenPresent(record::getInstId)
            .set(storageInstId).equalToWhenPresent(record::getStorageInstId)
            .set(storageMasterInstId).equalToWhenPresent(record::getStorageMasterInstId)
            .set(ip).equalToWhenPresent(record::getIp)
            .set(port).equalToWhenPresent(record::getPort)
            .set(xport).equalToWhenPresent(record::getXport)
            .set(user).equalToWhenPresent(record::getUser)
            .set(storageType).equalToWhenPresent(record::getStorageType)
            .set(instKind).equalToWhenPresent(record::getInstKind)
            .set(status).equalToWhenPresent(record::getStatus)
            .set(regionId).equalToWhenPresent(record::getRegionId)
            .set(azoneId).equalToWhenPresent(record::getAzoneId)
            .set(idcId).equalToWhenPresent(record::getIdcId)
            .set(maxConn).equalToWhenPresent(record::getMaxConn)
            .set(cpuCore).equalToWhenPresent(record::getCpuCore)
            .set(memSize).equalToWhenPresent(record::getMemSize)
            .set(isVip).equalToWhenPresent(record::getIsVip)
            .set(passwdEnc).equalToWhenPresent(record::getPasswdEnc)
            .set(extras).equalToWhenPresent(record::getExtras);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.42+08:00",
        comments = "Source Table: storage_info")
    default int updateByPrimaryKey(StorageInfo record) {
        return update(c ->
            c.set(gmtCreated).equalTo(record::getGmtCreated)
                .set(gmtModified).equalTo(record::getGmtModified)
                .set(instId).equalTo(record::getInstId)
                .set(storageInstId).equalTo(record::getStorageInstId)
                .set(storageMasterInstId).equalTo(record::getStorageMasterInstId)
                .set(ip).equalTo(record::getIp)
                .set(port).equalTo(record::getPort)
                .set(xport).equalTo(record::getXport)
                .set(user).equalTo(record::getUser)
                .set(storageType).equalTo(record::getStorageType)
                .set(instKind).equalTo(record::getInstKind)
                .set(status).equalTo(record::getStatus)
                .set(regionId).equalTo(record::getRegionId)
                .set(azoneId).equalTo(record::getAzoneId)
                .set(idcId).equalTo(record::getIdcId)
                .set(maxConn).equalTo(record::getMaxConn)
                .set(cpuCore).equalTo(record::getCpuCore)
                .set(memSize).equalTo(record::getMemSize)
                .set(isVip).equalTo(record::getIsVip)
                .set(passwdEnc).equalTo(record::getPasswdEnc)
                .set(extras).equalTo(record::getExtras)
                .where(id, isEqualTo(record::getId))
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2020-11-20T14:55:42.421+08:00",
        comments = "Source Table: storage_info")
    default int updateByPrimaryKeySelective(StorageInfo record) {
        return update(c ->
            c.set(gmtCreated).equalToWhenPresent(record::getGmtCreated)
                .set(gmtModified).equalToWhenPresent(record::getGmtModified)
                .set(instId).equalToWhenPresent(record::getInstId)
                .set(storageInstId).equalToWhenPresent(record::getStorageInstId)
                .set(storageMasterInstId).equalToWhenPresent(record::getStorageMasterInstId)
                .set(ip).equalToWhenPresent(record::getIp)
                .set(port).equalToWhenPresent(record::getPort)
                .set(xport).equalToWhenPresent(record::getXport)
                .set(user).equalToWhenPresent(record::getUser)
                .set(storageType).equalToWhenPresent(record::getStorageType)
                .set(instKind).equalToWhenPresent(record::getInstKind)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(regionId).equalToWhenPresent(record::getRegionId)
                .set(azoneId).equalToWhenPresent(record::getAzoneId)
                .set(idcId).equalToWhenPresent(record::getIdcId)
                .set(maxConn).equalToWhenPresent(record::getMaxConn)
                .set(cpuCore).equalToWhenPresent(record::getCpuCore)
                .set(memSize).equalToWhenPresent(record::getMemSize)
                .set(isVip).equalToWhenPresent(record::getIsVip)
                .set(passwdEnc).equalToWhenPresent(record::getPasswdEnc)
                .set(extras).equalToWhenPresent(record::getExtras)
                .where(id, isEqualTo(record::getId))
        );
    }
}