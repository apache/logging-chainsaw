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
package org.apache.log4j.chainsaw.filter;

import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.LocationInfo;

/**
 * This class is used as a Model for Filtering, and retains the unique entries that
 * come through over a set of LoggingEvents
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class FilterModel {
    private EventTypeEntryContainer eventContainer = new EventTypeEntryContainer();

    public void processNewLoggingEvent(ChainsawLoggingEvent event) {

        eventContainer.addLevel(event.m_level);
        eventContainer.addLogger(event.m_logger);
        eventContainer.addThread(event.m_threadName);
        eventContainer.addNDC(event.m_ndc);
        eventContainer.addProperties(event.getProperties());

        LocationInfo info = event.m_locationInfo;
        if (info != null) {
            eventContainer.addClass(info.className);
            eventContainer.addMethod(info.methodName);
            eventContainer.addFileName(info.fileName);
        }
    }

    public EventTypeEntryContainer getContainer() {
        return eventContainer;
    }
}
