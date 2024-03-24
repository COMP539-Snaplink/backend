package com.example.comp539_team2_backend.configs;

import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;

public class BigtableRepository {

    private final org.apache.hadoop.conf.Configuration config;
    private final String tableId;

    public BigtableRepository(String projectId, String instanceId, String tableId) {
        this.config = BigtableConfiguration.configure(projectId, instanceId);
        this.tableId = tableId;
    }

    public void save(String rowKey, String columnFamily, String columnQualifier, String data) throws IOException {
        try (Connection connection = ConnectionFactory.createConnection((org.apache.hadoop.conf.Configuration) config);
             Table table = connection.getTable(TableName.valueOf(tableId))) {
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier), Bytes.toBytes(data));
            table.put(put);
        }
    }

    public String get(String rowKey, String columnFamily, String columnQualifier) throws IOException {
        try (Connection connection = ConnectionFactory.createConnection((org.apache.hadoop.conf.Configuration) config);
             Table table = connection.getTable(TableName.valueOf(tableId))) {
            Result result = table.get(new Get(Bytes.toBytes(rowKey)));
            return Bytes.toString(result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier)));
        }
    }
}

