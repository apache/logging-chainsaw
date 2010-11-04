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

package org.apache.log4j.chainsaw.helper;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * A collection of standard utility methods for use within Swing.
 * 
 * @author Paul Smith <psmith@apache.org>
 *
 */
public final class SwingHelper {
  /**
   * Centers the Component on screen.
   *
   * @param component
   */
  public static void centerOnScreen(Component component) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    component.setLocation(
      (screenSize.width / 2) - (component.getWidth() / 2),
      (screenSize.height / 2) - (component.getHeight() / 2));
  }
  
  /**
   * This method configures a standard Cancel action, bound to the ESC key, to dispose of the dialog,
   * and sets the buttons action to be this action, and adds the action to the dialog's rootPane 
   * action map
   * @param dialog
   * @param cancelButton
   */
  public static void configureCancelForDialog(final JDialog dialog, JButton cancelButton) {
    String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";
    int noModifiers = 0;
    KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, noModifiers, false);
    InputMap inputMap = dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(escapeKey, CANCEL_ACTION_KEY);
    
    Action closeAction = new AbstractAction("Cancel") {

      public void actionPerformed(ActionEvent arg0) {
        dialog.dispose();
      }};
    cancelButton.setAction(closeAction);
    dialog.getRootPane().getActionMap().put(CANCEL_ACTION_KEY, closeAction);
    
  }

  public static void invokeOnEDT(Runnable runnable) {
    if (EventQueue.isDispatchThread()) {
      runnable.run();
    } else {
      EventQueue.invokeLater(runnable);
    }
  }

  public static boolean isMacOSX() {
    return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  }

  public static List orderOKCancelButtons(JButton okButton, JButton cancelButton) {
    List result = new ArrayList();
    if (isMacOSX()) {
      result.add(cancelButton);
      result.add(okButton);
    } else {
      result.add(okButton);
      result.add(cancelButton);
    }
    return result;
  }

  public static File promptForFile(Container parent, String defaultPath, String title, boolean loadDialog) {
        if (SwingHelper.isMacOSX()) {
            //use filedialog on mac
            Component root = SwingUtilities.getRoot(parent);
            Frame frame = null;
            if (root instanceof Frame) {
              frame = (Frame) root;
            }

            FileDialog fileDialog = new FileDialog(frame, title);
            fileDialog.setModal(true);
            fileDialog.setMode(loadDialog ? FileDialog.LOAD : FileDialog.SAVE);
            if (defaultPath != null) {
              fileDialog.setDirectory(defaultPath);
            }
            fileDialog.setVisible(true);
            String fileString = fileDialog.getFile();
            if (fileString == null) {
              return null;
            }
          if (fileDialog.getDirectory() != null) {
            return new File(fileDialog.getDirectory(), fileString);
          } else {
            return new File(fileString);
          }
          } else {

                JFileChooser chooser;
                if (defaultPath != null) {
                  chooser = new JFileChooser(defaultPath);
                } else {
                  chooser = new JFileChooser();
                }

                chooser.setDialogTitle(title);

                chooser.setAcceptAllFileFilterUsed(true);

                int i;
                if (loadDialog) {
                  i = chooser.showOpenDialog(parent);
                } else {
                  i = chooser.showSaveDialog(parent);
                }

                if (i != JFileChooser.APPROVE_OPTION) {
                    return null;
                }
            return chooser.getSelectedFile();
        }
    }
}
