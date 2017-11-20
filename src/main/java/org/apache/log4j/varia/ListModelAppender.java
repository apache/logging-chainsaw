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

package org.apache.log4j.varia;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;


/**
 * A very basic appender that takes the events and stores them in to a
 * ListModel for late retrieval.
 *
 *
 * @author Paul Smith (psmith@apache.org)
 *
 */
public final class ListModelAppender extends AbstractAppender {
    /**
     * Default list model.
     */
  private final DefaultListModel<LogEvent> model = new DefaultListModel<>();

  /**
   * Constructs a ListModelAppender.
   */
  public ListModelAppender() {
    super("ListModelAppender", null, null);
  }
  /**
   * Returns a reference to the ListModel that contains all the LoggingEvents
   * that have been appended to this class.
   *
   * @return the list model
   */
  public ListModel getModel() {
    return model;
  }

  public void append(final LogEvent event) {
    model.addElement(event);
  }

  public void stop() {
    clearModel();
  }

  /**
   * Removes all the Events from the model.
   */
  public void clearModel() {
    model.clear();
  }

}
