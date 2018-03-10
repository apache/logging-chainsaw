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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import org.apache.log4j.rule.AbstractRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A mediator class that implements the Rule interface, by combining several 
 * optional rules used by Chainsaw's filtering GUI's into a single Rule.
 *
 * <p>Setting the individual sub-rules propagates a PropertyChangeEvent as per
 * standard Java beans principles.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class RuleMediator extends AbstractRule {
  private Rule loggerRule;
  private Rule filterRule;
  private Rule findRule;
  private final PropertyChangeListener ruleChangerNotifier = new RuleChangerNotifier();
  private boolean findRuleRequired;

  public RuleMediator(boolean findRuleRequired) {
    this.findRuleRequired = findRuleRequired;
  }
  /* (non-Javadoc)
   * @see org.apache.log4j.chainsaw.rule.Rule#evaluate(org.apache.log4j.spi.LoggingEvent)
   */
  public boolean evaluate(LoggingEvent e, Map matches) {
    if (findRuleRequired) {
      if (findRule == null) {
        return false;
      }
      if (!findRule.evaluate(e, null)) {
        return false;
      }
    }

    if (loggerRule != null && !loggerRule.evaluate(e, null)) {
      return false;
    }

      return filterRule == null || filterRule.evaluate(e, null);
  }

  public boolean isFindRuleRequired() {
    return findRuleRequired;
  }
  
  public void setFilterRule(Rule r) {
    Rule oldFilterRule = this.filterRule;
    this.filterRule = r;
    firePropertyChange("filterRule", oldFilterRule, this.filterRule);
  }

  public void setFindRule(Rule r) {
    Rule oldFindRule = this.findRule;
    this.findRule = r;
    firePropertyChange("findRule", oldFindRule, this.findRule);
  }

  public void setLoggerRule(Rule r) {
    Rule oldLoggerRule = this.loggerRule;
    this.loggerRule = r;
    if(oldLoggerRule!=null){
      oldLoggerRule.removePropertyChangeListener(ruleChangerNotifier);
    }
    this.loggerRule.addPropertyChangeListener(ruleChangerNotifier);
    firePropertyChange("loggerRule", oldLoggerRule, this.loggerRule);
  }

  /**
   * Helper class that propagates internal Rules propertyChange events
   * to external parties, since an internal rule changing really means
   * this outter rule is going to change too.
   */
  private class RuleChangerNotifier implements PropertyChangeListener {
    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
      RuleMediator.this.firePropertyChange(evt);
    }
  }
}
