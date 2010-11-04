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
package org.apache.log4j.chainsaw.receivers;


import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.plugins.PluginClassLoaderFactory;
import org.apache.log4j.plugins.Plugin;
import org.apache.log4j.plugins.PluginRegistry;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggerRepositoryEx;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Helper class to assisit with all the known Receivers.
 * 
 * A local resource 'known.receivers' is read in on initialization
 * with each line representing the FQN of the Class that is a recognised Receiver.
 * 
 * @author Paul Smith <psmith@apache.org>
 *
 */
public class ReceiversHelper {

    private static final ReceiversHelper instance = new ReceiversHelper();

    private final Logger logger = LogManager.getLogger(ReceiversHelper.class);
    private List receiverClassList = new ArrayList();
    /**
     *
     */
    private ReceiversHelper() {

        URL url = this.getClass().getClassLoader().getResource(
            this.getClass().getPackage().getName().replace('.','/') + "/known.receivers");
        if (url == null) {
            logger.warn("Failed to locate known.receivers file");
            return;
        }
        LineNumberReader stream = null;
        try {

            stream = new LineNumberReader(new InputStreamReader(url.openStream()));
            String line;
            // we need the special Classloader, because under Web start, optional jars might be local
            // to this workstation
            ClassLoader classLoader = PluginClassLoaderFactory.getInstance().getClassLoader();

            while ((line = stream.readLine()) != null) {
            	
            	try {
            		if (line.startsWith("#") || (line.length() == 0)) {
            			continue;
            		}
            		Class receiverClass = classLoader.loadClass(line);
            		receiverClassList.add(receiverClass);
            		logger.debug("Located known Receiver class " + receiverClass.getName());
            	} catch (ClassNotFoundException e) {
            		logger.warn("Failed to locate Receiver class:" + line);
            	}
            	catch (NoClassDefFoundError e) {
            		logger.error("Failed to locate Receiver class:" + line + ", looks like a dependent class is missing from the classpath", e);
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }


    public static ReceiversHelper getInstance() {
        return instance;
    }


    /**
     * Returns an unmodifiable list of Class objects which represent all the 'known'
     * Receiver classes.
     * @return known receiver classes
     */
    public List getKnownReceiverClasses() {
      return Collections.unmodifiableList(receiverClassList);
    }


  public void saveReceiverConfiguration(File file) {
    LoggerRepository repo = LogManager.getLoggerRepository();
    PluginRegistry pluginRegistry = ((LoggerRepositoryEx) repo).getPluginRegistry();
    List fullPluginList = pluginRegistry.getPlugins();
    List pluginList = new ArrayList();
    for (Iterator iter = fullPluginList.iterator();iter.hasNext();) {
        Plugin thisPlugin = (Plugin)iter.next();
        if (thisPlugin instanceof Receiver) {
            pluginList.add(thisPlugin);
        }
    }
    //remove everything that isn't a receiver..otherwise, we'd create an empty config file
    try {
        if (pluginList.size() > 0) {
            //we programmatically register the ZeroConf plugin in the plugin registry
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element rootElement = document.createElementNS("http://jakarta.apache.org/log4j/", "configuration");
            rootElement.setPrefix("log4j");
            rootElement.setAttribute("xmlns:log4j", "http://jakarta.apache.org/log4j/");
            rootElement.setAttribute("debug", "true");

            for (int i = 0; i < pluginList.size(); i++) {
                Receiver receiver;

                if (pluginList.get(i) instanceof Receiver) {
                    receiver = (Receiver) pluginList.get(i);
                } else {
                    continue;
                }

                Element pluginElement = document.createElement("plugin");
                pluginElement.setAttribute("name", receiver.getName());
                pluginElement.setAttribute("class", receiver.getClass().getName());

                BeanInfo beanInfo = Introspector.getBeanInfo(receiver.getClass());
                List list = new ArrayList(Arrays.asList(beanInfo.getPropertyDescriptors()));

                for (int j = 0; j < list.size(); j++) {
                    PropertyDescriptor d = (PropertyDescriptor) list.get(j);
                    //don't serialize the loggerRepository property for subclasses of componentbase..
                    //easier to change this than tweak componentbase right now..
                    if (d.getReadMethod().getName().equals("getLoggerRepository")) {
                        continue;
                    }
                    Object o = d.getReadMethod().invoke(receiver, new Object[] {} );
                    if (o != null) {
                        Element paramElement = document.createElement("param");
                        paramElement.setAttribute("name", d.getName());
                        paramElement.setAttribute("value", o.toString());
                        pluginElement.appendChild(paramElement);
                    }
                }

                rootElement.appendChild(pluginElement);

            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(rootElement);
            FileOutputStream stream = new FileOutputStream(file);
            StreamResult result = new StreamResult(stream);
            transformer.transform(source, result);
            stream.close();
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
  }
}
