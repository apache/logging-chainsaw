/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author Paul Smith <psmith@apache.org>
 *
 */
package org.apache.log4j.chainsaw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.help.HelpManager;

/**
 * A simple About box telling people stuff about this project
 * 
 * @author Paul Smith <psmith@apache.org>
 * 
 */
class ChainsawAbout extends JDialog {
    private static final Logger LOG = Logger.getLogger(ChainsawAbout.class);

    private final JEditorPane editPane = new JEditorPane("text/html", "");

    private final JScrollPane scrollPane = new JScrollPane(editPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final String url = ChainsawAbout.class.getName().replace('.', '/')
            + ".html";

    private boolean sleep = false;

    private final Object guard = new Object();

    ChainsawAbout(JFrame parent) {
        super(parent, "About Chainsaw v2", true);
        // setResizable(false);
        setBackground(Color.white);
        getContentPane().setLayout(new BorderLayout());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        closeButton.setDefaultCapable(true);

        try {
            editPane.setPage(this.getClass().getClassLoader().getResource(url));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find the About panel HTML", e);
        }
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(closeButton, BorderLayout.SOUTH);

        editPane.setEditable(false);
        editPane.addHyperlinkListener(
                new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                          HelpManager.getInstance().setHelpURL(e.getURL());
                      }
                    }
                  });
        
        setSize(320, 240);
        new Thread(new Scroller()).start();
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        setLocationRelativeTo(parent);
    }

    private class Scroller implements Runnable {

        public void run() {
            while (true) {
                try {
                    if (sleep) {
                        synchronized (guard) {
                            guard.wait();
                        }
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    scrollPane.getViewport().setViewPosition(
                                            new Point(0, 0));
                                }
                            });
                        continue;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            scrollPane.getViewport().setViewPosition(
                                    new Point(0, scrollPane.getViewport()
                                            .getViewPosition().y + 1));
                        }
                    });
                    Thread.sleep(100);
                } catch (Exception e) {
                    LOG.error("Error during scrolling", e);
                }

            }
        }
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        sleep = !visible;
        synchronized (guard) {
            guard.notifyAll();
        }
    }
}
