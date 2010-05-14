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

import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import org.apache.log4j.chainsaw.prefs.LoadSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SaveSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SettingsListener;
import org.apache.log4j.chainsaw.prefs.SettingsManager;


/**
 * The only reason this class is needed is because
 * of a stupid 'issue' with the JTabbedPane.
 *
 * If the currently selected tab is the first tab,
 * and we insert a new tab at the front, then as
 * far as the JTabbedPane is concerned, NO STATE has
 * changed, as the currently selected tab index is still
 * the same (even though the TAB is different - go figure)
 * and therefore no ChangeEvent is generated and sent
 * to listeners.  Thanks very much Sun!
 *
 * For more information on the issue:
 * http://developer.java.sun.com/developer/bugParade/bugs/4253819.html
 * 
 * @author Paul Smith <psmith@apache.org>
 * @author Scott Deboy <sdeboy@apache.org>
 *
 */

class ChainsawTabbedPane extends JTabbedPane implements SettingsListener {
  public SavableTabSetting tabSetting;
  public static final String WELCOME_TAB = "Welcome";
  public static final String ZEROCONF = "Zeroconf";
  /**
   *
   * Create the tabbed pane.  
   *
   */
  public ChainsawTabbedPane() {
    super();
  }

  /**
   * Returns true if this TabbedPane has an instance of the WelcomePanel
   * in it
   * @return true/false
   */
  boolean containsWelcomePanel() {
    return indexOfTab("Welcome") > -1;
  }

  /**
   * Our custom implementation of inserting a new tab,
   * this method ALWAYS inserts it at the front because
   * we get an ArrayIndexOutOfBoundsException otherwise
   * under some JDK implementations.
   *
   * This method also causes a fireStateChange() to be
   * called so that listeners get notified of the event.
   * See the class level comments for the reason why...
   * @param name
   * @param component
   */
  public void addANewTab(String name, JComponent component, Icon icon) {
    super.insertTab(name, icon, component, null, getTabCount());

    super.fireStateChanged();
    if (!"chainsaw-log".equals(name)) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                setSelectedTab(getTabCount() - 1);
            }
        });
    }
  }

  public void setSelectedTab(int index) {
    if (getTabCount() >= index) {
      setSelectedIndex(index);
    }

    getSelectedComponent().setVisible(true);
    getSelectedComponent().validate();
    super.fireStateChanged();
  }

  public void addANewTab(
    String name, JComponent component, Icon icon, String tooltip) {
    super.insertTab(name, icon, component, tooltip, getTabCount());
    super.fireStateChanged();
  }

  public void remove(Component component) {
    super.remove(component);
    super.fireStateChanged();
  }

  /**
   * Saves the state of the currently active tabs to an XML file.
   * Only considers the Welcome, Drag and Drop and chainsaw-log
   * panels as they are the panel which are always running. Saves
   * whether they are hidden or not....
   */

  public void saveSettings(SaveSettingsEvent event){
   File file = new File(SettingsManager.getInstance().getSettingsDirectory(), "tab-settings.xml");
   XStream stream = new XStream(new DomDriver());
   try {
     FileWriter writer = new FileWriter(file);
     int count = super.getTabCount();
     String title;
     SavableTabSetting setting = new SavableTabSetting();
     for(int i = 0 ; i < count ; i++){
       title = super.getTitleAt(i);
       if(title.equals(WELCOME_TAB)){
         setting.setWelcome(true);
       } else if (title.equals("chainsaw-log")){
         setting.setChainsawLog(true);
       } else if (title.equals(ZEROCONF)){
         setting.setZeroconf(true);
       }
     }

     stream.toXML(setting, writer);
     writer.close();

   } catch (Exception e) {
     file.delete();
     e.printStackTrace();
   }
  }

  /**
   * Loads the saved tab setting by reading the XML file.
   * If the file doesn't exist, all three panels should be
   * shown as the default setting....
   */

  public void loadSettings(LoadSettingsEvent event){
    File file = new File(SettingsManager.getInstance().getSettingsDirectory(), "tab-settings.xml");
    XStream stream = new XStream(new DomDriver());
    try {
      if (file.exists()) {
        FileReader reader = new FileReader(file);
        tabSetting = (SavableTabSetting) stream.fromXML(reader);
        reader.close();
      } else {
        tabSetting = new SavableTabSetting();
        tabSetting.setWelcome(true);
        tabSetting.setChainsawLog(true);
        tabSetting.setZeroconf(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      file.delete();
    }
  }
}
