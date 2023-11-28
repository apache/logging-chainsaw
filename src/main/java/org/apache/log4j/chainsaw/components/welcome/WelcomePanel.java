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

package org.apache.log4j.chainsaw.components.welcome;

import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.JTextComponentFormatter;
import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Stack;


/**
 * An initial Welcome Panel that is used when Chainsaw starts up, can displays
 * a HTML pages based on URLs.
 *
 * @author Paul Smith
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class WelcomePanel extends JPanel {
    private Logger logger = LogManager.getLogger(WelcomePanel.class);

    private Stack<URL> urlStack = new Stack<>();
    private final JEditorPane textInfo = new JEditorPane();
    private final URLToolbar urlToolbar = new URLToolbar();

    public WelcomePanel() {
        super(new BorderLayout());
        setBackground(Color.white);
        add(urlToolbar, BorderLayout.NORTH);

        URL helpURL = ChainsawConstants.WELCOME_URL;

        if (helpURL != null) {
            textInfo.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

            JScrollPane pane = new JScrollPane(textInfo);
            pane.setBorder(null);
            add(pane, BorderLayout.CENTER);

            try {
                textInfo.setEditable(false);
                textInfo.setPreferredSize(new Dimension(320, 240));
                textInfo.setPage(helpURL);
                JTextComponentFormatter.applySystemFontAndSize(textInfo);
                textInfo.addHyperlinkListener(
                    e -> {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            urlStack.add(textInfo.getPage());

                            try {
                                textInfo.setPage(e.getURL());
                                urlToolbar.updateToolbar();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
            } catch (Exception e) {
                logger.error(e,e);
            }
        }
    }

    public void setURL(final URL url) {
        SwingUtilities.invokeLater(
            () -> {
                try {
                    urlStack.push(textInfo.getPage());
                    textInfo.setPage(url);
                    //not all pages displayed in the Welcome Panel are html-based (example receiver config is an xml file)..
                    JTextComponentFormatter.applySystemFontAndSize(textInfo);
                    urlToolbar.updateToolbar();
                } catch (IOException e) {
                    logger.error(e,e);
                }
            });
    }

    private class URLToolbar extends JToolBar {
        JButton previousButton;

        private URLToolbar() {
            setFloatable(false);

            JButton home = new SmallButton.Builder()
                .iconUrl(ChainsawIcons.ICON_HOME)
                .action(() -> {
                    setURL(ChainsawConstants.WELCOME_URL);
                    urlStack.clear();
                })
                .shortDescription("Home").build();

            add(home);

            addSeparator();

            previousButton = new SmallButton.Builder()
                .iconUrl(ChainsawIcons.ICON_BACK)
                .shortDescription("Back")
                .disabled()
                .action(() -> {
                    if (urlStack.isEmpty()) {
                        return;
                    }

                    setURL(urlStack.pop());
                }).build();

            add(previousButton);

            addSeparator();
            updateToolbar();
        }

        void updateToolbar() {
            previousButton.setEnabled(!urlStack.isEmpty());
        }
    }

    /**
     * @return tooolbar
     */
    public JToolBar getToolbar() {
        return urlToolbar;
    }
}
