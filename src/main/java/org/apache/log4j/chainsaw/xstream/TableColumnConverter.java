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
package org.apache.log4j.chainsaw.xstream;

import javax.swing.table.TableColumn;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream Converter implementation that deals with TableColumns settings
 * 
 * @see Converter
 * @see XStream
 * @see TableColumn
 * @author psmith
 * 
 */
public class TableColumnConverter implements Converter {

    public boolean canConvert(Class type) {
        return TableColumn.class.equals(type);
    }

    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        TableColumn column = (TableColumn) source;
        writer.addAttribute("width", column.getWidth() + "");
        writer.addAttribute("modelIndex", column.getModelIndex() + "");
        writer.addAttribute("headerValue", column.getHeaderValue().toString());
    }

    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {
        TableColumn column = new TableColumn();
        column.setWidth(Integer.parseInt(reader.getAttribute("width")));
        column.setPreferredWidth(column.getWidth());
        column.setModelIndex(Integer
                .parseInt(reader.getAttribute("modelIndex")));
        column.setHeaderValue(reader.getAttribute("headerValue"));
        return column;
    }

}
