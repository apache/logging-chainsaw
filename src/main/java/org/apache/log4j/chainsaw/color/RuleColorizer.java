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

package org.apache.log4j.chainsaw.color;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.DataConfiguration;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;


/**
 * A colorizer supporting an ordered collection of ColorRules, including support for notification of
 * color rule changes via a propertyChangeListener and the 'colorrule' property.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class RuleColorizer implements Colorizer {
    private static final Logger logger = LogManager.getLogger();
    public static final String PROPERTY_CHANGED_COLORRULE = "colorrule";

    private final List<ColorRule> rules;
    private final PropertyChangeSupport colorChangeSupport =
        new PropertyChangeSupport(this);

    private Rule findRule;
    private Rule loggerRule;

    private static final Color WARN_DEFAULT_COLOR = new Color(255, 255, 153);
    private static final Color FATAL_OR_ERROR_DEFAULT_COLOR = new Color(255, 153, 153);
    private static final Color MARKER_DEFAULT_COLOR = new Color(153, 255, 153);

    private static final String DEFAULT_WARN_EXPRESSION = "level == WARN";
    private static final String DEFAULT_FATAL_ERROR_EXCEPTION_EXPRESSION = "level == FATAL || level == ERROR || exception exists";
    private static final String DEFAULT_MARKER_EXPRESSION = "prop.marker exists";

    public static List<ColorRule> defaultRules(){
        List<ColorRule> rulesList = new ArrayList<>();

        String expression = DEFAULT_FATAL_ERROR_EXCEPTION_EXPRESSION;
        rulesList.add(
            new ColorRule(
                expression, ExpressionRule.getRule(expression), FATAL_OR_ERROR_DEFAULT_COLOR,
                Color.black));
        expression = DEFAULT_WARN_EXPRESSION;
        rulesList.add(
            new ColorRule(
                expression, ExpressionRule.getRule(expression), WARN_DEFAULT_COLOR,
                Color.black));

        expression = DEFAULT_MARKER_EXPRESSION;
        rulesList.add(
            new ColorRule(
                expression, ExpressionRule.getRule(expression), MARKER_DEFAULT_COLOR,
                Color.black));

        return rulesList;
    }

    public RuleColorizer() {
        this.rules = defaultRules();
    }

    public void setLoggerRule(Rule loggerRule) {
        this.loggerRule = loggerRule;
        colorChangeSupport.firePropertyChange(PROPERTY_CHANGED_COLORRULE, false, true);
    }

    public void setFindRule(Rule findRule) {
        this.findRule = findRule;
        colorChangeSupport.firePropertyChange(PROPERTY_CHANGED_COLORRULE, false, true);
    }

    public Rule getFindRule() {
        return findRule;
    }

    public Rule getLoggerRule() {
        return loggerRule;
    }

    public void setRules(List<ColorRule> rules) {
        this.rules.clear();
        this.rules.addAll(rules);
        colorChangeSupport.firePropertyChange(PROPERTY_CHANGED_COLORRULE, false, true);
    }

    public List<ColorRule> getRules() {
        return rules;
    }

    public void addRule(ColorRule rule) {
        rules.add(rule);

        colorChangeSupport.firePropertyChange(PROPERTY_CHANGED_COLORRULE, false, true);
    }

    /**
     * {{@inheritDoc}
     */
    @Override
    public Color getBackgroundColor(ChainsawLoggingEvent event) {
        for (ColorRule rule : rules) {

            if ((rule.getBackgroundColor() != null) && (rule.evaluate(event, null))) {
                return rule.getBackgroundColor();
            }
        }

        return null;
    }

    /**
     * {{@inheritDoc}
     */
    @Override
    public Color getForegroundColor(ChainsawLoggingEvent event) {
        for (ColorRule rule : rules) {

            if ((rule.getForegroundColor() != null) && (rule.evaluate(event, null))) {
                return rule.getForegroundColor();
            }
        }

        return null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        colorChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * @param propertyName
     * @param listener
     */
    public void addPropertyChangeListener(
        String propertyName, PropertyChangeListener listener) {
        colorChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public static String colorToRGBString( Color c ){
        return String.format( "#%02x%02x%02x",
                c.getRed(),
                c.getGreen(),
                c.getBlue());
    }

    public static List<Color> getDefaultColors() {
        List<Color> vec = new ArrayList<>();

        vec.add(Color.white);
        vec.add(Color.black);
        //add default alternating color & search backgrounds (both foreground are black)
        vec.add(ChainsawConstants.COLOR_ODD_ROW_BACKGROUND);
        vec.add(ChainsawConstants.FIND_LOGGER_BACKGROUND);

        vec.add(new Color(255, 255, 225));
        vec.add(new Color(255, 225, 255));
        vec.add(new Color(225, 255, 255));
        vec.add(new Color(255, 225, 225));
        vec.add(new Color(225, 255, 225));
        vec.add(new Color(225, 225, 255));
        vec.add(new Color(225, 225, 183));
        vec.add(new Color(225, 183, 225));
        vec.add(new Color(183, 225, 225));
        vec.add(new Color(183, 225, 183));
        vec.add(new Color(183, 183, 225));
        vec.add(new Color(232, 201, 169));
        vec.add(new Color(255, 255, 153));
        vec.add(new Color(255, 153, 153));
        vec.add(new Color(189, 156, 89));
        vec.add(new Color(255, 102, 102));
        vec.add(new Color(255, 177, 61));
        vec.add(new Color(61, 255, 61));
        vec.add(new Color(153, 153, 255));
        vec.add(new Color(255, 153, 255));

        return vec;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append( String.format( "RuleColorizer - %d rules", rules.size() ) );
        for( ColorRule rule : rules ){
            sb.append( System.lineSeparator() );
            sb.append( "  " );
            sb.append( rule.getRule().toString() );
        }
        sb.append( System.lineSeparator() );

        return sb.toString();
    }

}
