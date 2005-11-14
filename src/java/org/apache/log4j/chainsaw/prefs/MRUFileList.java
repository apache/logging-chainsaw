package org.apache.log4j.chainsaw.prefs;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class MRUFileList{

    private static MRUFileList log4jList = new MRUFileList();
    private static MRUFileList jdk14List = new MRUFileList();
    private static final int DEFAULT_MRU_SIZE = 5;
    
    private List fileList = new ArrayList();
    private int size = DEFAULT_MRU_SIZE;
    
    private static transient EventListenerList listeners = new EventListenerList();
    
    private MRUFileList() {
        
    }
    
    public static void addChangeListener(ChangeListener listener){
        listeners.add(ChangeListener.class, listener);
    }
    public static void removeChangeListener(ChangeListener listener) {
        listeners.remove(ChangeListener.class, listener);
    }
    
    /**
     * Call this method when something opens a log file, this method
     * adds the URL to the list of known URL's, automatically 
     * rolling the list to ensure the list maintains the
     * size property
     * 
     * 
     * @param url
     */
    public void opened(URL url) {
        // first remove any existence of the URL already, make sure we don't have dupes
        fileList.remove(url);
        // now make sure we obey the size property,  leaving room for 1 more to be added at the front
        while(fileList.size()>=size) {
            fileList.remove(fileList.size()-1);
        }
        fileList.add(0, url);
        fireChangeEvent();
    }
    
    private static void fireChangeEvent() {
        
        ChangeEvent event = null;
        EventListener[] eventListeners = listeners.getListeners(ChangeListener.class);
        for (int i = 0; i < eventListeners.length; i++) {
            ChangeListener listener = (ChangeListener) eventListeners[i];
            if(event==null) {
                event = new ChangeEvent(MRUFileList.class);
            }
            listener.stateChanged(event);
        }
    }

    /**
     * Returns an <b>unmodifiable</b> List of the MRU opened file list within Chainsaw
     * 
     * @return
     */
    public List getMRUList() {
        return Collections.unmodifiableList(fileList);
    }

    public static MRUFileList log4jMRU() {
        return log4jList;
    }
    
    public static void loadLog4jMRUListFromXML(String xml) {
        XStream xstream = new XStream(new DomDriver());
        log4jList = (MRUFileList) xstream.fromXML(xml);
        fireChangeEvent();
    }
    public static void loadLog4jMRUListFromReader(Reader reader) {
        XStream xstream = new XStream(new DomDriver());
        log4jList = (MRUFileList) xstream.fromXML(reader);
        fireChangeEvent();
        
    }
        // TODO Auto-generated method stub
}
