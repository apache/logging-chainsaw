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
package org.apache.log4j.chainsaw.icons;

import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 */
public class LevelIconFactory {
    private static final LevelIconFactory instance = new LevelIconFactory();
    private final Map<String, javax.swing.Icon> iconMap = new HashMap<>();

    private LevelIconFactory() {
        // reuse ERROR icon for FATAL level
        String[] iconFileNames = new String[] {"Warn.gif", "Inform.gif", "Error.gif", "Error.gif"};
        String[] iconLabels = new String[] {"WARN", "INFO", "ERROR", "FATAL"};

        for (int i = 0; i < iconLabels.length; i++) {
            URL resourceURL = UIManager.getLookAndFeel().getClass().getResource("icons/" + iconFileNames[i]);
            if (resourceURL == null) {
                resourceURL = MetalLookAndFeel.class.getResource("icons/" + iconFileNames[i]);
            }
            if (resourceURL == null) {
                iconMap.put(iconLabels[i], ChainsawIcons.ICON_DEBUG);
            } else {

                final ImageIcon icon = new ImageIcon(resourceURL);
                double scalex = .5;
                double scaley = .5;
                final int newWidth = (int) (scalex * icon.getIconWidth());
                final int newHeight = (int) (scaley * icon.getIconHeight());
                Image iconImage = icon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                iconMap.put(iconLabels[i], new ImageIcon(iconImage));
            }
        }
        // reuse DEBUG icon for TRACE level
        iconMap.put("TRACE", ChainsawIcons.ICON_DEBUG);
        iconMap.put("DEBUG", ChainsawIcons.ICON_DEBUG);
    }

    public static final LevelIconFactory getInstance() {
        return instance;
    }

    public Map<String, javax.swing.Icon> getLevelToIconMap() {
        return iconMap;
    }
}
