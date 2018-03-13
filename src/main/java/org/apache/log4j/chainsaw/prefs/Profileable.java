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
package org.apache.log4j.chainsaw.prefs;

/**
 * A component implementing this interface is interested in being able to
 * configure itself.
 * <p>
 * Since this interface extends SettingsListener, the component
 * will receive Load and Save settings events as described
 * in SettingsManager
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @see org.apache.log4j.chainsaw.prefs.SettingsManager
 */
public interface Profileable extends SettingsListener {

    /**
     * Must be able to provide a name which is used to determine at a minimum,
     * the default profile name prefix for this component.
     */
    String getNamespace();


}
