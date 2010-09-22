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

import org.apache.log4j.Logger;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;

public class ExtendedLoggingEvent extends LoggingEvent
{
    private static final int DEFAULT_HEIGHT = -1;

    private Color colorRuleBackground = ChainsawConstants.COLOR_DEFAULT_BACKGROUND;
    private Color colorRuleForeground = ChainsawConstants.COLOR_DEFAULT_FOREGROUND;
    private int markerHeight = DEFAULT_HEIGHT;
    private int msgHeight = DEFAULT_HEIGHT;

    private boolean searchMatch = false;
    //a Map of event fields to Sets of string matches (can be used to render matches differently)
    Map eventMatches = new HashMap();

    //copy constructor
    public ExtendedLoggingEvent(LoggingEvent e) {
        super(e.getFQNOfLoggerClass(), e.getLogger() != null ? e.getLogger() : Logger.getLogger(e.getLoggerName()), e.getTimeStamp(), e.getLevel(), e.getMessage(), e.getThreadName(), e.getThrowableInformation(), e.getNDC(), e.getLocationInformation(), e.getProperties());
    }

    public void updateColorRuleColors(Color backgroundColor, Color foregroundColor) {
        if (backgroundColor != null && foregroundColor != null) {
            this.colorRuleBackground = backgroundColor;
            this.colorRuleForeground = foregroundColor;
        } else {
            this.colorRuleBackground = ChainsawConstants.COLOR_DEFAULT_BACKGROUND;
            this.colorRuleForeground = ChainsawConstants.COLOR_DEFAULT_FOREGROUND;
        }
    }

    public void evaluateSearchRule(Rule searchRule) {
        eventMatches.clear();
        searchMatch = searchRule != null && searchRule.evaluate(this, eventMatches);
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

    public void setDisplayed(boolean b)
    {
        markerHeight = DEFAULT_HEIGHT;
        msgHeight = DEFAULT_HEIGHT;
        if (!b) {
            setProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE, "");
        }
    }

    public String toString() {
        return "ExtendedLoggingEvent - id: " + getProperty("log4jid") + " background: " + getBackground() + ", foreground: " + getForeground() + ", msg: " + getMessage();
    }

    public void setPreviousDisplayedEventTimestamp(long previousDisplayedEventTimeStamp)
    {
        setProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE, String.valueOf(timeStamp - previousDisplayedEventTimeStamp));
    }
}
