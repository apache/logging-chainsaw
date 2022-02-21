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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.Level;

/**
 *
 */
public abstract class ChainsawReceiverSkeleton implements ChainsawReceiver {
    
    /**
     * Name of this plugin.
     */
    protected String name = "Receiver";


    /**
     * This is a delegate that does all the PropertyChangeListener
     * support.
     */
    private PropertyChangeSupport propertySupport =
        new PropertyChangeSupport(this);
    
    /**
     * Threshold level.
     */
    protected Level thresholdLevel = Level.TRACE;
    
    private List<ChainsawEventBatchListener> m_eventListeners;
    private WorkQueue m_worker;
    private final Object mutex = new Object();
    private int m_sleepInterval = 1000;
    private boolean m_paused = false;
    
    public ChainsawReceiverSkeleton(){
        m_eventListeners = new ArrayList<>();
        m_worker = new WorkQueue();
    }

    @Override
    public void addChainsawEventBatchListener(ChainsawEventBatchListener listen) {
        if( listen != null ){
            m_eventListeners.add( listen );
        }
    }

    @Override
    public void removeEventBatchListener(ChainsawEventBatchListener listen) {
        if( listen != null ){
            m_eventListeners.remove( listen );
        }
    }

    @Override
    public void setThreshold(Level level) {
        Level oldValue = this.thresholdLevel;
        thresholdLevel = level;
        propertySupport.firePropertyChange("threshold", oldValue, this.thresholdLevel);
    }

    @Override
    public Level getThreshold() {
        return thresholdLevel;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        if( this.name.equals(name) ){
            return;
        }

        String oldName = this.name;
        this.name = name;
        propertySupport.firePropertyChange("name", oldName, name);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }
    
    @Override
    public int getQueueInterval() {
        return m_sleepInterval;
    }

    @Override
    public void setQueueInterval(int interval) {
        m_sleepInterval = interval;
    }
    
    @Override
    public void setPaused(boolean paused){
        m_paused = paused;
    }
    
    @Override
    public boolean getPaused(){
        return m_paused;
    }
    
    /**
     * Whenever a new log event comes in, create a ChainsawLoggingEvent and call
     * this method.  If this receiver is paused, discard the event.
     * 
     * @param event 
     */
    protected void append(final ChainsawLoggingEvent event){
        if( m_paused ) return;
        m_worker.enqueue(event);
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
