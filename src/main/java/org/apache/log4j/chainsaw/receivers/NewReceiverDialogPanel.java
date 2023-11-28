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

import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverFactory;
import org.apache.log4j.chainsaw.helper.OkCancelPanel;

import javax.swing.*;
import java.awt.*;
import java.beans.IntrospectionException;


/**
 * A panel that allows a user to configure a new Plugin, and
 * view that plugins javadoc at the same time
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class NewReceiverDialogPanel extends JPanel {
    private final PluginPropertyEditorPanel pluginEditorPanel = new PluginPropertyEditorPanel();
    private final OkCancelPanel okPanel = new OkCancelPanel();
    private final JEditorPane javaDocPane = new JEditorPane();
    private final JScrollPane javaDocScroller = new JScrollPane(javaDocPane);
    private final JSplitPane splitter = new JSplitPane();

    private NewReceiverDialogPanel() {
        setupComponents();
        javaDocPane.setContentType("text/html");
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
     * @return NewReceiverDialogPanel
     * @throws IllegalArgumentException if the specified class is not a Receiver
     */
    public static NewReceiverDialogPanel create(ChainsawReceiverFactory recvFact) throws IntrospectionException {

        ChainsawReceiver recv = recvFact.create();

        NewReceiverDialogPanel panel = new NewReceiverDialogPanel();

        panel.pluginEditorPanel.setReceiverAndProperties(recv, recvFact.getPropertyDescriptors());

        panel.javaDocPane.setText(recvFact.getReceiverDocumentation());

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
