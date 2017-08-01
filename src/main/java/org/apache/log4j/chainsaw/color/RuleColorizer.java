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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A colorizer supporting an ordered collection of ColorRules, including support for notification of
 * color rule changes via a propertyChangeListener and the 'colorrule' property.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class RuleColorizer implements Colorizer {
  private Map rules;
  private final PropertyChangeSupport colorChangeSupport =
    new PropertyChangeSupport(this);
  private Map defaultRules = new HashMap();
  private String currentRuleSet = ChainsawConstants.DEFAULT_COLOR_RULE_NAME;
  private Rule findRule;
  private Rule loggerRule;

  private static final String COLORS_EXTENSION = ".colors";

  private final Color WARN_DEFAULT_COLOR = new Color(255, 255, 153);
  private final Color FATAL_OR_ERROR_DEFAULT_COLOR = new Color(255, 153, 153);
  private final Color MARKER_DEFAULT_COLOR = new Color(153, 255, 153);

  private final String DEFAULT_WARN_EXPRESSION = "level == WARN";
  private final String DEFAULT_FATAL_ERROR_EXCEPTION_EXPRESSION = "level == FATAL || level == ERROR || exception exists";
  private final String DEFAULT_MARKER_EXPRESSION = "prop.marker exists";

  public RuleColorizer() {
    List rulesList = new ArrayList();

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

    defaultRules.put(currentRuleSet, rulesList);
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

        if ((rule.getBackgroundColor() != null) && (rule.evaluate(event, null))) {
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

        if ((rule.getForegroundColor() != null) && (rule.evaluate(event, null))) {
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


    /**
     * Save panel color settings
     */
    public void saveColorSettings(String name) {
      ObjectOutputStream o = null;
      try {
        File f = new File(SettingsManager.getInstance().getSettingsDirectory(), URLEncoder.encode(name + COLORS_EXTENSION));

        o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));

        o.writeObject(getRules());
        o.flush();
      } catch (FileNotFoundException fnfe) {
        fnfe.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      } finally {
        try {
          if (o != null) {
            o.close();
          }
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }

  /**
   * Load panel color settings if they exist - otherwise, load default color settings
   */
  public void loadColorSettings(String name) {
    if (!doLoadColorSettings(name)) {
      doLoadColorSettings(ChainsawConstants.DEFAULT_COLOR_RULE_NAME);
    }
  }

  private boolean doLoadColorSettings(String name) {
    //first attempt to load encoded file
    File f = new File(SettingsManager.getInstance().getSettingsDirectory(), URLEncoder.encode(name) + COLORS_EXTENSION);

    if (f.exists()) {
      ObjectInputStream s = null;

      try {
        s = new ObjectInputStream(
            new BufferedInputStream(new FileInputStream(f)));

        Map map = (Map) s.readObject();
        setRules(map);
      } catch (EOFException eof) { //end of file - ignore..
      }catch (IOException ioe) {
        ioe.printStackTrace();
        //unable to load file - delete it
        f.delete();
      } catch (ClassNotFoundException cnfe) {
        cnfe.printStackTrace();
      } finally {
        if (s != null) {
          try {
            s.close();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
      }
    }
    return f.exists();
  }

    public Vector getDefaultColors() {
      Vector vec = new Vector();

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

}
