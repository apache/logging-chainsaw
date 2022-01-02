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

package org.apache.log4j.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.ChainsawEventBatchListener;
import org.apache.log4j.chainsaw.EventBatchListener;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.Thresholdable;


/**
 * Defines the base class for Receiver plugins.
 * <p></p>
 * <p>Just as Appenders send logging events outside of the log4j
 * environment (to files, to smtp, to sockets, etc), Receivers bring
 * logging events inside the log4j environment.
 * <p></p>
 * <p>Receivers are meant to support the receiving of
 * remote logging events from another process.
 * <p></p>
 * <p>Receivers can also be used to "import" log messages from other
 * logging packages into the log4j environment.
 * <p></p>
 * <p>Receivers can be configured to post events to a given
 * LoggerRepository.
 * <p></p>
 * <p>Subclasses of Receiver must implement the isActive(),
 * activateOptions(), and shutdown() methods. The doPost() method
 * is provided to standardize the "import" of remote events into
 * the repository.
 *
 * @author Mark Womack
 * @author Ceki G&uuml;lc&uuml;
 * @author Paul Smith (psmith@apache.org)
 */
public abstract class Receiver extends PluginSkeleton implements Thresholdable {
    /**
     * Threshold level.
     */
    protected Level thresholdLevel;

    private List<ChainsawEventBatchListener> m_eventListeners;
    private WorkQueue m_worker;
    private final Object mutex = new Object();
    private int m_sleepInterval = 1000;

    /**
     * Create new instance.
     */
    protected Receiver() {
        super();
        m_eventListeners = new ArrayList<>();
        m_worker = new WorkQueue();
    }

    /**
     * Sets the receiver theshold to the given level.
     *
     * @param level The threshold level events must equal or be greater
     *              than before further processing can be done.
     */
    public void setThreshold(final Level level) {
        Level oldValue = this.thresholdLevel;
        thresholdLevel = level;
        firePropertyChange("threshold", oldValue, this.thresholdLevel);
    }

    /**
     * Gets the current threshold setting of the receiver.
     *
     * @return Level The current threshold level of the receiver.
     */
    public Level getThreshold() {
        return thresholdLevel;
    }

    /**
     * Returns true if the given level is equals or greater than the current
     * threshold value of the receiver.
     *
     * @param level The level to test against the receiver threshold.
     * @return boolean True if level is equal or greater than the
     * receiver threshold.
     */
    public boolean isAsSevereAsThreshold(final Level level) {
        return ((thresholdLevel == null)
            || level.isGreaterOrEqual(thresholdLevel));
    }

    public void addChainsawEventBatchListener( ChainsawEventBatchListener listen ){
        if( listen != null ){
            m_eventListeners.add( listen );
        }
    }

    public void removeEventBatchListener( ChainsawEventBatchListener listen ){
        if( listen != null ){
            m_eventListeners.remove( listen );
        }
    }

    public void append(final ChainsawLoggingEvent event){
        m_worker.enqueue(event);
    }

    /**
     * Posts the logging event to a logger in the configured logger
     * repository.
     *
     * @param event the log event to post to the local log4j environment.
     */
    public void doPost(final LoggingEvent event) {
        // if event does not meet threshold, exit now
        if (!isAsSevereAsThreshold(event.getLevel())) {
            return;
        }

        // get the "local" logger for this event from the
        // configured repository.
        Logger localLogger =
            getLoggerRepository().getLogger(event.getLoggerName());

        // if the logger level is greater or equal to the level
        // of the event, use the logger to append the event.
        if (event.getLevel()
            .isGreaterOrEqual(localLogger.getEffectiveLevel())) {
            // call the loggers appenders to process the event
            localLogger.callAppenders(event);
        }
    }

    public int getQueueInterval() {
        return m_sleepInterval;
    }

    public void setQueueInterval(int interval) {
        m_sleepInterval = interval;
    }

    /**
     * Queue of Events are placed in here, which are picked up by an asychronous
     * thread. The WorkerThread looks for events once a second and processes all
     * events accumulated during that time..
     */
    class WorkQueue {
        final ArrayList<ChainsawLoggingEvent> queue = new ArrayList<>();
        Thread workerThread;

        protected WorkQueue() {
            workerThread = new WorkerThread();
            workerThread.start();
        }

        public final void enqueue(ChainsawLoggingEvent event) {
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
                while (true) {
                    List<ChainsawLoggingEvent> innerList = new ArrayList<>();
                    synchronized (mutex) {
                        try {
                            while ((queue.size() == 0)) {
//                                setDataRate(0);
                                mutex.wait();
                            }
                            if (queue.size() > 0) {
                                innerList.addAll(queue);
                                queue.clear();
                            }
                        } catch (InterruptedException ie) {
                        }
                    }

                    System.out.println( "Sending " + innerList.size() + " events to " + m_eventListeners.size() + " rx" );
                    for( ChainsawEventBatchListener evtListner : m_eventListeners ){
                        evtListner.receiveChainsawEventBatch(innerList);
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
//                    if (size == 0) {
//                        setDataRate(0.0);
//                    } else {
//                        long timeEnd = System.currentTimeMillis();
//                        long diffInSeconds = (timeEnd - timeStart) / 1000;
//                        double rate = (((double) size) / diffInSeconds);
//                        setDataRate(rate);
//                    }
                }
            }
        }
    }
}
