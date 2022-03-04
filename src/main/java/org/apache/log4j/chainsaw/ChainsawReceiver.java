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
import org.apache.log4j.chainsaw.logevents.Level;

/**
 * A receiver receives log events from a source.  A ChainsawReceiver will create
 * from 1...N ChainsawReceiverNodes
 */
public interface ChainsawReceiver {
    
    public void addChainsawEventBatchListener( ChainsawEventBatchListener listen );
    
    public void removeEventBatchListener( ChainsawEventBatchListener listen );
    
    public void setThreshold(final Level level);
    
    public Level getThreshold();
    
    public String getName();
    
    public void setName(String name);
    
    public int getQueueInterval();

    public void setQueueInterval(int interval);
    
    public void setPaused(boolean paused);
    
    public boolean getPaused();
    
    /**
     * Start this receiver by(for example) opening a network socket.
     */
    public void start();
    
    /**
     * Stop this receiver by(for example) closing network sockets.
     */
    public void shutdown();
    
    public void addPropertyChangeListener(final PropertyChangeListener listener);
    
    public void addPropertyChangeListener(
        final String propertyName,
        final PropertyChangeListener listener);
    
    public void removePropertyChangeListener(
        final PropertyChangeListener listener);
    
    public void removePropertyChangeListener(
        final String propertyName,
        final PropertyChangeListener listener);
}
