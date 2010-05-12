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

import java.awt.Color;
import java.net.URL;

/**
 * 
 * Constants used throught Chainsaw.
 * 
 * @author Paul Smith <psmith@apache.org>
 * @author Scott Deboy <sdeboy@apache.org>
 * 
 */
public class ChainsawConstants {
  private ChainsawConstants(){}
  
  public static final String DEFAULT_COLOR_RULE_NAME = "Default";
  public static final Color COLOR_DEFAULT_BACKGROUND = new Color(255,255,255);
  public static final Color COLOR_DEFAULT_FOREGROUND = Color.BLACK;

  public static final int DEFAULT_ROW_HEIGHT = 20;
  public static final Color FIND_LOGGER_BACKGROUND = new Color(213, 226, 235);
  public static final Color FIND_LOGGER_FOREGROUND = Color.BLACK;

  public static final Color COLOR_ODD_ROW_BACKGROUND = new Color(227, 227, 227);
  public static final Color COLOR_ODD_ROW_FOREGROUND = Color.BLACK;

  public static final Color COLOR_EVEN_ROW_BACKGROUND = COLOR_DEFAULT_BACKGROUND;
  public static final Color COLOR_EVEN_ROW_FOREGROUND = Color.BLACK;

  public static final URL WELCOME_URL = ChainsawConstants.class.getClassLoader().getResource(
  "org/apache/log4j/chainsaw/WelcomePanel.html");
  
  public static final URL EXAMLE_CONFIG_URL =
  ChainsawConstants.class.getClassLoader().getResource(
	  "org/apache/log4j/chainsaw/log4j-receiver-sample.xml");

  public static final URL TUTORIAL_URL =
  ChainsawConstants.class.getClassLoader().getResource(
	  "org/apache/log4j/chainsaw/help/tutorial.html");
  public static final URL RELEASE_NOTES_URL =
      ChainsawConstants.class.getClassLoader().getResource(
          "org/apache/log4j/chainsaw/help/release-notes.html");
        
  static final String MAIN_PANEL = "panel";
  static final String LOWER_PANEL = "lower";
  static final String UPPER_PANEL = "upper";
  static final String EMPTY_STRING = "";
  static final String FILTERS_EXTENSION = ".filters";
  static final String SETTINGS_EXTENSION = ".settings";

  //COLUMN NAMES
  static final String LOGGER_COL_NAME = "LOGGER";
  static final String LOG4J_MARKER_COL_NAME_LOWERCASE = "log4j.marker"; //properties are case-sensitive-using lowercase for easier entry
  static final String TIMESTAMP_COL_NAME = "TIMESTAMP";
  static final String LEVEL_COL_NAME = "LEVEL";
  static final String THREAD_COL_NAME = "THREAD";
  static final String MESSAGE_COL_NAME = "MESSAGE";
  static final String NDC_COL_NAME = "NDC";
  static final String THROWABLE_COL_NAME = "THROWABLE";
  static final String CLASS_COL_NAME = "CLASS";
  static final String METHOD_COL_NAME = "METHOD";
  static final String FILE_COL_NAME = "FILE";
  static final String LINE_COL_NAME = "LINE";
  static final String PROPERTIES_COL_NAME = "PROPERTIES";
  static final String ID_COL_NAME = "ID";

  //none is not a real column name, but is used by filters as a way to apply no filter for colors or display
  static final String NONE_COL_NAME = "None";
  static final String LOG4J_REMOTEHOST_KEY = "log4j.remoteSourceInfo";
  static final String UNKNOWN_TAB_NAME = "Unknown";
  static final String GLOBAL_MATCH = "*";
  public static final String DETAIL_CONTENT_TYPE = "text/html";

  static final String LEVEL_DISPLAY = "level.display";
  static final String LEVEL_DISPLAY_ICONS = "icons";
  static final String LEVEL_DISPLAY_TEXT = "text";


  static final String DATETIME_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";
  
//  TODO come up with a better page not found url
  public static final URL URL_PAGE_NOT_FOUND = WELCOME_URL;

}
