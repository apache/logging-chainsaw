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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Wrap access to a LoggingEvent.  All property updates need to go through this object and not through the wrapped logging event,
 * since the properties are shared by two views of the same backing LoggingEvent, and loggingEvent itself creates a copy of passed-in properties..
 *
 * Property reads can be made on the actual LoggingEvent.
 */
public class LoggingEventWrapper {
  private final LoggingEvent loggingEvent;
  private static final int DEFAULT_HEIGHT = -1;

  private Color colorRuleBackground = ChainsawConstants.COLOR_DEFAULT_BACKGROUND;
  private Color colorRuleForeground = ChainsawConstants.COLOR_DEFAULT_FOREGROUND;
  private int markerHeight = DEFAULT_HEIGHT;
  private int msgHeight = DEFAULT_HEIGHT;

  //set to the log4jid value via setId - assumed to never change
  private int id;

  private boolean searchMatch = false;
  //a Map of event fields to Sets of string matches (can be used to render matches differently)
  Map eventMatches = new HashMap();
  private LoggingEventWrapper syncWrapper;
  private boolean displayed;

  public LoggingEventWrapper(LoggingEvent loggingEvent) {
    this.loggingEvent = loggingEvent;
  }

  public LoggingEventWrapper(LoggingEventWrapper loggingEventWrapper) {
    this.loggingEvent = loggingEventWrapper.getLoggingEvent();
    this.id = loggingEventWrapper.id;
    this.syncWrapper = loggingEventWrapper;
    loggingEventWrapper.syncWrapper = this;
  }

  public LoggingEvent getLoggingEvent() {
    return loggingEvent;
  }

  public void setProperty(String propName, String propValue) {
    loggingEvent.setProperty(propName, propValue);
    if (id == 0 && propName.equals(Constants.LOG4J_ID_KEY)) {
      id = Integer.parseInt(propValue);
    }
    if (syncWrapper != null && !propName.equals(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE)) {
      syncWrapper.getLoggingEvent().setProperty(propName, propValue);
    }
  }

  public Object removeProperty(String propName) {
    Object result = loggingEvent.removeProperty(propName);
    if (syncWrapper != null && !propName.equals(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE)) {
      syncWrapper.getLoggingEvent().removeProperty(propName);
    }
    return result;
  }

  public Set getPropertyKeySet() {
    return loggingEvent.getPropertyKeySet();
  }

  public void updateColorRuleColors(Color backgroundColor, Color foregroundColor) {
    if (backgroundColor != null && foregroundColor != null) {
      this.colorRuleBackground = backgroundColor;
      this.colorRuleForeground = foregroundColor;
      if (syncWrapper != null) {
        syncWrapper.colorRuleBackground = this.colorRuleBackground;
        syncWrapper.colorRuleForeground = this.colorRuleForeground;
      }
    } else {
      this.colorRuleBackground = ChainsawConstants.COLOR_DEFAULT_BACKGROUND;
      this.colorRuleForeground = ChainsawConstants.COLOR_DEFAULT_FOREGROUND;
      if (syncWrapper != null) {
        syncWrapper.colorRuleBackground = this.colorRuleBackground;
        syncWrapper.colorRuleForeground = this.colorRuleForeground;
      }
    }
  }

  public void evaluateSearchRule(Rule searchRule) {
    eventMatches.clear();
    searchMatch = searchRule != null && searchRule.evaluate(loggingEvent, eventMatches);
  }

  public Map getSearchMatches() {
    return eventMatches;
  }

  public Color getForeground() {
    return colorRuleForeground;
  }

  public Color getBackground() {
    return colorRuleBackground;
  }

  public Color getColorRuleBackground() {
    return colorRuleBackground;
  }

  public Color getColorRuleForeground() {
    return colorRuleForeground;
  }

  public boolean isSearchMatch() {
    return searchMatch;
  }

  public void setMarkerHeight(int markerHeight) {
    this.markerHeight = markerHeight;
  }

  public int getMarkerHeight() {
    return markerHeight;
  }

  public void setMsgHeight(int msgHeight) {
    this.msgHeight = msgHeight;
  }

  public int getMsgHeight() {
    return msgHeight;
  }

  public void setDisplayed(boolean b) {
    markerHeight = DEFAULT_HEIGHT;
    msgHeight = DEFAULT_HEIGHT;
    displayed = b;
  }

  public void setPreviousDisplayedEventTimestamp(long previousDisplayedEventTimeStamp) {
    setProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE, String.valueOf(loggingEvent.getTimeStamp() - previousDisplayedEventTimeStamp));
  }

  public boolean isDisplayed() {
    return displayed;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LoggingEventWrapper that = (LoggingEventWrapper) o;

    if (id != that.id) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    return id;
  }

  public String toString() {
    return "LoggingEventWrapper - id: " + id + " background: " + getBackground() + ", foreground: " + getForeground() + ", msg: " + loggingEvent.getMessage();
  }
}
