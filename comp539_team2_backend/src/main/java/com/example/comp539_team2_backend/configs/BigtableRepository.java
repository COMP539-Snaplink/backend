package com.example.comp539_team2_backend.configs;

import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.*;
import com.google.api.gax.rpc.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.logging.Logger;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.Filters;

import static com.google.cloud.bigtable.data.v2.models.Filters.*;
import com.google.protobuf.ByteString;


@Repository
public class BigtableRepository {

//    private final org.apache.hadoop.conf.Configuration config;
    private final String tableId;
    private static final Logger LOGGER = Logger.getLogger(BigtableRepository.class.getName());
    private final BigtableDataClient client;

    @Autowired
    public BigtableRepository(BigtableDataClient client, @Value("$bigtable.tableId") String tableId) {
        this.client = client;
        this.tableId = tableId;
    }



    public void save(String rowKey, String columnFamily, String columnQualifier, String data) throws IOException {
        try {
            RowMutation mutation = RowMutation.create(tableId, rowKey)
                    .setCell(columnFamily, columnQualifier, data);
            client.mutateRow(mutation);
            LOGGER.info("Data saved successfully for rowKey: " + rowKey);
        } catch (NotFoundException e) {
            LOGGER.severe("Failed to save data for rowKey " + rowKey + ": " + e.getMessage());
        }
    }

    public String get(String rowKey, String columnFamily, String columnQualifier) {
        try {
            Filter filter = FILTERS.chain()
                    .filter(FILTERS.family().exactMatch(columnFamily))
                    .filter(FILTERS.qualifier().exactMatch(columnQualifier));

            Row row = client.readRow(tableId, rowKey, filter);

            if (row != null && !row.getCells(columnFamily, columnQualifier).isEmpty()) {
                String value = row.getCells(columnFamily, columnQualifier).get(0).getValue().toStringUtf8();
                LOGGER.info("Retrieved data successfully for rowKey: " + rowKey);
                return value;
            } else {
                LOGGER.info("No data found for rowKey: " + rowKey);
                return null;
            }
        } catch (NotFoundException e) {
            LOGGER.severe("Row not found for rowKey " + rowKey + ": " + e.getMessage());
            return null;
        }
    }

    public void deleteRow(String rowKey) {
        try {
            RowMutation mutation = RowMutation.create(tableId, rowKey).deleteRow();
            client.mutateRow(mutation);
            LOGGER.info("Row deleted successfully for rowKey: " + rowKey);
        } catch (NotFoundException e) {
            LOGGER.severe("Failed to delete row for rowKey " + rowKey + ": " + e.getMessage());
        }
    }

    public void updateExpiration(String email) {
        Filters.Filter filter = Filters.FILTERS.qualifier().exactMatch("creator");

        Query query = Query.create(tableId).filter(filter);

        ServerStream<Row> rows = client.readRows(query);

        for (Row row : rows) {
            String value = row.getCells("url", "creator").get(0).getValue().toStringUtf8();
            if (email.equals(value)) {
                String rowKey = row.getKey().toStringUtf8();
                RowMutation mutation = RowMutation.create(tableId, rowKey)
                        .setCell("url", "expiredAt", ByteString.copyFromUtf8("NEVER").size());
                client.mutateRow(mutation);
            }
        }
        LOGGER.info("Finished updating expiration for user: " + email);
    }

    public void cleanExpirationData() {
        Filter timestampFilter = FILTERS.timestamp().range().endOpen(System.currentTimeMillis());

        Query query = Query.create(tableId)
                .filter(FILTERS.chain()
                        .filter(FILTERS.family().exactMatch("url"))
                        .filter(FILTERS.qualifier().exactMatch("expiredAt"))
                        .filter(timestampFilter));

        ServerStream<Row> rows = client.readRows(query);

        for (Row row : rows) {
            String expiredValue = row.getCells("url", "expiredAt").get(0).getValue().toStringUtf8();
            long expiredTime = Long.parseLong(expiredValue);
            if (expiredTime < System.currentTimeMillis()) {
                // Row is expired, perform deletion
                RowMutation mutation = RowMutation.create(tableId, row.getKey())
                        .deleteCells("url", "expiredAt");
                client.mutateRow(mutation);
            }
        }
    }

}

