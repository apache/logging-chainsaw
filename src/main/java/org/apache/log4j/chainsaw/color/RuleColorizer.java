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

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A colorizer supporting an ordered collection of ColorRules, including support for notification of
 * color rule changes via a propertyChangeListener and the 'colorrule' property.
 *
 * @author Scott Deboy <sdeboy@apache.org>
 */
public class RuleColorizer implements Colorizer {
  public static final String DEFAULT_NAME = "Default";
  private Map rules;
  private final PropertyChangeSupport colorChangeSupport =
    new PropertyChangeSupport(this);
  private Map defaultRules = new HashMap();
  private String currentRuleSet = DEFAULT_NAME;
  private Rule findRule;
  private Rule loggerRule;

  private final Color WARN_DEFAULT_COLOR = new Color(255, 255, 153);
  private final Color ERROR_OR_FATAL_DEFAULT_COLOR = new Color(255, 153, 153);
  private final Color MARKER_DEFAULT_COLOR = new Color(153, 255, 153);

  private final String DEFAULT_WARN_EXPRESSION = "level == WARN";
  private final String DEFAULT_ERROR_FATAL_EXPRESSION = "level == FATAL || level == ERROR";
  private final String DEFAULT_MARKER_EXPRESSION = "prop.log4j.marker exists";

  public RuleColorizer() {
    List rulesList = new ArrayList();

      String expression = DEFAULT_ERROR_FATAL_EXPRESSION;
      rulesList.add(
      new ColorRule(
        expression, ExpressionRule.getRule(expression), ERROR_OR_FATAL_DEFAULT_COLOR,
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

    defaultRules.put(DEFAULT_NAME, rulesList);
    setRules(defaultRules);
  }

  public void setLoggerRule(Rule loggerRule) {
    this.loggerRule = loggerRule;
    colorChangeSupport.firePropertyChange("colorrule", false, true);
  }

  public void setFindRule(Rule findRule) {
    this.findRule = findRule;
    colorChangeSupport.firePropertyChange("colorrule", false, true);
  }

  public Rule getFindRule() {
    return findRule;
  }

  public Rule getLoggerRule() {
    return loggerRule;
  }

  public void setRules(Map rules) {
    this.rules = rules;
    colorChangeSupport.firePropertyChange("colorrule", false, true);
  }
  
  public Map getRules() {
    return rules;
  }

  public List getCurrentRules() {
    return (List) rules.get(currentRuleSet);
  }

  public void addRules(Map newRules) {
    Iterator iter = newRules.entrySet().iterator();

    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();

      if (rules.containsKey(entry.getKey())) {
        ((List) rules.get(entry.getKey())).addAll((List) entry.getValue());
      } else {
        rules.put(entry.getKey(), entry.getValue());
      }
    }

    colorChangeSupport.firePropertyChange("colorrule", false, true);
  }

  public void addRule(String ruleSetName, ColorRule rule) {
    if (rules.containsKey(ruleSetName)) {
      ((List) rules.get(ruleSetName)).add(rule);
    } else {
      List list = new ArrayList();
      list.add(rule);
      rules.put(ruleSetName, list);
    }

    colorChangeSupport.firePropertyChange("colorrule", false, true);
  }

  public void removeRule(String ruleSetName, String expression) {
    if (rules.containsKey(ruleSetName)) {
      List list = (List) rules.get(ruleSetName);

      for (int i = 0; i < list.size(); i++) {
        ColorRule rule = (ColorRule) list.get(i);

        if (rule.getExpression().equals(expression)) {
          list.remove(rule);

          return;
        }
      }
    }
  }

  public void setCurrentRuleSet(String ruleSetName) {
    currentRuleSet = ruleSetName;
  }

  public Color getBackgroundColor(LoggingEvent event) {
    if (rules.containsKey(currentRuleSet)) {
      List list = (List) rules.get(currentRuleSet);
      Iterator iter = list.iterator();

      while (iter.hasNext()) {
        ColorRule rule = (ColorRule) iter.next();

        if ((rule.getBackgroundColor() != null) && (rule.evaluate(event))) {
          return rule.getBackgroundColor();
        }
      }
    }

    return null;
  }

  public Color getForegroundColor(LoggingEvent event) {
    if (rules.containsKey(currentRuleSet)) {
      List list = (List) rules.get(currentRuleSet);
      Iterator iter = list.iterator();

      while (iter.hasNext()) {
        ColorRule rule = (ColorRule) iter.next();

        if ((rule.getForegroundColor() != null) && (rule.evaluate(event))) {
          return rule.getForegroundColor();
        }
      }
    }

    return null;
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    colorChangeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    colorChangeSupport.removePropertyChangeListener(listener);
  }

  /**
   * @param propertyName
   * @param listener
   */
  public void addPropertyChangeListener(
    String propertyName, PropertyChangeListener listener) {
    colorChangeSupport.addPropertyChangeListener(propertyName, listener);
  }
}
