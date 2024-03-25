package com.example.comp539_team2_backend.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Repository;
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

    public void deleteColumn(String rowKey, String columnFamily, String columnQualifier) throws IOException {
        try (Connection conn = ConnectionFactory.createConnection(config);
             Table table = conn.getTable(TableName.valueOf(tableId))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier));
            table.delete(delete);
        }
    }

    public void deleteRow(String rowKey) throws IOException {
        try (Connection connection = ConnectionFactory.createConnection(config);
             Table table = connection.getTable(TableName.valueOf(tableId))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
        }
    }
}

