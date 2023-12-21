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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverFactory;
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
    private static final Logger logger = LogManager.getLogger(SettingsManager.class);

    private static final String GLOBAL_SETTINGS_FILE_NAME = "chainsaw.global.properties";

    private static class TabSettingsData {
        FileBasedConfigurationBuilder<PropertiesConfiguration> file;
        AbstractConfiguration tabSettings;
    }

    private PropertiesConfiguration propertiesConfiguration;
    private FileBasedConfigurationBuilder<PropertiesConfiguration> builder;
    private Map<String, TabSettingsData> tabSettings;

    private final Map<Class, PropertyDescriptor[]> classToProperties = new HashMap<>();
    private final Map<Class, String> classToName = new HashMap<>();

    /**
     * Initialises the SettingsManager by loading the default Properties from
     * a resource
     */
    public SettingsManager() {
        tabSettings = new HashMap<>();
        Parameters params = new Parameters();
        File f = new File(getSettingsDirectory(), GLOBAL_SETTINGS_FILE_NAME);

        File settingsDir = getSettingsDirectory();

        if (!settingsDir.exists()) {
            settingsDir.mkdir();
        }

        FileBasedBuilderParameters fileParams = params.fileBased();
        if (f.exists()) {
            fileParams.setFile(f);
        } else {
            URL defaultPrefs =
                    this.getClass().getClassLoader().getResource("org/apache/log4j/chainsaw/prefs/default.properties");
            fileParams.setURL(defaultPrefs);
        }

        builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(fileParams.setListDelimiterHandler(new DefaultListDelimiterHandler(',')));

        try {
            PropertiesConfiguration config = builder.getConfiguration();
            propertiesConfiguration = config;
            builder.getFileHandler().setFile(f);
        } catch (ConfigurationException ex) {
            logger.error(ex);
        }

        ServiceLoader<ChainsawReceiverFactory> sl = ServiceLoader.load(ChainsawReceiverFactory.class);

        for (ChainsawReceiverFactory crFactory : sl) {
            ChainsawReceiver rx = crFactory.create();
            try {
                classToProperties.put(rx.getClass(), crFactory.getPropertyDescriptors());
                classToName.put(rx.getClass(), crFactory.getReceiverName());
            } catch (IntrospectionException ex) {
                logger.error(ex);
            }
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

    public AbstractConfiguration getGlobalConfiguration() {
        return propertiesConfiguration;
    }

    public CombinedConfiguration getCombinedSettingsForRecevierTab(String identifier) {
        // Override combiner: nodes in the first structure take precedence over the second
        CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

        combinedConfig.addConfiguration(getSettingsForReceiverTab(identifier));
        combinedConfig.addConfiguration(getGlobalConfiguration());

        return combinedConfig;
    }

    @SuppressFBWarnings // TODO: loading files like this is dangerous - at least in web. see if we can do better
    public AbstractConfiguration getSettingsForReceiverTab(String identifier) {
        if (tabSettings.containsKey(identifier)) {
            return tabSettings.get(identifier).tabSettings;
        }

        // Either we don't contain the key, or we got an exception.  Regardless,
        // create a new configuration that we can use
        FileBasedBuilderParameters params = new Parameters().fileBased();
        File f = new File(getSettingsDirectory(), identifier + "-receiver.properties");

        if (!f.exists()) {
            URL defaultPrefs =
                    this.getClass().getClassLoader().getResource("org/apache/log4j/chainsaw/prefs/logpanel.properties");
            params.setURL(defaultPrefs);
        } else {
            params.setFile(f);
        }

        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(
                        PropertiesConfiguration.class)
                .configure(params.setListDelimiterHandler(new DefaultListDelimiterHandler(',')));

        TabSettingsData data = new TabSettingsData();
        data.file = builder;

        try {
            AbstractConfiguration config = builder.getConfiguration();

            builder.getFileHandler().setFile(f);
            data.tabSettings = config;

            tabSettings.put(identifier, data);

            return config;
        } catch (ConfigurationException ex) {
            logger.error(ex);
        }

        return null;
    }

    public static File getSettingsDirectory() {
        return new File(System.getProperty("user.home"), ".chainsaw");
    }

    public void saveGlobalSettings() {
        try {
            builder.save();
        } catch (ConfigurationException ex) {
            logger.error("Unable to save global settings: {}", ex);
        }
    }

    public void saveAllSettings() {
        logger.info("Saving all settings");
        try {
            builder.save();
        } catch (ConfigurationException ex) {
            logger.error("Unable to save global settings: {}", ex);
        }

        for (String key : tabSettings.keySet()) {
            try {
                logger.debug(
                        "Saving {}({}) to {}",
                        key,
                        tabSettings.get(key).tabSettings,
                        tabSettings.get(key).file.getFileHandler().getURL());
                tabSettings.get(key).file.save();
            } catch (ConfigurationException ex) {
                logger.error("Unable to save settings for {}", key);
            }
        }
    }

    public void saveSettingsForReceiver(ChainsawReceiver rx) {
        PropertyDescriptor[] desc = classToProperties.get(rx.getClass());

        if (desc == null) {
            return;
        }

        AbstractConfiguration config = getSettingsForReceiverTab(rx.getName());

        config.setProperty("receiver.type", classToName.get(rx.getClass()));

        for (PropertyDescriptor d : desc) {
            Method readMethod = d.getReadMethod();

            try {
                config.setProperty("receiver." + d.getDisplayName(), readMethod.invoke(rx));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                logger.error(ex);
            }
        }
    }

    public void loadSettingsForReceiver(ChainsawReceiver rx) {
        PropertyDescriptor[] desc = classToProperties.get(rx.getClass());

        if (desc == null) {
            return;
        }

        AbstractConfiguration config = getSettingsForReceiverTab(rx.getName());

        for (PropertyDescriptor d : desc) {
            Method writeMethod = d.getWriteMethod();

            try {
                writeMethod.invoke(rx, config.get(d.getPropertyType(), "receiver." + d.getDisplayName()));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                logger.error(ex);
            }
        }
    }
}
