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

import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.prefs.MRUFileList;
import org.apache.log4j.xml.UtilLoggingXMLDecoder;
import org.apache.log4j.xml.XMLDecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;


/**
 * The complete File Menu for the main GUI, containing
 * the Load, Save, Close Welcome Tab, and Exit actions
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
class FileMenu extends JMenu {
    private Action loadConfigAction;
    private Action exitAction;
    private Action loadLog4JAction;
    private Action loadUtilLoggingAction;
    private Action remoteLog4JAction;
    private Action remoteUtilLoggingAction;
    private Action saveAction;

    public FileMenu(final LogUI logUI) {
        super("File");
        setMnemonic(KeyEvent.VK_F);

        loadConfigAction = new AbstractAction("Load Chainsaw configuration") {
            public void actionPerformed(ActionEvent actionEvent) {
                logUI.showReceiverConfiguration();
            }
        };

        loadLog4JAction = null;
//            new FileLoadAction(
//                logUI, new XMLDecoder(logUI), "Open log4j XML-formatted file (.xml or .zip)...", false);

        loadLog4JAction.putValue(
            Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        loadLog4JAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
        loadLog4JAction.putValue(Action.SHORT_DESCRIPTION, "Loads events from a local XMLLayout-formatted file ");
        loadLog4JAction.putValue(Action.SMALL_ICON, new ImageIcon(ChainsawIcons.FILE_OPEN));

        loadUtilLoggingAction = null;
//            new FileLoadAction(
//                logUI, new UtilLoggingXMLDecoder(logUI),
//                "Open util.logging XML-formatted file (.xml or .zip)...", false);

        remoteLog4JAction = null;
//            new FileLoadAction(
//                logUI, new XMLDecoder(logUI), "Open remote log4j XML-formatted file (.xml or .zip)...",
//                true);
        remoteUtilLoggingAction = null;
//            new FileLoadAction(
//                logUI, new UtilLoggingXMLDecoder(logUI),
//                "Open remote util.logging XML-formatted file (.xml or .zip)...", true);

        saveAction = new FileSaveAction(logUI);

        JMenuItem loadChainsawConfig = new JMenuItem(loadConfigAction);
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
            KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        exitAction.putValue(Action.SHORT_DESCRIPTION, "Exits the Application");
        exitAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
        exitAction.putValue(Action.NAME, "Exit");

        JMenuItem menuItemExit = new JMenuItem(exitAction);

        add(loadChainsawConfig);
        add(loadLog4JFile);
        add(loadUtilLoggingFile);
        addSeparator();
        add(remoteLog4JFile);
        add(remoteUtilLoggingFile);
        addSeparator();
        add(saveFile);
        addSeparator();

        final JMenu mrulog4j = new JMenu("MRU...");


        MRUFileList.addChangeListener(e -> buildMRUMenu(mrulog4j, logUI));
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
            for (Object o : MRUFileList.log4jMRU().getMRUList()) {
                final URL url = (URL) o;
                // TODO work out the 'name', for local files it can't just be the full path
                final String name = url.getProtocol().startsWith("file") ? url.getPath().substring(url.getPath().lastIndexOf('/') + 1) : url.getPath();
                String title = (counter++) + " - " + url.toExternalForm();
                JMenuItem menuItem = new JMenuItem(new AbstractAction(title) {

                    public void actionPerformed(ActionEvent e) {
//                        FileLoadAction.importURL(logui.handler,
//                            new XMLDecoder(), name, url);
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
