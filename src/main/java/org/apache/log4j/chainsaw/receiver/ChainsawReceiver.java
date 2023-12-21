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
package org.apache.log4j.chainsaw.receiver;

import java.beans.PropertyChangeListener;
import org.apache.log4j.chainsaw.ChainsawEventBatchListener;
import org.apache.log4j.chainsaw.logevents.Level;

/**
 * A receiver receives log events from a source.
 */
public interface ChainsawReceiver {

    void addChainsawEventBatchListener(ChainsawEventBatchListener listen);

    void removeEventBatchListener(ChainsawEventBatchListener listen);

    void setThreshold(final Level level);

    Level getThreshold();

    String getName();

    void setName(String name);

    int getQueueInterval();

    void setQueueInterval(int interval);

    void setPaused(boolean paused);

    boolean getPaused();

    /**
     * Start this receiver by(for example) opening a network socket.
     */
    void start();

    /**
     * Stop this receiver by(for example) closing network sockets.
     */
    void shutdown();

    void addPropertyChangeListener(final PropertyChangeListener listener);

    void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener);

    void removePropertyChangeListener(final PropertyChangeListener listener);

    void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener);
}
