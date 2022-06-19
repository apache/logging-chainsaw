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

import java.util.Properties;

/**
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ProfileManager {

    private static final ProfileManager instance = new ProfileManager();

    public static final ProfileManager getInstance() {
        return instance;
    }

    public void configure(Profileable p) {
//        Properties props = new Properties(SettingsManager.getInstance().getDefaultSettings());
//        LoadSettingsEvent event = new LoadSettingsEvent(this, props);
//        p.loadSettings(event);
    }

    public void configure(Profileable p, String profileName) {
        throw new UnsupportedOperationException("Not implemented as yet");
    }

    private ProfileManager() {


    }
}
