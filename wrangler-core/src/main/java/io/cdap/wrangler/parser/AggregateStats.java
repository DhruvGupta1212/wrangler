/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
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
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A directive that aggregates byte sizes and time durations from specified columns.
 * It maintains running totals using the TransientStore and generates a summary row
 * with the total size, total time, and row count.
 */
@Plugin(type = Directive.TYPE)
@Name(AggregateStats.NAME)
@Categories(categories = {"aggregate"})
@Description("Aggregates byte sizes and time durations from specified columns")
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  
  private static final String TOTAL_SIZE_KEY = "total_size";
  private static final String TOTAL_TIME_KEY = "total_time";
  private static final String ROW_COUNT_KEY = "row_count";

  private ColumnName sizeColumn;
  private ColumnName timeColumn;
  private ColumnName targetSizeColumn;
  private ColumnName targetTimeColumn;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("size-column", TokenType.COLUMN_NAME, "Column with byte-size values");
    builder.define("time-column", TokenType.COLUMN_NAME, "Column with time-duration values");
    builder.define("target-size-column", TokenType.COLUMN_NAME, "Output column for total size");
    builder.define("target-time-column", TokenType.COLUMN_NAME, "Output column for total time");
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    try {
      sizeColumn = args.value("size-column");
      timeColumn = args.value("time-column");
      targetSizeColumn = args.value("target-size-column");
      targetTimeColumn = args.value("target-time-column");
    } catch (Exception e) {
      throw new DirectiveParseException(
          String.format("Error initializing directive '%s': %s", NAME, e.getMessage()));
    }
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    TransientStore store = context.getTransientStore();
    
    // Initialize store if not already done
    if (store.get(TOTAL_SIZE_KEY) == null) {
      store.set(TransientVariableScope.GLOBAL, TOTAL_SIZE_KEY, 0L);
      store.set(TransientVariableScope.GLOBAL, TOTAL_TIME_KEY, 0L);
      store.set(TransientVariableScope.GLOBAL, ROW_COUNT_KEY, 0L);
    }

    // Process each row
    for (Row row : rows) {
      try {
        // Get and convert byte size
        Object sizeObj = row.getValue(sizeColumn.value());
        if (sizeObj instanceof ByteSize) {
          ByteSize byteSize = (ByteSize) sizeObj;
          store.increment(TransientVariableScope.GLOBAL, TOTAL_SIZE_KEY, byteSize.getBytes());
        }

        // Get and convert time duration
        Object timeObj = row.getValue(timeColumn.value());
        if (timeObj instanceof TimeDuration) {
          TimeDuration timeDuration = (TimeDuration) timeObj;
          store.increment(TransientVariableScope.GLOBAL, TOTAL_TIME_KEY, timeDuration.getMilliseconds());
        }

        store.increment(TransientVariableScope.GLOBAL, ROW_COUNT_KEY, 1L);
      } catch (Exception e) {
        throw new DirectiveExecutionException(
            String.format("Error processing row in directive '%s': %s", NAME, e.getMessage()));
      }
    }

    // If this is the last batch, create a summary row with unit conversions
    if (context.getEnvironment() == ExecutorContext.Environment.TRANSFORM) {
      long totalSizeBytes = (Long) store.get(TOTAL_SIZE_KEY);
      long totalTimeMs = (Long) store.get(TOTAL_TIME_KEY);
      long rowCount = (Long) store.get(ROW_COUNT_KEY);

      // Convert bytes to MB (1 MB = 1024 * 1024 bytes)
      double totalSizeMB = totalSizeBytes / (1024.0 * 1024.0);
      
      // Convert milliseconds to seconds (1 second = 1000 milliseconds)
      double totalTimeSec = totalTimeMs / 1000.0;

      Row summary = new Row();
      summary.add(targetSizeColumn.value(), totalSizeMB);
      summary.add(targetTimeColumn.value(), totalTimeSec);
      summary.add("row_count", rowCount);

      // Reset store for next batch
      store.reset(TransientVariableScope.GLOBAL);

      return List.of(summary);
    }

    // Return empty list during processing to avoid duplicate rows
    return Collections.emptyList();
  }

  @Override
  public void destroy() {
    // No-op
  }
}
