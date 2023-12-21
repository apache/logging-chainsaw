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

import java.util.EventListener;

/**
 * Implementations are notified when new Logger names
 * are added to the related LoggerNameModel instance.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public interface LoggerNameListener extends EventListener {
    /**
     * Called by LoggerNameModel instances when a new unique
     * Logger name string has been introduced into the model
     *
     * @param loggerName the new, unique loggerName
     */
    void loggerNameAdded(String loggerName);

    /**
     * Logger names were reset (used to clear the tree model)
     */
    void reset();
}
