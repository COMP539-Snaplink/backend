package com.example.comp539_team2_backend.configs;

import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.logging.Logger;

public class BigtableRepository {

    private final org.apache.hadoop.conf.Configuration config;
    private final String tableId;
    private static final Logger LOGGER = Logger.getLogger(BigtableRepository.class.getName());

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
            LOGGER.info("Data Saved successfully for rowKey: " + rowKey);
        } catch (Exception e) {
            LOGGER.severe("Failed to save data for rowKey " + rowKey + ": " + e.getMessage());
            throw e;
        }
    }

    public String get(String rowKey, String columnFamily, String columnQualifier) throws IOException {
        try {
            Connection connection = ConnectionFactory.createConnection((org.apache.hadoop.conf.Configuration) config);
            Table table = connection.getTable(TableName.valueOf(tableId));
            Result result = table.get(new Get(Bytes.toBytes(rowKey)));
            LOGGER.info("Retrieved data successfully for rowKey:" + rowKey);
            return Bytes.toString(result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier)));
        } catch (IOException e) {
            LOGGER.severe("Failed to establish connection to Bigtable: " + e.getMessage());
            Throwable cause = e.getCause();
            Objects.requireNonNullElse(cause, e).printStackTrace();
            throw e;
        }
    }


    public void deleteColumn(String rowKey, String columnFamily, String columnQualifier) throws IOException {
        try (Connection conn = ConnectionFactory.createConnection(config);
             Table table = conn.getTable(TableName.valueOf(tableId))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier));
            table.delete(delete);
            LOGGER.info("Column deleted successfully for rowKey: " + rowKey);
        } catch (IOException e) {
            Throwable cause = e.getCause();
            Objects.requireNonNullElse(cause, e).printStackTrace();
            LOGGER.severe("Failed to delete column for rowKey " + rowKey + ": " + e.getCause().getMessage());
            throw e;
        }
    }

    public void deleteRow(String rowKey) throws IOException {
        try (Connection connection = ConnectionFactory.createConnection(config);
             Table table = connection.getTable(TableName.valueOf(tableId))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            LOGGER.info("Row deleted successfully for rowKey: " + rowKey);
        } catch (IOException e) {
            LOGGER.severe("Failed to delete row for rowKey " + rowKey + ": " + e.getMessage());
            throw e;
        }
    }

    public void updateExpiration(String email) throws IOException {
        try (Connection connection = ConnectionFactory.createConnection(config);
        Table table = connection.getTable(TableName.valueOf(tableId))) {
            SingleColumnValueFilter filter = new SingleColumnValueFilter(
                    Bytes.toBytes("url"),
                    Bytes.toBytes("creator"),
                    CompareFilter.CompareOp.EQUAL,
                    Bytes.toBytes(email)
            );

            Scan scan = new Scan();
            scan.setFilter(filter);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    byte[] rowKey = result.getRow();
                    Put put = new Put(rowKey);
                    put.addColumn(Bytes.toBytes("url"), Bytes.toBytes("expiredAt"), Bytes.toBytes("NEVER"));
                    table.put(put);
                }
            }
            LOGGER.info("Finish to update expiration for user: " + email);
        } catch (IOException e) {
            LOGGER.severe("Failed to update expiration for user: " + email + ": " + e.getMessage());
            throw e;
        }
    }
}

