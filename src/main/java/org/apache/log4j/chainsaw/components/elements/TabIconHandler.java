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
package org.apache.log4j.chainsaw.components.elements;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.log4j.chainsaw.EventCountListener;
import org.apache.log4j.chainsaw.components.tabbedpane.ChainsawTabbedPane;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;

public class TabIconHandler implements EventCountListener, ChangeListener {
    // the tabIconHandler is associated with a new tab, and a new tab always
    // shows the 'new events' icon
    private boolean newEvents = true;
    private boolean seenEvents = false;
    private final String ident;
    private ChainsawTabbedPane tabbedPane;
    ImageIcon NEW_EVENTS = new ImageIcon(ChainsawIcons.ANIM_RADIO_TOWER);
    ImageIcon HAS_EVENTS = new ImageIcon(ChainsawIcons.INFO);
    Icon SELECTED = LineIconFactory.createBlankIcon();

    public TabIconHandler(String identifier, ChainsawTabbedPane tabbedPane) {
        ident = identifier;
        this.tabbedPane = tabbedPane;

        new Thread(() -> {
                    while (true) {
                        // if this tab is active, remove the icon
                        // don't process undocked tabs
                        if (tabbedPane.indexOfTab(ident) > -1
                                && tabbedPane.getSelectedIndex() == tabbedPane.indexOfTab(ident)) {
                            tabbedPane.setIconAt(tabbedPane.indexOfTab(ident), SELECTED);
                            newEvents = false;
                            seenEvents = true;
                        } else if (tabbedPane.indexOfTab(ident) > -1) {
                            if (newEvents) {
                                tabbedPane.setIconAt(tabbedPane.indexOfTab(ident), NEW_EVENTS);
                                newEvents = false;
                                seenEvents = false;
                            } else if (!seenEvents) {
                                tabbedPane.setIconAt(tabbedPane.indexOfTab(ident), HAS_EVENTS);
                            }
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }
                    }
                })
                .start();
    }

    @Override
    public void eventCountChanged(int currentCount, int totalCount) {
        newEvents = true;
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (tabbedPane.indexOfTab(ident) > -1 && tabbedPane.indexOfTab(ident) == tabbedPane.getSelectedIndex()) {
            tabbedPane.setIconAt(tabbedPane.indexOfTab(ident), SELECTED);
        }
    }
}
