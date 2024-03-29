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
package org.apache.log4j.chainsaw.components.loggernamepanel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.event.EventListenerList;

/**
 * An implementation of LoggerNameModel which can be used as a delegate
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class LoggerNameModelSupport implements LoggerNameModel {
    private final Set<String> loggerNameSet = new HashSet<>();
    private final EventListenerList listenerList = new EventListenerList();

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameModel#getLoggerNames()
     */
    public Collection<String> getLoggerNames() {
        return Collections.unmodifiableSet(loggerNameSet);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameModel#addLoggerName(java.lang.String)
     */
    public boolean addLoggerName(String loggerName) {
        boolean isNew = loggerNameSet.add(loggerName);

        if (isNew) {
            notifyListeners(loggerName);
        }

        return isNew;
    }

    public void reset() {
        loggerNameSet.clear();
        LoggerNameListener[] eventListeners = listenerList.getListeners(LoggerNameListener.class);

        for (LoggerNameListener listener : eventListeners) {
            listener.reset();
        }
    }

    /**
     * Notifies all the registered listeners that a new unique
     * logger name has been added to this model
     *
     * @param loggerName
     */
    private void notifyListeners(String loggerName) {
        LoggerNameListener[] eventListeners = listenerList.getListeners(LoggerNameListener.class);

        for (LoggerNameListener listener : eventListeners) {
            listener.loggerNameAdded(loggerName);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameModel#addLoggerNameListener(org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameListener)
     */
    public void addLoggerNameListener(LoggerNameListener l) {
        listenerList.add(LoggerNameListener.class, l);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameModel#removeLoggerNameListener(org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameListener)
     */
    public void removeLoggerNameListener(LoggerNameListener l) {
        listenerList.remove(LoggerNameListener.class, l);
    }
}
