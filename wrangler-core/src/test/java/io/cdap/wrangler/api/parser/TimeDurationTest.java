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

package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TimeDuration} class
 */
public class TimeDurationTest {

  @Test
  public void testParseDuration() {
    // Test milliseconds
    TimeDuration ms = new TimeDuration("100ms");
    Assert.assertEquals(100L, ms.getMilliseconds());
    
    // Test seconds
    TimeDuration sec = new TimeDuration("1.5s");
    Assert.assertEquals(1500L, sec.getMilliseconds());
    
    // Test minutes
    TimeDuration min = new TimeDuration("2m");
    Assert.assertEquals(2L * 60 * 1000, min.getMilliseconds());
    
    // Test hours
    TimeDuration hour = new TimeDuration("0.5h");
    Assert.assertEquals((long) (0.5 * 60 * 60 * 1000), hour.getMilliseconds());
  }

  @Test
  public void testCaseInsensitive() {
    TimeDuration ms1 = new TimeDuration("100MS");
    TimeDuration ms2 = new TimeDuration("100ms");
    Assert.assertEquals(ms1.getMilliseconds(), ms2.getMilliseconds());
    
    TimeDuration sec1 = new TimeDuration("1.5S");
    TimeDuration sec2 = new TimeDuration("1.5s");
    Assert.assertEquals(sec1.getMilliseconds(), sec2.getMilliseconds());
  }

  @Test
  public void testWhitespace() {
    TimeDuration ms1 = new TimeDuration(" 100 ms ");
    TimeDuration ms2 = new TimeDuration("100ms");
    Assert.assertEquals(ms1.getMilliseconds(), ms2.getMilliseconds());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("100msec");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNumber() {
    new TimeDuration("abcms");
  }

  @Test
  public void testValueAndType() {
    TimeDuration ms = new TimeDuration("100ms");
    Assert.assertEquals(100L, ms.value());
    Assert.assertEquals(TokenType.TIME_DURATION, ms.type());
  }
} 