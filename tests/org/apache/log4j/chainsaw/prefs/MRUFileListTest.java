package org.apache.log4j.chainsaw.prefs;

import java.net.MalformedURLException;
import java.net.URL;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import junit.framework.TestCase;

public class MRUFileListTest extends TestCase {
    private static final String[] urls = new String[] {
            "file://foo.bar.txt",
            "http://www.cnn.com",
            "file://uber.log",
            "http://logging.apache.org/",
            "https://something.com",
            "file:///usr/local/tomcat/logs/catalina.out",
            
    };
    
    public static final String EXAMPLE_XML = "<org.apache.log4j.chainsaw.prefs.MRUFileList>\n" + 
            "  <fileList>\n" + 
            "    <url>file:/usr/local/tomcat/logs/catalina.out</url>\n" + 
            "    <url>https://something.com</url>\n" + 
            "    <url>http://logging.apache.org/</url>\n" + 
            "    <url>file://uber.log</url>\n" + 
            "    <url>http://www.cnn.com</url>\n" + 
            "  </fileList>\n" + 
            "  <size>5</size>\n" + 
            "</org.apache.log4j.chainsaw.prefs.MRUFileList>";
    
    MRUFileList fl =  MRUFileList.log4jMRU();
    
    
    public void testMRUFileList() throws Exception {
        
        assertEquals(fl.getMRUList().size(), 0);
        
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            fl.opened(new URL(url));
        }
        
        assertEquals(5, fl.getMRUList().size());
        assertEquals(new URL(urls[5]), fl.getMRUList().get(0));
        
        assertTrue(!fl.getMRUList().contains(new URL(urls[0])));
        
    }
    
    public void testSerialization() {
        XStream xstream = new XStream(new DomDriver());
        String string = xstream.toXML(fl);
        System.out.println("toXML:");
        System.out.println(string);
        
        MRUFileList newFL =   (MRUFileList) xstream.fromXML(string);
        assertEquals(5, newFL.getMRUList().size());
        
        System.out.println("After object->xml->object:");
        String string2 = xstream.toXML(newFL);
        System.out.println(string2);
        assertEquals(string, string2);
        
        
    }

}
