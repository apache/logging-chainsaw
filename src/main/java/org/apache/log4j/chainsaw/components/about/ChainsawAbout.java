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
package org.apache.log4j.chainsaw.components.about;

import org.apache.log4j.chainsaw.JTextComponentFormatter;
import org.apache.log4j.chainsaw.help.HelpManager;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.chainsaw.logui.LogUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple About box telling people stuff about this project
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ChainsawAbout extends JDialog {
    private static final Logger LOG = LogManager.getLogger(ChainsawAbout.class);

    private final LogUI logUI;

    public ChainsawAbout(LogUI logUI) {
        super(logUI, "About Chainsaw v2", true);
        this.logUI = logUI;
        setBackground(Color.white);
        getContentPane().setLayout(new BorderLayout());

        JButton closeButton = new JButton(" Close ");
        closeButton.addActionListener(e -> setVisible(false));
        closeButton.setDefaultCapable(true);

        JEditorPane editPane = new JEditorPane("text/html", "");
        try {
            URL url = ClassLoader.getSystemResource("pages/about.html");
            editPane.setPage(url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find the about panel HTML", e);
        }

        JScrollPane scrollPane = new JScrollPane(
            editPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(closeButton, BorderLayout.SOUTH);

        editPane.setEditable(false);
        editPane.addHyperlinkListener(
            e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    HelpManager.getInstance().setHelpURL(e.getURL());
                }
            });

        calculateDimentions();
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        setLocationRelativeTo(logUI);
    }

    /**
     * Calculates the dimensions of the about panel based on the parents component (logUI).
     * The default dimensions are 10% smaller than the parent component, except the height/width
     * are 400 or below. In that case the same dimensions of the parent component are taken.
     */
    public void calculateDimentions() {
        Dimension size = logUI.getSize();
        double height = size.getHeight();
        double width = size.getWidth();
        if (height > 400) {
            height = height - (height * 0.10);
        }
        if (width > 400) {
            width = width - (width * 0.10);
        }
        setSize((int)width, (int)height);

    }
}
