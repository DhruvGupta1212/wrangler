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

import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link AggregateStats} directive
 */
public class AggregateStatsTest {
  private AggregateStats directive;
  private ExecutorContext context;
  private TransientStore store;

  @Before
  public void setUp() throws DirectiveParseException {
    directive = new AggregateStats();
    context = Mockito.mock(ExecutorContext.class);
    store = Mockito.mock(TransientStore.class);
    
    Mockito.when(context.getTransientStore()).thenReturn(store);
    Mockito.when(context.getEnvironment()).thenReturn(ExecutorContext.Environment.TRANSFORM);
    
    // Initialize directive with test columns
    directive.initialize(new TestArguments(
        new ColumnName("size"),
        new ColumnName("time"),
        new ColumnName("total_size"),
        new ColumnName("total_time")
    ));
  }

  @Test
  public void testBasicAggregation() throws DirectiveExecutionException {
    // Setup test data
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("size", new ByteSize("1MB"));
    row1.add("time", new TimeDuration("1s"));
    rows.add(row1);

    Row row2 = new Row();
    row2.add("size", new ByteSize("2MB"));
    row2.add("time", new TimeDuration("2s"));
    rows.add(row2);

    // Mock store behavior
    Mockito.when(store.get("total_size")).thenReturn(3L * 1024 * 1024); // 3MB in bytes
    Mockito.when(store.get("total_time")).thenReturn(3000L); // 3s in milliseconds
    Mockito.when(store.get("row_count")).thenReturn(2L);

    // Execute directive
    List<Row> result = directive.execute(rows, context);

    // Verify results
    Assert.assertEquals(1, result.size());
    Row summary = result.get(0);
    Assert.assertEquals(3.0, summary.getValue("total_size")); // 3MB
    Assert.assertEquals(3.0, summary.getValue("total_time")); // 3s
    Assert.assertEquals(2L, summary.getValue("row_count"));
  }

  @Test
  public void testEmptyInput() throws DirectiveExecutionException {
    List<Row> rows = new ArrayList<>();
    
    // Mock store behavior for empty input
    Mockito.when(store.get("total_size")).thenReturn(0L);
    Mockito.when(store.get("total_time")).thenReturn(0L);
    Mockito.when(store.get("row_count")).thenReturn(0L);

    List<Row> result = directive.execute(rows, context);
    
    Assert.assertEquals(1, result.size());
    Row summary = result.get(0);
    Assert.assertEquals(0.0, summary.getValue("total_size"));
    Assert.assertEquals(0.0, summary.getValue("total_time"));
    Assert.assertEquals(0L, summary.getValue("row_count"));
  }

  @Test
  public void testUnitConversions() throws DirectiveExecutionException {
    // Setup test data with different units
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("size", new ByteSize("1KB"));
    row1.add("time", new TimeDuration("500ms"));
    rows.add(row1);

    Row row2 = new Row();
    row2.add("size", new ByteSize("1MB"));
    row2.add("time", new TimeDuration("1.5s"));
    rows.add(row2);

    // Mock store behavior
    Mockito.when(store.get("total_size")).thenReturn(1024L + 1024L * 1024L); // 1KB + 1MB in bytes
    Mockito.when(store.get("total_time")).thenReturn(500L + 1500L); // 500ms + 1.5s in milliseconds
    Mockito.when(store.get("row_count")).thenReturn(2L);

    // Execute directive
    List<Row> result = directive.execute(rows, context);

    // Verify results with unit conversions
    Assert.assertEquals(1, result.size());
    Row summary = result.get(0);
    Assert.assertEquals(1.0009765625, summary.getValue("total_size")); // (1KB + 1MB) in MB
    Assert.assertEquals(2.0, summary.getValue("total_time")); // (500ms + 1.5s) in seconds
    Assert.assertEquals(2L, summary.getValue("row_count"));
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testInvalidSizeValue() throws DirectiveExecutionException {
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("size", "invalid");
    row.add("time", new TimeDuration("1s"));
    rows.add(row);

    directive.execute(rows, context);
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testInvalidTimeValue() throws DirectiveExecutionException {
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("size", new ByteSize("1MB"));
    row.add("time", "invalid");
    rows.add(row);

    directive.execute(rows, context);
  }

  @Test
  public void testProcessingPhase() throws DirectiveExecutionException {
    // Set environment to TESTING
    Mockito.when(context.getEnvironment()).thenReturn(ExecutorContext.Environment.TESTING);

    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("size", new ByteSize("1MB"));
    row.add("time", new TimeDuration("1s"));
    rows.add(row);

    // Execute directive
    List<Row> result = directive.execute(rows, context);

    // Should return empty list during processing
    Assert.assertTrue(result.isEmpty());
  }

  /**
   * Helper class to provide test arguments
   */
  private static class TestArguments implements io.cdap.wrangler.api.Arguments {
    private final ColumnName sizeColumn;
    private final ColumnName timeColumn;
    private final ColumnName targetSizeColumn;
    private final ColumnName targetTimeColumn;

    TestArguments(ColumnName sizeColumn, ColumnName timeColumn, 
                 ColumnName targetSizeColumn, ColumnName targetTimeColumn) {
      this.sizeColumn = sizeColumn;
      this.timeColumn = timeColumn;
      this.targetSizeColumn = targetSizeColumn;
      this.targetTimeColumn = targetTimeColumn;
    }

    @Override
    public <T extends Token> T value(String name) {
      switch (name) {
        case "size-column":
          return (T) sizeColumn;
        case "time-column":
          return (T) timeColumn;
        case "target-size-column":
          return (T) targetSizeColumn;
        case "target-time-column":
          return (T) targetTimeColumn;
        default:
          throw new IllegalArgumentException("Unknown argument: " + name);
      }
    }

    @Override
    public boolean contains(String name) {
      return name.equals("size-column") || name.equals("time-column") ||
             name.equals("target-size-column") || name.equals("target-time-column");
    }

    @Override
    public int size() {
      return 4;
    }

    @Override
    public String source() {
      return "test";
    }

    @Override
    public int line() {
      return 1;
    }

    @Override
    public int column() {
      return 1;
    }

    @Override
    public TokenType type(String name) {
      return TokenType.COLUMN_NAME;
    }

    @Override
    public com.google.gson.JsonElement toJson() {
      com.google.gson.JsonObject json = new com.google.gson.JsonObject();
      json.addProperty("size-column", sizeColumn.value());
      json.addProperty("time-column", timeColumn.value());
      json.addProperty("target-size-column", targetSizeColumn.value());
      json.addProperty("target-time-column", targetTimeColumn.value());
      return json;
    }
  }
} 