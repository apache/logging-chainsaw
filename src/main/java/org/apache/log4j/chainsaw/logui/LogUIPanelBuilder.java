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

package org.apache.log4j.chainsaw.logui;

import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.ChainsawStatusBar;
import org.apache.log4j.chainsaw.ChainsawToolBarAndMenus;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.components.elements.TabIconHandler;
import org.apache.log4j.chainsaw.components.logpanel.LogPanel;
import org.apache.log4j.chainsaw.components.tabbedpane.ChainsawTabbedPane;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.zeroconf.ZeroConfPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogUIPanelBuilder {
    private static Logger logger = LogManager.getLogger(LogUIPanelBuilder.class);

    ChainsawTabbedPane tabbedPane;
    List<LogPanel> identifierPanels;
    ChainsawToolBarAndMenus chainsawToolBarAndMenus;
    private Map<String, Component> panelMap;

    private SettingsManager settingsManager;
    private ChainsawStatusBar chainsawStatusBar;
    private ZeroConfPlugin zeroConfPlugin;

    private Map<String, RuleColorizer> allColorizers = new HashMap<>();
    private RuleColorizer globalRuleColorizer = new RuleColorizer(settingsManager, true);

    public LogUIPanelBuilder(ChainsawTabbedPane tabbedPane, List<LogPanel> identifierPanels,
                             ChainsawToolBarAndMenus chainsawToolBarAndMenus, Map<String, Component> panelMap,
                             SettingsManager settingsManager, ChainsawStatusBar chainsawStatusBar, ZeroConfPlugin zeroConfPlugin) {
        this.tabbedPane = tabbedPane;
        this.identifierPanels = identifierPanels;
        this.chainsawToolBarAndMenus = chainsawToolBarAndMenus;
        this.panelMap = panelMap;
        this.settingsManager = settingsManager;
        this.chainsawStatusBar = chainsawStatusBar;
        this.zeroConfPlugin = zeroConfPlugin;

        RuleColorizer colorizer = new RuleColorizer(settingsManager);
        allColorizers.put(ChainsawConstants.DEFAULT_COLOR_RULE_NAME, colorizer);
        globalRuleColorizer.setConfiguration(settingsManager.getGlobalConfiguration());
        globalRuleColorizer.loadColorSettings();
    }

    void buildLogPanel(boolean customExpression, final String ident, final ChainsawReceiver rx)
        throws IllegalArgumentException {
        final LogPanel thisPanel = new LogPanel(settingsManager, chainsawStatusBar, ident, allColorizers, globalRuleColorizer);

        if (!customExpression && rx != null) {
            thisPanel.setReceiver(rx);
        }

        /*
         * Now add the panel as a batch listener so it can handle it's own
         * batchs
         */
        if (!customExpression) {
            identifierPanels.add(thisPanel);
        }

        TabIconHandler iconHandler = new TabIconHandler(ident, tabbedPane);
        thisPanel.addEventCountListener(iconHandler);

        tabbedPane.addChangeListener(iconHandler);

        PropertyChangeListener toolbarMenuUpdateListener =
            evt -> chainsawToolBarAndMenus.stateChange();

        thisPanel.addPropertyChangeListener(toolbarMenuUpdateListener);
        thisPanel.addPreferencePropertyChangeListener(toolbarMenuUpdateListener);

        thisPanel.addPropertyChangeListener(
            "docked",
            evt -> {
                LogPanel logPanel = (LogPanel) evt.getSource();

                if (logPanel.isDocked()) {
                    panelMap.put(logPanel.getIdentifier(), logPanel);
                    tabbedPane.addANewTab(
                        logPanel.getIdentifier(), logPanel, null,
                        true);
                    tabbedPane.setSelectedTab(tabbedPane.indexOfTab(logPanel.getIdentifier()));
                } else {
                    tabbedPane.remove(logPanel);
                }
            });

        logger.debug("adding logpanel to tabbed pane: {}", ident);

        //NOTE: tab addition is a very fragile process - if you modify this code,
        //verify the frames in the individual log panels initialize to their
        //correct sizes
        tabbedPane.add(ident, thisPanel);
        panelMap.put(ident, thisPanel);

        /*
         * Let the new LogPanel receive this batch
         */

        SwingUtilities.invokeLater(
            () -> {
                tabbedPane.addANewTab(
                    ident,
                    thisPanel,
                    new ImageIcon(ChainsawIcons.ANIM_RADIO_TOWER),
                    false);
                thisPanel.layoutComponents();

                tabbedPane.addANewTab(ChainsawTabbedPane.ZEROCONF,
                    zeroConfPlugin,
                    null,
                    false);
            });

        logger.debug("added tab {}", ident);
    }
}
