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

import org.apache.log4j.chainsaw.help.HelpManager;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;

import org.apache.log4j.chainsaw.logui.LogUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple About box telling people stuff about this project
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ChainsawAbout extends JDialog {
    private static final Logger LOG = LogManager.getLogger();

    private final JEditorPane editPane = new JEditorPane("text/html", "");

    private final JScrollPane scrollPane = new JScrollPane(editPane,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private boolean sleep = false;

    private final Object guard = new Object();

    public ChainsawAbout(LogUI logUI) {
        super(logUI, "About Chainsaw v2", true);
        setBackground(Color.white);
        getContentPane().setLayout(new BorderLayout());

        JButton closeButton = new JButton(" Close ");
        closeButton.addActionListener(e -> setVisible(false));
        closeButton.setDefaultCapable(true);

        try {
            String url = ChainsawAbout.class.getName().replace('.', '/') + ".html";
            editPane.setPage(this.getClass().getClassLoader().getResource(url));
        } catch (IOException e) {
            throw new RuntimeException("Failed to find the About panel HTML", e);
        }

        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(closeButton, BorderLayout.SOUTH);

        JTextComponentFormatter.applySystemFontAndSize(editPane);

        editPane.setEditable(false);
        editPane.addHyperlinkListener(
            e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    HelpManager.getInstance().setHelpURL(e.getURL());
                }
            });

        setSize(320, 240);
        new Thread(new Scroller()).start();
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        setLocationRelativeTo(logUI);
    }

    private class Scroller implements Runnable {

        public void run() {
            while (true) {
                try {
                    if (sleep) {
                        synchronized (guard) {
                            guard.wait();
                        }
                        SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(
                            new Point(0, 0)));
                        continue;
                    }
                    SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(
                        new Point(0, scrollPane.getViewport()
                            .getViewPosition().y + 1)));
                    Thread.sleep(100);
                } catch (Exception e) {
                    LOG.error("Error during scrolling", e);
                }

            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        sleep = !visible;
        synchronized (guard) {
            guard.notifyAll();
        }
    }
}
