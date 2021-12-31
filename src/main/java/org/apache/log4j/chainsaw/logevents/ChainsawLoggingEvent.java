/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * The ChainsawLoggingEvent is a Chainsaw-specific type of logging event.  This
 * ensures that logging events that we get from other sources all conform to
 * the same type of event, not just the Log4j1 style of logging event.
 *
 * ChainsawLoggingEvents are immutable, so use the ChainsawLoggingEventBuilder
 * class to construct one.
 */
public class ChainsawLoggingEvent {

    public final ZonedDateTime m_timestamp;
    public final Level m_level;
    public final String m_message;
    public final String m_threadName;
    public final String m_logger;
    public final LocationInfo m_locationInfo;
    public final String m_ndc;
    public final Map<String,String> m_mdc;

    ChainsawLoggingEvent( ChainsawLoggingEventBuilder b ){
        m_timestamp = b.m_timestamp;
        m_level = b.m_level;
        m_message = b.m_message;
        m_threadName = b.m_threadName;
        m_logger = b.m_logger;
        m_locationInfo = b.m_locationInfo;
        m_ndc = b.m_ndc;
        m_mdc = b.m_mdc;
    }
}
