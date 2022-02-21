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

import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.helper.OkCancelPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.net.URL;
import org.apache.log4j.chainsaw.ChainsawReceiver;
import org.apache.log4j.chainsaw.ChainsawReceiverFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


/**
 * A panel that allows a user to configure a new Plugin, and
 * view that plugins javadoc at the same time
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class NewReceiverDialogPanel extends JPanel {

    private PluginPropertyEditorPanel pluginEditorPanel =
        new PluginPropertyEditorPanel();
    private final OkCancelPanel okPanel = new OkCancelPanel();
    private final JEditorPane javaDocPane = new JEditorPane();
    private final JScrollPane javaDocScroller = new JScrollPane(javaDocPane);
    private final JSplitPane splitter = new JSplitPane();
    private static final Logger logger = LogManager.getLogger();

    private NewReceiverDialogPanel() {
        setupComponents();
        setupListeners();
    }

    /**
     *
     */
    private void setupListeners() {

        /**
         * We listen for the plugin change, and modify the editor panes
         * url to be the Help resource for that class
         */
        pluginEditorPanel.addPropertyChangeListener("plugin",
            evt -> {

//                Plugin plugin = (Plugin) evt.getNewValue();
//                URL url = HelpManager.getInstance().getHelpForClass(
//                    plugin.getClass());
//
//                try {
//                    javaDocPane.setPage(url);
//                } catch (IOException e) {
//                    logger.error(
//                        "Failed to load the Help resource for " +
//                            plugin.getClass(), e);
//                }
            });
    }

    /**
     *
     */
    private void setupComponents() {
        setLayout(new BorderLayout());

        setupJavadoc();

        setupPluginPropertyPanel();

        setupSplitter();

        add(splitter, BorderLayout.CENTER);
        add(okPanel, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(600, 600));
        setPreferredSize(getMinimumSize());
    }

    private void setupPluginPropertyPanel() {
        pluginEditorPanel.setMinimumSize(new Dimension(320, 160));
        pluginEditorPanel.setPreferredSize(pluginEditorPanel.getMinimumSize());
    }

    private void setupSplitter() {
        splitter.setTopComponent(javaDocScroller);
        splitter.setBottomComponent(pluginEditorPanel);
        splitter.setResizeWeight(0.8);
        splitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
    }

    private void setupJavadoc() {
        javaDocPane.setEditable(false);
    }

    /**
     * Creates a new panel, with the contents configured to allow the editing
     * of a NEW instance of the specified class (which must implement the Receiver
     * interface)
     *
     * @param receiverClass
     * @return NewReceiverDialogPanel
     * @throws IllegalArgumentException if the specified class is not a Receiver
     */
    public static NewReceiverDialogPanel create(ChainsawReceiverFactory recvFact) throws IntrospectionException {

        ChainsawReceiver recv = recvFact.create();

        NewReceiverDialogPanel panel = new NewReceiverDialogPanel();

        panel.pluginEditorPanel.setReceiverAndProperties(recv, recvFact.getPropertyDescriptors());

        return panel;
    }

    /**
     * @return Returns the okPanel.
     */
    public final OkCancelPanel getOkPanel() {

        return okPanel;
    }

    /**
     *
     */
    public ChainsawReceiver getReceiver() {

        return this.pluginEditorPanel.getPlugin();
    }

}
