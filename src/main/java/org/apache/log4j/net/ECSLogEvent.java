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
package org.apache.log4j.net;

import com.owlike.genson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.Hashtable;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * Represents a LogEvent as from a ECS(ElasticSearch) event.
 */
public class ECSLogEvent {
    @JsonProperty("@timestamp")
    public String timestamp;
    @JsonProperty("log.level")
    public String level;
    public String message;
    @JsonProperty("process.thread.name")
    public String thread_name;
    @JsonProperty("log.logger")
    public String logger;
    public List<String> tags;

    ChainsawLoggingEvent toChainsawLoggingEvent( ChainsawLoggingEventBuilder build ){
        build.clear();

        build.setLevelFromString( level )
                .setMessage(message)
                .setLogger(logger)
                .setThreadName(thread_name)
                .setTimestamp(ZonedDateTime.parse(timestamp).toInstant());

        return build.create();
    }

    LoggingEvent toLoggingEvent(){
        Logger logger;
        long timeStamp;
        Level level;
        String ndc = null;
        String[] exception = null;
        String className = null;
        String methodName = null;
        String fileName = null;
        String lineNumber = null;
        Hashtable properties = null;

        logger = Logger.getLogger(this.logger);
        timeStamp = ZonedDateTime.parse(this.timestamp).toInstant().toEpochMilli();
        level = Level.toLevel(this.level);

        LocationInfo info;
        if ((fileName != null)
            || (className != null)
            || (methodName != null)
            || (lineNumber != null)) {
            info = new LocationInfo(fileName, className, methodName, lineNumber);
        } else {
            info = LocationInfo.NA_LOCATION_INFO;
        }
        ThrowableInformation throwableInfo = null;
        if (exception != null) {
            throwableInfo = new ThrowableInformation(exception);
        }

        LoggingEvent loggingEvent = new LoggingEvent(null,
                logger, timeStamp, level, message,
                this.thread_name,
                throwableInfo,
                ndc,
                info,
                properties);

        return loggingEvent;
    }
}
