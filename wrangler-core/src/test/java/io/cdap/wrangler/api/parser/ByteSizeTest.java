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
 * Tests for {@link ByteSize} class
 */
public class ByteSizeTest {

  @Test
  public void testParseBytes() {
    // Test KB
    ByteSize kb = new ByteSize("10KB");
    Assert.assertEquals(10 * 1024L, kb.getBytes());
    
    // Test MB
    ByteSize mb = new ByteSize("1.5MB");
    Assert.assertEquals((long) (1.5 * 1024 * 1024), mb.getBytes());
    
    // Test GB
    ByteSize gb = new ByteSize("2GB");
    Assert.assertEquals(2L * 1024 * 1024 * 1024, gb.getBytes());
    
    // Test TB
    ByteSize tb = new ByteSize("0.5TB");
    Assert.assertEquals((long) (0.5 * 1024 * 1024 * 1024 * 1024), tb.getBytes());
    
    // Test bytes
    ByteSize bytes = new ByteSize("1024B");
    Assert.assertEquals(1024L, bytes.getBytes());
  }

  @Test
  public void testCaseInsensitive() {
    ByteSize kb1 = new ByteSize("10kb");
    ByteSize kb2 = new ByteSize("10KB");
    Assert.assertEquals(kb1.getBytes(), kb2.getBytes());
    
    ByteSize mb1 = new ByteSize("1.5mb");
    ByteSize mb2 = new ByteSize("1.5MB");
    Assert.assertEquals(mb1.getBytes(), mb2.getBytes());
  }

  @Test
  public void testWhitespace() {
    ByteSize kb1 = new ByteSize(" 10 KB ");
    ByteSize kb2 = new ByteSize("10KB");
    Assert.assertEquals(kb1.getBytes(), kb2.getBytes());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new ByteSize("10K");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNumber() {
    new ByteSize("abcKB");
  }

  @Test
  public void testValueAndType() {
    ByteSize kb = new ByteSize("10KB");
    Assert.assertEquals(10 * 1024L, kb.value());
    Assert.assertEquals(TokenType.BYTE_SIZE, kb.type());
  }
} 