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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.XMLLayout;


/**
 * Allows the user to specify a particular file to which the current tab's
 * displayed events will be saved.
 *
 * @author Scott Deboy <sdeboy@apache.org>
 * @author Paul Smith <psmith@apache.org>
 * @author Stephen Pain
 */
class FileSaveAction extends AbstractAction {
  private LogUI parent;
  private JFileChooser chooser = null;

  /**
   * This action must have a reference to a LogUI
   * in order to retrieve events to save
   *
   */
  public FileSaveAction(LogUI parent) {
    super("Save displayed events as...");

    putValue(
      Action.ACCELERATOR_KEY,
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
    putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
    putValue(
      Action.SHORT_DESCRIPTION, "Saves displayed events for the current tab");
    putValue(Action.SMALL_ICON, new ImageIcon(ChainsawIcons.FILE_SAVE_AS));
    this.parent = parent;
  }
  
  /*
   * When the user chooses the Save action,
   * a File chooser is presented to allow them to
   * find an XML file to save events to.
   *
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {

    if( chooser == null ){
      chooser = new JFileChooser();
    }
    
    chooser.setAcceptAllFileFilterUsed(true);
    chooser.setDialogTitle("Save displayed events (XML or .zipped XML)...");
    chooser.showSaveDialog(parent);

    File selectedFile = chooser.getSelectedFile();

    if (selectedFile != null) {
    List v = parent.getCurrentLogPanel().getFilteredEvents();

    if (((v != null) && (v.size() == 0)) || (v == null)) {
      //no events to save
      return;
    }

    XMLLayout layout = new XMLLayout();
    layout.setProperties(true);
    boolean saveAsZip = selectedFile.getName().toLowerCase().endsWith(".zip");
    Writer writer = null;
    try {
      if (saveAsZip) {
          ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(selectedFile)));
          ZipEntry entry = new ZipEntry(selectedFile.getName().substring(0, selectedFile.getName().length() - ".zip".length()) + ".xml");
          zipOutput.putNextEntry(entry);
          writer = new OutputStreamWriter(zipOutput);
      } else {
          writer = new BufferedWriter(new FileWriter(selectedFile));
      }
      Iterator iter = v.iterator();
      while (iter.hasNext()) {
        LoggingEvent event = (LoggingEvent) iter.next();
        layout.setLocationInfo(event.getThrowableInformation() != null);
        writer.write(layout.format(event));
      }
    } catch (IOException ioe) {
      LogLog.warn("Unable to save file", ioe);
    } finally {
      if (writer != null) {
          try {
              writer.flush();
              writer.close();
          }
          catch (IOException e1) {
              //ignore
          }
      }
    }
  }
  }
}
