/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.chainsaw.logevents;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ChainsawLoggingEventBuilder {

    // All fields are package-private so that ChainsawLoggingEvent can access
    Instant m_timestamp;
    Level m_level;
    String m_message;
    String m_threadName;
    String m_logger;
    LocationInfo m_locationInfo;
    String m_ndc;
    Map<String, String> m_mdc;

    public ChainsawLoggingEventBuilder() {}

    /**
     * Copy fields from an already-existing log event.
     * Clears any data that already exists in this builder.
     */
    public ChainsawLoggingEventBuilder copyFromEvent(final ChainsawLoggingEvent evt) {
        clear();
        m_timestamp = evt.m_timestamp;
        m_level = evt.m_level;
        m_message = evt.m_message;
        m_threadName = evt.m_threadName;
        m_logger = evt.m_logger;
        m_locationInfo = evt.m_locationInfo;
        m_ndc = evt.m_ndc;
        if (evt.m_mdc != null) {
            m_mdc = new HashMap<>(evt.m_mdc);
        }
        return this;
    }

    public void clear() {
        m_timestamp = null;
        m_level = null;
        m_message = null;
        m_logger = null;
        m_locationInfo = null;
        m_ndc = null;
        m_mdc = null;
    }

    public ChainsawLoggingEventBuilder setTimestamp(Instant inTimestamp) {
        m_timestamp = inTimestamp;
        return this;
    }

    public ChainsawLoggingEventBuilder setLevel(Level inLevel) {
        m_level = inLevel;
        return this;
    }

    public ChainsawLoggingEventBuilder setLevelFromString(String inLevel) {
        m_level = Level.valueOf(inLevel);
        return this;
    }

    public ChainsawLoggingEventBuilder setMessage(String inMessage) {
        m_message = inMessage;
        return this;
    }

    public ChainsawLoggingEventBuilder setThreadName(String threadName) {
        m_threadName = threadName;
        return this;
    }

    public ChainsawLoggingEventBuilder setLogger(String logger) {
        m_logger = logger;
        return this;
    }

    public ChainsawLoggingEventBuilder setLocationInfo(LocationInfo info) {
        m_locationInfo = info;
        return this;
    }

    public ChainsawLoggingEventBuilder setNDC(String ndc) {
        m_ndc = ndc;
        return this;
    }

    public ChainsawLoggingEventBuilder setMDC(Map<String, String> mdc) {
        m_mdc = mdc;
        return this;
    }

    public ChainsawLoggingEventBuilder addMDCEntry(String key, String value) {
        if (m_mdc == null) {
            m_mdc = new HashMap<>();
        }
        m_mdc.put(key, value);
        return this;
    }

    public ChainsawLoggingEvent create() {
        return new ChainsawLoggingEvent(this);
    }
}
