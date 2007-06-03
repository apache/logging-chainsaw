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
 * @author Paul Smith <psmith@apache.org>
 *
*/
package org.apache.log4j.chainsaw;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.prefs.MRUFileList;
import org.apache.log4j.xml.UtilLoggingXMLDecoder;
import org.apache.log4j.xml.XMLDecoder;


/**
 * The complete File Menu for the main GUI, containing
 * the Load, Save, Close Welcome Tab, and Exit actions
 *
 * @author Paul Smith <psmith@apache.org>
 * @author Scott Deboy <sdeboy@apache.org>
 */
class FileMenu extends JMenu {
  private Action exitAction;
  private Action loadLog4JAction;
  private Action loadUtilLoggingAction;
  private Action remoteLog4JAction;
  private Action remoteUtilLoggingAction;
  private Action saveAction;

  public FileMenu(final LogUI logUI) {
    super("File");
    setMnemonic(KeyEvent.VK_F);

    loadLog4JAction =
      new FileLoadAction(
        logUI, new XMLDecoder(logUI), "Load Log4J File...", false);

      loadLog4JAction.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
      loadLog4JAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
      loadLog4JAction.putValue(Action.SHORT_DESCRIPTION, "Loads an XML event file");
      loadLog4JAction.putValue(Action.SMALL_ICON, new ImageIcon(ChainsawIcons.FILE_OPEN));

    loadUtilLoggingAction =
      new FileLoadAction(
        logUI, new UtilLoggingXMLDecoder(logUI),
        "Load Java Util File...", false);

    remoteLog4JAction =
      new FileLoadAction(
        logUI, new XMLDecoder(logUI), "Load Remote Log4J File...",
        true);
    remoteUtilLoggingAction =
      new FileLoadAction(
        logUI, new UtilLoggingXMLDecoder(logUI),
        "Load Remote Java Util File...", true);

    saveAction = new FileSaveAction(logUI);

    JMenuItem loadLog4JFile = new JMenuItem(loadLog4JAction);
    JMenuItem loadUtilLoggingFile = new JMenuItem(loadUtilLoggingAction);
    JMenuItem remoteLog4JFile = new JMenuItem(remoteLog4JAction);
    JMenuItem remoteUtilLoggingFile = new JMenuItem(remoteUtilLoggingAction);
    JMenuItem saveFile = new JMenuItem(saveAction);

    exitAction =
      new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            logUI.exit();
          }
        };

    exitAction.putValue(
      Action.ACCELERATOR_KEY,
      KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK));
    exitAction.putValue(Action.SHORT_DESCRIPTION, "Exits the Application");
    exitAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
    exitAction.putValue(Action.NAME, "Exit");

    JMenuItem menuItemExit = new JMenuItem(exitAction);

    add(loadLog4JFile);
    add(loadUtilLoggingFile);
    addSeparator();
    add(remoteLog4JFile);
    add(remoteUtilLoggingFile);
    addSeparator();
    add(saveFile);
    addSeparator();

    final JMenu mrulog4j = new JMenu("MRU...");
    
  
    
    MRUFileList.addChangeListener(new ChangeListener() {
        
        public void stateChanged(ChangeEvent e) {
            
            buildMRUMenu(mrulog4j, logUI);
        }
        
    });
    buildMRUMenu(mrulog4j, logUI);
    
    add(mrulog4j);
    if (!OSXIntegration.IS_OSX) {
        addSeparator();
        add(menuItemExit);
    }
    
    
  }

  private void buildMRUMenu(final JMenu mrulog4j, final LogUI logui) {
        mrulog4j.removeAll();
        int counter = 1;
        if (MRUFileList.log4jMRU().getMRUList().size() > 0) {
            for (Iterator iter = MRUFileList.log4jMRU().getMRUList().iterator(); iter
                    .hasNext();) {
                final URL url = (URL) iter.next();
                // TODO work out the 'name', for local files it can't just be the full path
                final String name = url.getProtocol().startsWith("file")?url.getPath().substring(url.getPath().lastIndexOf('/')+1):url.getPath();
                String title = (counter++) + " - " + url.toExternalForm();
                JMenuItem menuItem = new JMenuItem(new AbstractAction(title) {

                    public void actionPerformed(ActionEvent e) {
                        FileLoadAction.importURL(logui.handler,
                                new XMLDecoder(), name, url);
                    }
                });
                mrulog4j.add(menuItem);
            }
        } else {
            JMenuItem none = new JMenuItem("None as yet...");
            none.setEnabled(false);
            mrulog4j.add(none);
        }
    }
  Action getLog4JFileOpenAction() {
    return loadLog4JAction;
  }

  Action getUtilLoggingJFileOpenAction() {
    return loadUtilLoggingAction;
  }

  Action getFileSaveAction() {
    return saveAction;
  }

  Action getExitAction() {
    return exitAction;
  }
}
