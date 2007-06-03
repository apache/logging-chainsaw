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
