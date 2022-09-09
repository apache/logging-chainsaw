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

import javax.swing.event.EventListenerList;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * SettingManager allows components to register interest in Saving/Loading
 * of general application preferences/settings.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public final class SettingsManager {
    private static final Logger logger = LogManager.getLogger();
    private static final SettingsManager instance = new SettingsManager();
    private static final String GLOBAL_SETTINGS_FILE_NAME = "chainsaw.settings.properties";

    private class TabSettingsData{
        FileBasedConfigurationBuilder<PropertiesConfiguration> file;
        AbstractConfiguration tabSettings;
    }

    private PropertiesConfiguration m_configuration;
    private FileBasedConfigurationBuilder<PropertiesConfiguration> m_builder;
    private Map<String,TabSettingsData> m_tabSettings;

    /**
     * Initialises the SettingsManager by loading the default Properties from
     * a resource
     */
    private SettingsManager() {
        m_tabSettings = new HashMap<>();
        Parameters params = new Parameters();
        File f = new File(getSettingsDirectory(), GLOBAL_SETTINGS_FILE_NAME);

        File settingsDir = getSettingsDirectory();

        if (!settingsDir.exists()) {
            settingsDir.mkdir();
        }

        FileBasedBuilderParameters fileParams = params.fileBased();
        if( f.exists() ){
            fileParams.setFile(f);
        }else{
            URL defaultPrefs = this.getClass().getClassLoader()
                .getResource("org/apache/log4j/chainsaw/prefs/default.properties");
            fileParams.setURL(defaultPrefs);
        }

        m_builder =
            new FileBasedConfigurationBuilder<PropertiesConfiguration>(
                PropertiesConfiguration.class)
                .configure(fileParams
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                );

        try{
            PropertiesConfiguration config = m_builder.getConfiguration();
            m_configuration = config;
            m_builder.getFileHandler().setFile(f);
            return;
        }catch( ConfigurationException ex ){
        }

        // If we get here, it is likely that we have not opened the file.
        // Force a save to create the file
//        try{
//            m_builder.save();
//            PropertiesConfiguration config = m_builder.getConfiguration();
//            m_configuration = config;
//            return;
//        }catch( ConfigurationException ex ){
//            ex.printStackTrace();
//        }
    }

    /**
     * Returns the singleton instance of the SettingsManager
     *
     * @return settings manager
     */
    public static SettingsManager getInstance() {
        return instance;
    }

    public AbstractConfiguration getGlobalConfiguration(){
        return m_configuration;
    }

    public AbstractConfiguration getCombinedSettingsForRecevierTab(String identifier){
        // Override combiner: nodes in the first structure take precedence over the second
        CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

        combinedConfig.addConfiguration(getSettingsForReceiverTab(identifier));
        combinedConfig.addConfiguration(getGlobalConfiguration());

        return combinedConfig;
    }

    public AbstractConfiguration getSettingsForReceiverTab(String identifier){
        if( m_tabSettings.containsKey( identifier ) ){
            return m_tabSettings.get( identifier ).tabSettings;
        }
        
        PropertiesConfiguration configuration = null;

        // Either we don't contain the key, or we got an exception.  Regardless,
        // create a new configuration that we can use
        FileBasedBuilderParameters params = new Parameters().fileBased();
        File f = new File(getSettingsDirectory(), identifier + "-receiver.properties");
        if( !f.exists() ){
            URL defaultPrefs = this.getClass().getClassLoader()
                .getResource("org/apache/log4j/chainsaw/prefs/logpanel.properties");
            params.setURL(defaultPrefs);
        }else{
            params.setFile( f );
        }

        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<PropertiesConfiguration>(
                PropertiesConfiguration.class)
                .configure(params
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                );

        TabSettingsData data = new TabSettingsData();
        data.file = builder;

        try{
            AbstractConfiguration config = builder.getConfiguration();

            builder.getFileHandler().setFile(f);
            data.tabSettings = config;

            m_tabSettings.put( identifier, data );

            return config;
        }catch( ConfigurationException ex ){}
        
        return null;
    }

    public File getSettingsDirectory() {
        return new File(System.getProperty("user.home"), ".chainsaw");
    }

    public void saveGlobalSettings(){
        try{
            m_builder.save();
        }catch( ConfigurationException ex ){
            logger.error( "Unable to save global settings: {}", ex );
        }
    }

    public void saveAllSettings(){
        try{
            m_builder.save();
        }catch( ConfigurationException ex ){
            logger.error( "Unable to save global settings: {}", ex );
        }

        for( String key : m_tabSettings.keySet() ){
            try{
                logger.debug( "Saving {}({}) to {}", key,
                        m_tabSettings.get(key).tabSettings,
                        m_tabSettings.get(key).file.getFileHandler().getURL() );
                m_tabSettings.get(key).file.save();
            }catch( ConfigurationException ex ){
                logger.error( "Unable to save settings for {}", key );
            }
        }
    }
}
