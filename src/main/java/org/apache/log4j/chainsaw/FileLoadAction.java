///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.log4j.chainsaw;
//
//import org.apache.log4j.Logger;
//import org.apache.log4j.chainsaw.helper.SwingHelper;
//import org.apache.log4j.chainsaw.prefs.MRUFileList;
//import org.apache.log4j.helpers.Constants;
//import org.apache.log4j.spi.Decoder;
//import org.apache.log4j.spi.LoggingEvent;
//
//import javax.swing.*;
//import java.awt.event.ActionEvent;
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Vector;
//
///**
// * Allows the user to specify a particular file to open and import the events
// * into a new tab.
// *
// * @author Paul Smith &lt;psmith@apache.org&gt;
// * @author Scott Deboy &lt;sdeboy@apache.org&gt;
// */
//class FileLoadAction extends AbstractAction {
//    private static final Logger LOG = Logger.getLogger(FileLoadAction.class);
//
//    /**
//     * This action must have a reference to a LogUI window so that it can append
//     * the events it loads
//     */
//    Decoder decoder;
//
//    private LogUI parent;
//
//    private boolean remoteURL;
//
//    public FileLoadAction(LogUI parent, Decoder decoder, String title,
//                          boolean isRemoteURL) {
//        super(title);
//        remoteURL = isRemoteURL;
//        this.decoder = decoder;
//        this.parent = parent;
//    }
//
//    /*
//     * When the user chooses the Load action, a File chooser is presented to
//     * allow them to find an XML file to load events from.
//     *
//     * Any events decoded from this file are added to one of the tabs.
//     *
//     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
//     */
//    public void actionPerformed(ActionEvent e) {
//        String name = "";
//        URL url = null;
//
//        if (!remoteURL) {
//            try {
//                File selectedFile = SwingHelper.promptForFile(parent, null, "Load Events from XML file or zipped XML file...", true);
//                if (selectedFile != null) {
//                    url = selectedFile.toURI().toURL();
//                    name = selectedFile.getName();
//                }
//            } catch (Exception ex) {
//                // TODO: handle exception
//            }
//        } else {
//            String urltext = JOptionPane
//                .showInputDialog(parent,
//                    "<html>Please type in the <b>complete</b> URL to the remote XML source.</html>");
//
//            if (urltext != null) {
//                try {
//                    url = new URL(urltext);
//                } catch (Exception ex) {
//                    JOptionPane.showMessageDialog(parent, "'" + urltext
//                        + "' is not a valid URL.");
//                }
//            }
//        }
//
//        if (url != null) {
////            importURL(parent.handler, decoder, name, url);
//            MRUFileList.log4jMRU().opened(url);
//        }
//    }
//
//    /**
//     * Imports a URL into Chainsaw, by using the Decoder, and
//     * using the name value as the Application key which (usually) determines
//     * the Tab name
//     *
//     * @param name
//     * @param url  URL to import
//     */
//    public static void importURL(final ChainsawAppenderHandler handler, final Decoder decoder, String name, URL url) {
//        Map additionalProperties = new HashMap();
//        additionalProperties.put(Constants.HOSTNAME_KEY, "file");
//        additionalProperties.put(Constants.APPLICATION_KEY, name);
//        decoder.setAdditionalProperties(additionalProperties);
//
//        final URL urlToUse = url;
//        new Thread(() -> {
//            try {
//                Vector events = decoder.decode(urlToUse);
//                for (Object event : events) {
//                    handler.append((LoggingEvent) event);
//                }
//            } catch (IOException e1) {
//                // TODO Handle the error with a nice msg
//                LOG.error(e1);
//            }
//            MRUFileList.log4jMRU().opened(urlToUse);
//        }).start();
//    }
//}
