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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import org.apache.log4j.chainsaw.prefs.LoadSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SaveSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SettingsListener;
import org.apache.log4j.chainsaw.prefs.SettingsManager;

/**
 * Helper class that helps delegate the work of loading and saving the  values
 * of the ApplicationPreferenceModel, allowing that class to remain a simple
 * bean.
 * 
 * The Model passed to this class' constructor is the instance of the ApplicationPreference
 * that will be saved, and will have properties modified by loading from the
 * 'chainsaw.settings.xml' file in the .chainsaw directory of the user's home directory.
 * 
 * @author psmith
 *
 */
public class ApplicationPreferenceModelSaver implements SettingsListener {

    private final ApplicationPreferenceModel model;
    
    /**
     * @param model
     */
    public ApplicationPreferenceModelSaver(final ApplicationPreferenceModel model) {
        this.model = model;
    }

    public void loadSettings(LoadSettingsEvent event) {
        XStream stream = new XStream(new DomDriver());
        File file = getApplicationPreferenceXMLFile(SettingsManager.getInstance().getSettingsDirectory());
        try {
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                ApplicationPreferenceModel loadedModel = (ApplicationPreferenceModel) stream
                        .fromXML(reader);
                model.apply(loadedModel);
                reader.close();
            }
        } catch (Exception e) {
//            TODO exception handling
            e.printStackTrace();
            //unable to process - delete file 
            file.delete();
        }
    }

    public void saveSettings(SaveSettingsEvent event) {
        XStream stream = new XStream(new DomDriver());
        try {
            File file = getApplicationPreferenceXMLFile(event.getSettingsLocation());
            FileWriter writer = new FileWriter(file);
            stream.toXML(model, writer);
            writer.close();
        } catch (Exception e) {
//            TODO exception handling
            e.printStackTrace();
        }

    }

    private File getApplicationPreferenceXMLFile(File settingsLocation) {
        return new File(settingsLocation, "chainsaw.settings.xml");
    }

}
