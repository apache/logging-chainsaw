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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Loads/Saves the MRU lists from preferences
 * 
 * @author psmith
 *
 */
public class MRUFileListPreferenceSaver implements SettingsListener{

    private static final MRUFileListPreferenceSaver instance = new MRUFileListPreferenceSaver();
    
    public static final MRUFileListPreferenceSaver getInstance() {
        return instance;
    }
    private MRUFileListPreferenceSaver() {}
    
    public void loadSettings(LoadSettingsEvent event) {
        File file = getMRULocation(SettingsManager.getInstance().getSettingsDirectory());
        if(file.exists()) {
            try {
                MRUFileList.loadLog4jMRUListFromReader(new FileReader(file));
            } catch (Exception e) {
//                TODO exception handling
                e.printStackTrace();
            }
        }
        
    }

    public void saveSettings(SaveSettingsEvent event) {
        XStream stream = new XStream(new DomDriver());
        try {
            File file = getMRULocation(event.getSettingsLocation());
            System.out.println("Writing MRU ->" + file.getAbsolutePath());
            FileWriter writer = new FileWriter(file);
            stream.toXML(MRUFileList.log4jMRU(), writer);
            writer.close();
        } catch (Exception e) {
//            TODO exception handling
            e.printStackTrace();
        }
    }
    private File getMRULocation(File dir) {
        File file = new File(dir, "mru.xml");
        return file;
    }

}
