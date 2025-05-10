/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration tests for AggregateStats directive using TestingRig
 */
public class AggregateStatsIntegrationTest {

  @Test
  public void testBasicAggregation() throws Exception {
    // Create sample data
    List<Row> rows = new ArrayList<>();
    
    // Row 1: 1MB transfer, 1.5s response time
    Row row1 = new Row();
    row1.add("data_transfer_size", new ByteSize("1MB"));
    row1.add("response_time", new TimeDuration("1.5s"));
    rows.add(row1);
    
    // Row 2: 2.5MB transfer, 500ms response time
    Row row2 = new Row();
    row2.add("data_transfer_size", new ByteSize("2.5MB"));
    row2.add("response_time", new TimeDuration("500ms"));
    rows.add(row2);
    
    // Row 3: 512KB transfer, 2s response time
    Row row3 = new Row();
    row3.add("data_transfer_size", new ByteSize("512KB"));
    row3.add("response_time", new TimeDuration("2s"));
    rows.add(row3);

    // Define recipe
    String[] recipe = new String[] {
      "aggregate-stats data_transfer_size response_time total_size_mb total_time_sec"
    };

    // Execute recipe
    List<Row> results = TestingRig.execute(recipe, rows);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row summary = results.get(0);

    // Expected total size: 1MB + 2.5MB + 512KB = 4MB + 0.5MB = 4.5MB
    // 1MB = 1024 * 1024 bytes
    // 2.5MB = 2.5 * 1024 * 1024 bytes
    // 512KB = 512 * 1024 bytes = 0.5 * 1024 * 1024 bytes
    double expectedTotalSizeInMB = 4.5;
    Assert.assertEquals(expectedTotalSizeInMB, ((Double) summary.getValue("total_size_mb")).doubleValue(), 0.001);

    // Expected total time: 1.5s + 0.5s + 2s = 4s
    // 1.5s = 1500ms
    // 500ms = 0.5s
    // 2s = 2000ms
    // Total: 4000ms = 4s
    double expectedTotalTimeInSeconds = 4.0;
    Assert.assertEquals(expectedTotalTimeInSeconds, ((Double) summary.getValue("total_time_sec")).doubleValue(), 0.001);
  }

  @Test
  public void testMixedUnits() throws Exception {
    // Create sample data with mixed units
    List<Row> rows = new ArrayList<>();
    
    // Row 1: 1024KB transfer (1MB), 1500ms response time
    Row row1 = new Row();
    row1.add("data_transfer_size", new ByteSize("1024KB"));
    row1.add("response_time", new TimeDuration("1500ms"));
    rows.add(row1);
    
    // Row 2: 1GB transfer, 0.5h response time
    Row row2 = new Row();
    row2.add("data_transfer_size", new ByteSize("1GB"));
    row2.add("response_time", new TimeDuration("0.5h"));
    rows.add(row2);
    
    // Row 3: 2048B transfer, 120s response time
    Row row3 = new Row();
    row3.add("data_transfer_size", new ByteSize("2048B"));
    row3.add("response_time", new TimeDuration("120s"));
    rows.add(row3);

    // Define recipe
    String[] recipe = new String[] {
      "aggregate-stats data_transfer_size response_time total_size_mb total_time_sec"
    };

    // Execute recipe
    List<Row> results = TestingRig.execute(recipe, rows);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row summary = results.get(0);

    // Expected total size:
    // 1024KB = 1MB = 1 * 1024 * 1024 bytes
    // 1GB = 1024MB = 1024 * 1024 * 1024 bytes
    // 2048B = 0.001953125MB
    // Total ≈ 1025.001953125 MB
    double expectedTotalSizeInMB = 1025.001953125;
    Assert.assertEquals(expectedTotalSizeInMB, ((Double) summary.getValue("total_size_mb")).doubleValue(), 0.001);

    // Expected total time:
    // 1500ms = 1.5s
    // 0.5h = 1800s
    // 120s = 120s
    // Total = 1921.5s
    double expectedTotalTimeInSeconds = 1921.5;
    Assert.assertEquals(expectedTotalTimeInSeconds, ((Double) summary.getValue("total_time_sec")).doubleValue(), 0.001);
  }

  @Test
  public void testEmptyInput() throws Exception {
    List<Row> rows = new ArrayList<>();

    String[] recipe = new String[] {
      "aggregate-stats data_transfer_size response_time total_size_mb total_time_sec"
    };

    List<Row> results = TestingRig.execute(recipe, rows);

    Assert.assertEquals(1, results.size());
    Row summary = results.get(0);
    Assert.assertEquals(0.0, ((Double) summary.getValue("total_size_mb")).doubleValue(), 0.001);
    Assert.assertEquals(0.0, ((Double) summary.getValue("total_time_sec")).doubleValue(), 0.001);
  }

  @Test
  public void testLargeNumbers() throws Exception {
    List<Row> rows = new ArrayList<>();
    
    // Add a row with large values to test numeric overflow handling
    Row row = new Row();
    row.add("data_transfer_size", new ByteSize("1TB"));
    row.add("response_time", new TimeDuration("24h"));
    rows.add(row);

    String[] recipe = new String[] {
      "aggregate-stats data_transfer_size response_time total_size_mb total_time_sec"
    };

    List<Row> results = TestingRig.execute(recipe, rows);

    Assert.assertEquals(1, results.size());
    Row summary = results.get(0);

    // 1TB = 1024 * 1024 MB
    double expectedTotalSizeInMB = 1024 * 1024;
    Assert.assertEquals(expectedTotalSizeInMB, ((Double) summary.getValue("total_size_mb")).doubleValue(), 0.001);

    // 24h = 24 * 60 * 60 seconds
    double expectedTotalTimeInSeconds = 24 * 60 * 60;
    Assert.assertEquals(expectedTotalTimeInSeconds, ((Double) summary.getValue("total_time_sec")).doubleValue(), 0.001);
  }
} 