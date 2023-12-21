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
package org.apache.log4j.chainsaw.prefs;

import java.util.*;

/**
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class LoadSettingsEvent extends SettingsEvent {

    LoadSettingsEvent(Object source, Properties properties) {
        super(source);
        this.properties = properties;
    }

    public String getSetting(String key) {
        return properties.getProperty(key);
    }

    public boolean asBoolean(String key) {
        return Boolean.valueOf(getSetting(key));
    }

    public int asInt(String key) {
        String val = getSetting(key);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new RuntimeException("An error occurred retrieving the Integer value of the setting '" + key + "'");
        }
    }

    private final Properties properties;

    /**
     * Returns an unmodifiable Collection of values whose
     * setting key begins (String.startsWith()) the specified
     * string.
     *
     * @param string
     */
    public Collection getSettingsStartingWith(String string) {
        Collection c = new ArrayList();

        for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
            Map.Entry<Object, Object> entry = objectObjectEntry;
            if (entry.getKey().toString().startsWith(string)) {
                c.add(entry.getValue());
            }
        }

        return Collections.unmodifiableCollection(c);
    }
}
