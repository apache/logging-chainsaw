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

package org.apache.log4j.chainsaw.layout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;


/**
 * Factory class to load and cache Layout information from resources.
 * 
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class DefaultLayoutFactory {
  private volatile static String defaultPatternLayout = null;

  private DefaultLayoutFactory() {
  }

  public static String getDefaultPatternLayout() {
      return getPatternLayout("org/apache/log4j/chainsaw/layout/DefaultDetailLayout.html");
  }
  public static String getFullPatternLayout() {
      return getPatternLayout("org/apache/log4j/chainsaw/layout/FullDetailLayout.html");
  }

  private static String getPatternLayout(String fileNamePath) {
      StringBuffer content = new StringBuffer();
      URL defaultLayoutURL =
        DefaultLayoutFactory.class.getClassLoader().getResource(fileNamePath);

      if (defaultLayoutURL == null) {
        LogManager.getLogger(DefaultLayoutFactory.class).warn(
          "Could not locate the default Layout for Event Details and Tooltips");
      } else {
        try {
          BufferedReader reader = null;

          try {
            reader =
              new BufferedReader(
                new InputStreamReader(defaultLayoutURL.openStream()));

            String line = "";

            while ((line = reader.readLine()) != null) {
              content.append(line).append("\n");
            }
          } finally {
            if (reader != null) {
              reader.close();
            }
          }
        } catch (Exception e) {
          content = new StringBuffer(PatternLayout.TTCC_CONVERSION_PATTERN);
        }
        String trimmedContent = content.toString().trim();
        //the default docs contain the apache license header, strip that out before displaying
        String startComment = "<!--";
        String endComment = "-->";
        if (trimmedContent.startsWith(startComment)) {
            int endIndex = trimmedContent.indexOf(endComment);
            if (endIndex > -1) {
                trimmedContent = trimmedContent.substring(endIndex + endComment.length()).trim();
            }
        }
        defaultPatternLayout = trimmedContent;
      }

    return defaultPatternLayout;
  }
}
