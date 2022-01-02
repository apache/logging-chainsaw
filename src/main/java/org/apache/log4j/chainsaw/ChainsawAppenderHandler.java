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

import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.LoggingEventFieldResolver;

import javax.swing.event.EventListenerList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

/**
 * A handler class that either extends a particular appender hierarchy or can be
 * bound into the Log4j appender framework, and queues events, to be later
 * dispatched to registered/interested parties.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ChainsawAppenderHandler {
    private static final String DEFAULT_IDENTIFIER = "Unknown";
    private final Object mutex = new Object();
    private int sleepInterval = 1000;
    private EventListenerList listenerList = new EventListenerList();
    private double dataRate = 0.0;
    private String identifierExpression;
    private final LoggingEventFieldResolver resolver = LoggingEventFieldResolver
        .getInstance();
    private PropertyChangeSupport propertySupport = new PropertyChangeSupport(
        this);
    private Map<String, Rule> customExpressionRules = new HashMap<>();

    /**
     * NOTE: This variable needs to be physically located LAST, because
     * of the initialization sequence, the WorkQueue constructor starts a thread
     * which ends up needing some reference to fields created in ChainsawAppenderHandler (outer instance)
     * which may not have been created yet.  Becomes a race condition, and therefore
     * this field initialization should be kept last.
     */
    private WorkQueue worker = new WorkQueue();

    public ChainsawAppenderHandler() {
    }

    public void setIdentifierExpression(String identifierExpression) {
        synchronized (mutex) {
            this.identifierExpression = identifierExpression;
            mutex.notify();
        }
    }

    public String getIdentifierExpression() {
        return identifierExpression;
    }

    public void addCustomEventBatchListener(String identifier,
                                            EventBatchListener l) throws IllegalArgumentException {
        customExpressionRules.put(identifier, ExpressionRule.getRule(identifier));
        listenerList.add(EventBatchListener.class, l);
    }

    public void addEventBatchListener(EventBatchListener l) {
        listenerList.add(EventBatchListener.class, l);
    }

    public void removeEventBatchListener(EventBatchListener l) {
        listenerList.remove(EventBatchListener.class, l);
    }

    public void append(LoggingEvent event) {
        worker.enqueue(event);
    }

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

    public int getQueueInterval() {
        return sleepInterval;
    }

    public void setQueueInterval(int interval) {
        sleepInterval = interval;
    }

    /**
     * Determines an appropriate title for the Tab for the Tab Pane by locating a
     * the hostname property
     *
     * @param e
     * @return identifier
     */
    String getTabIdentifier(LoggingEvent e) {
        return "";
//        String ident = resolver.applyFields(identifierExpression, e);
//        return ((ident != null) ? ident : DEFAULT_IDENTIFIER);
    }

    /**
     * Exposes the current Data rate calculated. This is periodically updated by
     * an internal Thread as is the number of events that have been processed, and
     * dispatched to all listeners since the last sample period divided by the
     * number of seconds since the last sample period.
     * <p>
     * This method fires a PropertyChange event so listeners can monitor the rate
     *
     * @return double # of events processed per second
     */
    public double getDataRate() {
        return dataRate;
    }

    /**
     * @param dataRate
     */
    void setDataRate(double dataRate) {
        double oldValue = this.dataRate;
        this.dataRate = dataRate;
        propertySupport.firePropertyChange("dataRate", oldValue,
            this.dataRate);
    }

    /**
     * @param listener
     */
    public synchronized void addPropertyChangeListener(
        PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    /**
     * @param propertyName
     * @param listener
     */
    public synchronized void addPropertyChangeListener(String propertyName,
                                                       PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @param listener
     */
    public synchronized void removePropertyChangeListener(
        PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    /**
     * @param propertyName
     * @param listener
     */
    public synchronized void removePropertyChangeListener(String propertyName,
                                                          PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Queue of Events are placed in here, which are picked up by an asychronous
     * thread. The WorkerThread looks for events once a second and processes all
     * events accumulated during that time..
     */
    class WorkQueue {
        final ArrayList<LoggingEvent> queue = new ArrayList<>();
        Thread workerThread;

        protected WorkQueue() {
            workerThread = new WorkerThread();
            workerThread.start();
        }

        public final void enqueue(LoggingEvent event) {
            synchronized (mutex) {
                queue.add(event);
                mutex.notify();
            }
        }

        public final void stop() {
            synchronized (mutex) {
                workerThread.interrupt();
            }
        }

        /**
         * The worker thread converts each queued event to a vector and forwards the
         * vector on to the UI.
         */
        private class WorkerThread extends Thread {
            public WorkerThread() {
                super("Chainsaw-WorkerThread");
                setDaemon(true);
                setPriority(Thread.NORM_PRIORITY - 1);
            }

            public void run() {
                List<LoggingEvent> innerList = new ArrayList<>();
                while (true) {
                    long timeStart = System.currentTimeMillis();
                    synchronized (mutex) {
                        try {
                            while ((queue.size() == 0) || (identifierExpression == null)) {
                                setDataRate(0);
                                mutex.wait();
                            }
                            if (queue.size() > 0) {
                                innerList.addAll(queue);
                                queue.clear();
                            }
                        } catch (InterruptedException ie) {
                        }
                    }
                    int size = innerList.size();
                    if (size > 0) {
                        Iterator<LoggingEvent> iter = innerList.iterator();
                        ChainsawEventBatch eventBatch = new ChainsawEventBatch();
                        while (iter.hasNext()) {
                            LoggingEvent e = iter.next();
                            // attempt to set the host name (without port), from
                            // remoteSourceInfo
                            // if 'hostname' property not provided
                            if (e.getProperty(Constants.HOSTNAME_KEY) == null) {
                                String remoteHost = e
                                    .getProperty(ChainsawConstants.LOG4J_REMOTEHOST_KEY);
                                if (remoteHost != null) {
                                    int colonIndex = remoteHost.indexOf(":");
                                    if (colonIndex == -1) {
                                        colonIndex = remoteHost.length();
                                    }
                                    e.setProperty(Constants.HOSTNAME_KEY, remoteHost.substring(0,
                                        colonIndex));
                                }
                            }
                            for (Object o : customExpressionRules.entrySet()) {
//                                Map.Entry entry = (Map.Entry) o;
//                                Rule rule = (Rule) entry.getValue();
//                                if (rule.evaluate(e, null)) {
//                                    eventBatch.addEvent((String) entry.getKey(), e);
//                                }
                            }
                            eventBatch.addEvent(getTabIdentifier(e), e);
                        }
                        dispatchEventBatch(eventBatch);
                        innerList.clear();
                    }
                    if (getQueueInterval() > 1000) {
                        try {
                            synchronized (this) {
                                wait(getQueueInterval());
                            }
                        } catch (InterruptedException ie) {
                        }
                    } else {
                        Thread.yield();
                    }
                    if (size == 0) {
                        setDataRate(0.0);
                    } else {
                        long timeEnd = System.currentTimeMillis();
                        long diffInSeconds = (timeEnd - timeStart) / 1000;
                        double rate = (((double) size) / diffInSeconds);
                        setDataRate(rate);
                    }
                }
            }

            /**
             * Dispatches the event batches contents to all the interested parties by
             * iterating over each identifier and dispatching the
             * ChainsawEventBatchEntry object to each listener that is interested.
             *
             * @param eventBatch
             */
            private void dispatchEventBatch(ChainsawEventBatch eventBatch) {
                EventBatchListener[] listeners = listenerList
                    .getListeners(EventBatchListener.class);
                for (Iterator<String> iter = eventBatch.identifierIterator(); iter.hasNext(); ) {
                    String identifier = iter.next();
                    List<LoggingEvent> eventList = null;
                    for (EventBatchListener listener : listeners) {
                        if ((listener.getInterestedIdentifier() == null)
                            || listener.getInterestedIdentifier().equals(identifier)) {
                            if (eventList == null) {
                                eventList = eventBatch.entrySet(identifier);
                            }
                            listener.receiveEventBatch(identifier, eventList);
                        }
                    }
                }
            }
        }
    }
}
