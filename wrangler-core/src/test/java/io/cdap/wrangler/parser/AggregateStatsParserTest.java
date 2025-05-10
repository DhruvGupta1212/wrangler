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
import io.cdap.wrangler.api.CompileStatus;
import io.cdap.wrangler.api.Compiler;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.RecipeParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for parsing the AggregateStats directive syntax
 */
public class AggregateStatsParserTest {

  @Test
  public void testValidSyntax() throws Exception {
    String[] recipe = new String[] {
      "aggregate-stats size-column:size time-column:time " +
      "target-size-column:total_size target-time-column:total_time"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    List<Directive> directives = parser.parse();
    Assert.assertEquals(1, directives.size());
    Assert.assertTrue(directives.get(0) instanceof AggregateStats);
  }

  @Test
  public void testValidSyntaxWithWhitespace() throws Exception {
    String[] recipe = new String[] {
      "aggregate-stats size-column: size time-column: time " +
      "target-size-column: total_size target-time-column: total_time"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    List<Directive> directives = parser.parse();
    Assert.assertEquals(1, directives.size());
    Assert.assertTrue(directives.get(0) instanceof AggregateStats);
  }

  @Test(expected = Exception.class)
  public void testMissingRequiredArguments() throws Exception {
    String[] recipe = new String[] {
      "aggregate-stats size-column:size time-column:time"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    parser.parse();
  }

  @Test(expected = Exception.class)
  public void testInvalidColumnName() throws Exception {
    String[] recipe = new String[] {
      "aggregate-stats size-column:size time-column:time " +
      "target-size-column:total size target-time-column:total_time"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    parser.parse();
  }

  @Test(expected = Exception.class)
  public void testInvalidDirectiveName() throws Exception {
    String[] recipe = new String[] {
      "aggregate-stats-invalid size-column:size time-column:time " +
      "target-size-column:total_size target-time-column:total_time"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    parser.parse();
  }

  @Test
  public void testMultipleDirectives() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv :body ',' true",
      "aggregate-stats size-column:size time-column:time " +
      "target-size-column:total_size target-time-column:total_time",
      "rename :total_size :final_size"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    List<Directive> directives = parser.parse();
    Assert.assertEquals(3, directives.size());
    Assert.assertTrue(directives.get(1) instanceof AggregateStats);
  }
} 