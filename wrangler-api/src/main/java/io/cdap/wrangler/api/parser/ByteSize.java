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


package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Token that represents a ByteSize value like KB, MB, GB, etc.
 */


public class ByteSize implements Token {
  private final long bytes;

  public ByteSize(String value) {
    this.bytes = parseBytes(value);
  }

  private long parseBytes(String val) {
    val = val.trim().toUpperCase();
    if (val.endsWith("KB")) {
      return (long) (Double.parseDouble(val.replace("KB", "")) * 1024);
    }
    if (val.endsWith("MB")) {
      return (long) (Double.parseDouble(val.replace("MB", "")) * 1024 * 1024);
    } 
    if (val.endsWith("GB")) {
      return (long) (Double.parseDouble(val.replace("GB", "")) * 1024 * 1024 * 1024);
    } 
    if (val.endsWith("TB")) {
      return (long) (Double.parseDouble(val.replace("TB", "")) * 1024L * 1024 * 1024 * 1024);
    } 
    if (val.endsWith("B")) {
      return Long.parseLong(val.replace("B", ""));
    } 
    throw new IllegalArgumentException("Invalid ByteSize value: " + val);
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  public long getBytes() {
    return bytes;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(bytes);
  }
}
