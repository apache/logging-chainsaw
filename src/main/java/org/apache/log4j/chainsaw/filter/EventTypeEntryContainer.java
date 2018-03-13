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
 */
package org.apache.log4j.chainsaw.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;

/**
 * A Container class used to hold unique LoggingEvent values
 * and provide them as unique ListModels.
 * 
 * @author Paul Smith 
 *
 */
public class EventTypeEntryContainer {
  private Set<String> ColumnNames = new HashSet<>();
  private Set<String> Methods = new HashSet<>();
  private Set<String> Classes = new HashSet<>();
  private Set<String> NDCs = new HashSet<>();
  private Set Levels = new HashSet();
  private Set<String> Loggers = new HashSet<>();
  private Set<String> Threads = new HashSet<>();
  private Set<String> FileNames = new HashSet<>();
  private DefaultListModel<String> columnNameListModel = new DefaultListModel<>();
  private DefaultListModel methodListModel = new DefaultListModel();
  private DefaultListModel classesListModel = new DefaultListModel();
  private DefaultListModel propListModel = new DefaultListModel();
  private DefaultListModel ndcListModel = new DefaultListModel();
  private DefaultListModel levelListModel = new DefaultListModel();
  private DefaultListModel loggerListModel = new DefaultListModel();
  private DefaultListModel threadListModel = new DefaultListModel();
  private DefaultListModel fileNameListModel = new DefaultListModel();
  private Map modelMap = new HashMap();
  private static final String LOGGER_FIELD = "LOGGER";
  private static final String LEVEL_FIELD = "LEVEL";
  private static final String CLASS_FIELD = "CLASS";
  private static final String FILE_FIELD = "FILE";
  private static final String THREAD_FIELD = "THREAD";
  private static final String METHOD_FIELD = "METHOD";
  private static final String PROP_FIELD = "PROP.";
  private static final String NDC_FIELD = "NDC";

  public EventTypeEntryContainer() {
      modelMap.put(LOGGER_FIELD, loggerListModel);
      modelMap.put(LEVEL_FIELD, levelListModel);
      modelMap.put(CLASS_FIELD, classesListModel);
      modelMap.put(FILE_FIELD, fileNameListModel);
      modelMap.put(THREAD_FIELD, threadListModel);
      modelMap.put(METHOD_FIELD, methodListModel);
      modelMap.put(NDC_FIELD, ndcListModel);
      modelMap.put(PROP_FIELD, propListModel);
  }
  
  public boolean modelExists(String fieldName) {
      return fieldName != null && modelMap.keySet().contains(fieldName.toUpperCase());
  }
  
  public ListModel getModel(String fieldName) {
      if (fieldName != null) {
          ListModel model = (ListModel)modelMap.get(fieldName.toUpperCase());
          if (model != null) {
              return model;
          }
      }
      return null;
  } 
  
  void addLevel(Object level) {
    if (Levels.add(level)) {
      levelListModel.addElement(level);
    }
  }

  void addLogger(String logger) {
    if (Loggers.add(logger)) {
      loggerListModel.addElement(logger);
    }
  }

  void addFileName(String filename) {
    if (FileNames.add(filename)) {
      fileNameListModel.addElement(filename);
    }
  }

  void addThread(String thread) {
    if (Threads.add(thread)) {
      threadListModel.addElement(thread);
    }
  }

  void addNDC(String ndc) {
    if (NDCs.add(ndc)) {
      ndcListModel.addElement(ndc);
    }
  }

  void addColumnName(String name) {
    if (ColumnNames.add(name)) {
      columnNameListModel.addElement(name);
    }
  }

  void addMethod(String method) {
    if (Methods.add(method)) {
      methodListModel.addElement(method);
    }
  }

  void addClass(String className) {
    if (Classes.add(className)) {
      classesListModel.addElement(className);
    }
  }

  void addProperties(Map properties) {
    if(properties == null) {
     return;   
    }
      for (Object o : properties.entrySet()) {
          Map.Entry entry = (Map.Entry) o;
          if (!(propListModel.contains(entry.getKey()))) {
              propListModel.addElement(entry.getKey());
          }
      }
    }
}
