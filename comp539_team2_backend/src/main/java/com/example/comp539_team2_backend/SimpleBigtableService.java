package com.example.comp539_team2_backend;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
public class SimpleBigtableService {

    String projectId = "rice-comp-539-spring-2022"; // my-gcp-project-id
    String instanceId = "shared-539" ; // my-bigtable-instance-id
    String tableId =  "spring24-team2-snaplink"; // my-bigtable-table-id

    private static final byte[] COLUMN_FAMILY_NAME = Bytes.toBytes("test");
    private static final byte[] COLUMN_NAME = Bytes.toBytes("greeting");

    public SimpleBigtableService() {
    }

    public String InsertandReadData() throws IOException {
        Configuration config = BigtableConfiguration.configure(projectId, instanceId);
        try (Connection connection = ConnectionFactory.createConnection(config)) {
            System.out.println("--- Connection established with Bigtable Instance ---");
            // Create a connection to the table that already exists
            // Use try-with-resources to make sure the connection to the table is closed correctly
            try (Table table = connection.getTable(TableName.valueOf(tableId))) {

                //insert a row
                String rowKey = "greeting";
                System.out.printf("--- inserting for row-key: %s for provided table: %s ---\n",
                        rowKey, tableId);
                Put put = new Put(Bytes.toBytes(rowKey));
                put.addColumn(COLUMN_FAMILY_NAME, COLUMN_NAME, Bytes.toBytes("Hello World"));
                table.put(put);

                System.out.printf("--- Reading for row-key: %s for provided table: %s ---\n",
                        rowKey, tableId);

                // Retrieve the result
                Result result = table.get(new Get(Bytes.toBytes(rowKey)));

                // Convert row data to string
                String rowValue = Bytes.toString(result.value());

                System.out.printf("--- Value Read: %s ---\n",
                        rowValue);

                return rowValue;


            }  catch (IOException e) {
                // handle exception while connecting to a table
                throw e;
            }
        } catch (IOException e) {
            System.err.println("Exception while running quickstart: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

