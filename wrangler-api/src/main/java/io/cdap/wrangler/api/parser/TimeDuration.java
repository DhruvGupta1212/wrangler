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
 * Token that represents a TimeDuration value like ms, s, min, etc.
 */


public class TimeDuration implements Token {
  private final long milliseconds;

  public TimeDuration(String value) {
    this.milliseconds = parseMilliseconds(value);
  }

  private long parseMilliseconds(String val) {
    val = val.trim().toLowerCase();
    if (val.endsWith("ms")) {
  return (long) Double.parseDouble(val.replace("ms", ""));
}
if (val.endsWith("ns")) {
  return (long) (Double.parseDouble(val.replace("ns", "")) / 1_000_000);
}
if (val.endsWith("s")) {
  return (long) (Double.parseDouble(val.replace("s", "")) * 1000);
}
if (val.endsWith("min")) {
  return (long) (Double.parseDouble(val.replace("min", "")) * 60000);
}
if (val.endsWith("h")) {
  return (long) (Double.parseDouble(val.replace("h", "")) * 3600000);
}

    throw new IllegalArgumentException("Invalid TimeDuration value: " + val);
  }

  @Override
  public Object value() {
    return milliseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  public long getMilliseconds() {
    return milliseconds;
  }

  public double getSeconds() {
    return milliseconds / 1000.0;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(milliseconds);
  }
}
