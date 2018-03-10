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

/*
 * @author Paul Smith &lt;psmith@apache.org&gt;
 *
*/
package org.apache.log4j.chainsaw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import org.apache.log4j.chainsaw.icons.ChainsawIcons;


/**
 * A simple splash screen to be used at startup, while everything get's initialized.
 * @author Paul Smith &lt;psmith@apache.org&gt;
 *
 */
class ChainsawSplash extends JWindow {
  ChainsawSplash(Frame owner) {
    super(owner);

    Container container = getContentPane();
    JPanel panel = new JPanel(new BorderLayout());
    JLabel logo = new JLabel(ChainsawIcons.ICON_LOG4J);

    JLabel text = new JLabel("Chainsaw v2", SwingConstants.CENTER);
    Font textFont = null;
    String[] preferredFontNames =
      new String[] { "Arial", "Helvetica", "SansSerif" };

    Set availableFontNames = new HashSet();
    Font[] allFonts =
      GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

      for (Font allFont : allFonts) {
          availableFontNames.add(allFont.getName());
      }

      for (String preferredFontName : preferredFontNames) {
          if (availableFontNames.contains(preferredFontName)) {
              textFont = new Font(preferredFontName, Font.PLAIN, 12);

              System.out.println("Using font=" + textFont.getName());

              break;
          }
      }

    if (textFont == null) {
      System.out.println("Using basic font");
      textFont = text.getFont();
    }

    text.setFont(textFont.deriveFont(16f).deriveFont(Font.BOLD));
    text.setBackground(Color.white);
    text.setForeground(Color.black);
    text.setBorder(BorderFactory.createLoweredBevelBorder());
    panel.add(logo, BorderLayout.CENTER);
    panel.add(text, BorderLayout.SOUTH);
    panel.setBorder(BorderFactory.createLineBorder(Color.black, 1));

    container.add(panel);
    pack();
  }
}
