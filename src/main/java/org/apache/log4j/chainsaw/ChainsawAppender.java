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
package org.apache.log4j.chainsaw;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.plugins.Receiver;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "ChainsawAppender", category = "Core", elementType = "appender", printObject = true)
public final class ChainsawAppender extends AbstractOutputStreamAppender {

    private ChainsawAppenderReceiver m_receiver = new ChainsawAppenderReceiver();

    private ChainsawAppender(String name) {
        super(name, null, null, true, true, null);
    }

    @PluginFactory
    public static ChainsawAppender createAppender(@PluginAttribute("name") String name) {
        System.out.println( "create the chainsaw appender" );

        if (name == null) {
            LOGGER.error("No name provided for ChainsawAppender");
            return null;
        }
        
        return new ChainsawAppender(name);
    }

    @Override
    public void append(final LogEvent event){
        ChainsawLoggingEventBuilder builder = new ChainsawLoggingEventBuilder();

        builder.setLevelFromString( event.getLevel().name() )
                .setLogger( event.getLoggerName() )
                .setMessage( event.getMessage().getFormattedMessage() )
                .setThreadName( event.getThreadName() )
                .setTimestamp( Instant.ofEpochMilli(event.getInstant().getEpochMillisecond()) );

        m_receiver.append(builder.create());
    }

    public Receiver getReceiver(){
        return m_receiver;
    }

    class ChainsawAppenderReceiver extends Receiver {
        public void shutdown(){}
        public void activateOptions(){}
    }
}
