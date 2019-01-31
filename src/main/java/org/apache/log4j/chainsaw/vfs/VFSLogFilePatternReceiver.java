/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.log4j.chainsaw.vfs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.URLFileName;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.commons.vfs.util.RandomAccessMode;
import org.apache.log4j.chainsaw.receivers.VisualReceiver;
import org.apache.log4j.varia.LogFilePatternReceiver;

/**
 * A VFS-enabled version of org.apache.log4j.varia.LogFilePatternReceiver.
 * 
 * VFSLogFilePatternReceiver can parse and tail log files, converting entries into
 * LoggingEvents.  If the file doesn't exist when the receiver is initialized, the
 * receiver will look for the file once every 10 seconds.
 * <p>
 * See the Chainsaw page (http://logging.apache.org/log4j/docs/chainsaw.html) for information
 * on how to set up Chainsaw with VFS.
 * <p>
 * See http://jakarta.apache.org/commons/vfs/filesystems.html for a list of VFS-supported
 * file systems and the URIs needed to access the file systems.
 * <p>
 * Because some VFS file systems allow you to provide username/password, this receiver
 * provides an optional GUI dialog for entering the username/password fields instead 
 * of requiring you to hard code usernames and passwords into the URI.
 * <p>
 * If the 'promptForUserInfo' param is set to true (default is false), 
 * the receiver will wait for a call to 'setContainer', and then display 
 * a username/password dialog.
 * <p>
 * If you are using this receiver without a GUI, don't set promptForUserInfo 
 * to true - it will block indefinitely waiting for a visual component.
 * <p> 
 * If the 'promptForUserInfo' param is set to true, the fileURL should -leave out- 
 * the username/password portion of the VFS-supported URI.  Examples:
 * <p>
 * An sftp URI that would be used with promptForUserInfo=true:
 * sftp://192.168.1.100:22/home/thisuser/logfile.txt
 * <p>
 * An sftp URI that would be used with promptForUserInfo=false:
 * sftp://username:password@192.168.1.100:22/home/thisuser/logfile.txt
 * <p>
 * This receiver relies on java.util.regex features to perform the parsing of text in the 
 * log file, however the only regular expression field explicitly supported is 
 * a glob-style wildcard used to ignore fields in the log file if needed.  All other
 * fields are parsed by using the supplied keywords.
 * <p>
 * <b>Features:</b><br>
 * - specify the URL of the log file to be processed<br>
 * - specify the timestamp format in the file (if one exists, using patterns from {@link java.text.SimpleDateFormat})<br>
 * - specify the pattern (logFormat) used in the log file using keywords, a wildcard character (*) and fixed text<br>
 * - 'tail' the file (allows the contents of the file to be continually read and new events processed)<br>
 * - supports the parsing of multi-line messages and exceptions
 * - to access 
 *<p>
 * <b>Keywords:</b><br>
 * TIMESTAMP<br>
 * LOGGER<br>
 * LEVEL<br>
 * THREAD<br>
 * CLASS<br>
 * FILE<br>
 * LINE<br>
 * METHOD<br>
 * RELATIVETIME<br>
 * MESSAGE<br>
 * NDC<br>
 * PROP(key)<br>
 * <p>
 * Use a * to ignore portions of the log format that should be ignored
 * <p>
 * Example:<br>
 * If your file's patternlayout is this:<br>
 * <b>%d %-5p [%t] %C{2} (%F:%L) - %m%n</b>
 *<p>
 * specify this as the log format:<br>
 * <b>TIMESTAMP LEVEL [THREAD] CLASS (FILE:LINE) - MESSAGE</b>
 *<p>
 * To define a PROPERTY field, use PROP(key)
 * <p>
 * Example:<br> 
 * If you used the RELATIVETIME pattern layout character in the file, 
 * you can use PROP(RELATIVETIME) in the logFormat definition to assign 
 * the RELATIVETIME field as a property on the event.
 * <p>
 * If your file's patternlayout is this:<br>
 * <b>%r [%t] %-5p %c %x - %m%n</b>
 *<p>
 * specify this as the log format:<br>
 * <b>PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE</b>
 * <p>
 * Note the * - it can be used to ignore a single word or sequence of words in the log file
 * (in order for the wildcard to ignore a sequence of words, the text being ignored must be
 *  followed by some delimiter, like '-' or '[') - ndc is being ignored in this example.
 * <p>
 * Assign a filterExpression in order to only process events which match a filter.
 * If a filterExpression is not assigned, all events are processed.
 *<p>
 * <b>Limitations:</b><br>
 * - no support for the single-line version of throwable supported by patternlayout<br>
 *   (this version of throwable will be included as the last line of the message)<br>
 * - the relativetime patternLayout character must be set as a property: PROP(RELATIVETIME)<br>
 * - messages should appear as the last field of the logFormat because the variability in message content<br>
 * - exceptions are converted if the exception stack trace (other than the first line of the exception)<br>
 *   is stored in the log file with a tab followed by the word 'at' as the first characters in the line<br>
 * - tailing may fail if the file rolls over. 
 *<p>
 * <b>Example receiver configuration settings</b> (add these as params, specifying a LogFilePatternReceiver 'plugin'):<br>
 * param: "timestampFormat" value="yyyy-MM-d HH:mm:ss,SSS"<br>
 * param: "logFormat" value="RELATIVETIME [THREAD] LEVEL LOGGER * - MESSAGE"<br>
 * param: "fileURL" value="file:///c:/events.log"<br>
 * param: "tailing" value="true"
 * param: "promptForUserInfo" value="false"
 *<p>
 * This configuration will be able to process these sample events:<br>
 * 710    [       Thread-0] DEBUG                   first.logger first - &lt;test&gt;   &lt;test2&gt;something here&lt;/test2&gt;   &lt;test3 blah=something/&gt;   &lt;test4&gt;       &lt;test5&gt;something else&lt;/test5&gt;   &lt;/test4&gt;&lt;/test&gt;<br>
 * 880    [       Thread-2] DEBUG                   first.logger third - &lt;test&gt;   &lt;test2&gt;something here&lt;/test2&gt;   &lt;test3 blah=something/&gt;   &lt;test4&gt;       &lt;test5&gt;something else&lt;/test5&gt;   &lt;/test4&gt;&lt;/test&gt;<br>
 * 880    [       Thread-0] INFO                    first.logger first - infomsg-0<br>
 * java.lang.Exception: someexception-first<br>
 *     at Generator2.run(Generator2.java:102)<br>
 *
 *@author Scott Deboy
 */
public class VFSLogFilePatternReceiver extends LogFilePatternReceiver implements VisualReceiver {

  private boolean promptForUserInfo = false;
  private Container container;
  private Object waitForContainerLock = new Object();
  private boolean autoReconnect;
  private VFSReader vfsReader;

    public VFSLogFilePatternReceiver() {
    super();
  }

  public void shutdown() {
    getLogger().info("shutdown VFSLogFilePatternReceiver");
    active = false;
	container = null;
    if (vfsReader != null) {
      vfsReader.terminate();
      vfsReader = null;
    }
  }
  
  /**
   * If set to true, will cause the receiver to block indefinitely until 'setContainer' has been called, 
   * at which point a username/password dialog will appear.
   * 
   * @param promptForUserInfo
   */
  public void setPromptForUserInfo(boolean promptForUserInfo) {
	  this.promptForUserInfo = promptForUserInfo;
  }
  
  public boolean isPromptForUserInfo() {
	  return promptForUserInfo;
  }

    /**
     * Accessor
     * @return
     */
    public boolean isAutoReconnect() {
      return autoReconnect;
    }

    /**
     * Mutator
     * @param autoReconnect
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

  /**
   * Implementation of VisualReceiver interface - allows this receiver to provide
   * a username/password dialog.
   */
  public void setContainer(Container container) {
      if (promptForUserInfo) {
    	  synchronized(waitForContainerLock) {
    		  this.container=container;
    		  waitForContainerLock.notify();
    	  }
      }
  }

  /**
   * Read and process the log file.
   */
  public void activateOptions() {
      //we don't want to call super.activateOptions, but we do want active to be set to true
      active = true;
      //on receiver restart, only prompt for credentials if we don't already have them
      if (promptForUserInfo && getFileURL().indexOf("@") == -1) {
    	  /*
    	  if promptforuserinfo is true, wait for a reference to the container
    	  (via the VisualReceiver callback).

    	  We need to display a login dialog on top of the container, so we must then
    	  wait until the container has been added to a frame
    	  */

    	  //get a reference to the container
    	  new Thread(new Runnable() {
    		  public void run() {
    	  synchronized(waitForContainerLock) {
    		  while (container == null) {
    			  try {
    				  waitForContainerLock.wait(1000);
    				  getLogger().debug("waiting for setContainer call");
    			  } catch (InterruptedException ie){}
    		  }
    	  }

    	  Frame containerFrame1;
          if (container instanceof Frame) {
              containerFrame1 = (Frame)container;
          } else {
              synchronized(waitForContainerLock) {
                  //loop until the container has a frame
                  while ((containerFrame1 = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, container)) == null) {
                      try {
                          waitForContainerLock.wait(1000);
                          getLogger().debug("waiting for container's frame to be available");
                      } catch (InterruptedException ie) {}
                  }
              }
          }
            final Frame containerFrame = containerFrame1;
    	  	  //create the dialog
    	  	  SwingUtilities.invokeLater(new Runnable() {
    	  		public void run() {
    	  			  Frame owner = null;
    	  			  if (container != null) {
    	  				  owner = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, containerFrame);
    	  			  }
    	  			  final UserNamePasswordDialog f = new UserNamePasswordDialog(owner);
    	  			  f.pack();
    	  			  Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    	  			  f.setLocation(d.width /2, d.height/2);
    	  			  f.setVisible(true);
    	  				if (null == f.getUserName() || null == f.getPassword()) {
    	  					getLogger().info("Username and password not both provided, not using credentials");
    	  				} else {
    	  				    String oldURL = getFileURL();
    	  					int index = oldURL.indexOf("://");
    	  					String firstPart = oldURL.substring(0, index);
    	  					String lastPart = oldURL.substring(index + "://".length());
    	  					setFileURL(firstPart + "://" + f.getUserName()+ ":" + new String(f.getPassword()) + "@" + lastPart);

    	  			        setHost(oldURL.substring(0, index + "://".length()));
    	  		            setPath(oldURL.substring(index + "://".length()));
    	  				}
                        vfsReader = new VFSReader();
    	  				new Thread(vfsReader).start();
    	  			  }
    	  		  });
    		  }}).start();
      } else {
        //starts with protocol:/  but not protocol://
        String oldURL = getFileURL();
        if (oldURL != null && oldURL.indexOf(":/") > -1 && oldURL.indexOf("://") == -1) {
          int index = oldURL.indexOf(":/");
          String lastPart = oldURL.substring(index + ":/".length());
          int passEndIndex = lastPart.indexOf("@");
          if (passEndIndex > -1) { //we have a username/password
              setHost(oldURL.substring(0, index + ":/".length()));
              setPath(lastPart.substring(passEndIndex + 1));
          }
          vfsReader = new VFSReader();
          new Thread(vfsReader).start();
        } else if (oldURL != null && oldURL.indexOf("://") > -1) {
        //starts with protocol://
            int index = oldURL.indexOf("://");
            String lastPart = oldURL.substring(index + "://".length());
            int passEndIndex = lastPart.indexOf("@");
            if (passEndIndex > -1) { //we have a username/password
                setHost(oldURL.substring(0, index + "://".length()));
                setPath(lastPart.substring(passEndIndex + 1));
            }
            vfsReader = new VFSReader();
            new Thread(vfsReader).start();
        } else {
            getLogger().info("null URL - unable to parse file");
        }
      }
   }

  private class VFSReader implements Runnable {
      private boolean terminated = false;
      private Reader reader;
      private FileObject fileObject;

      private boolean IsGZip(String fileName) {
    	  return fileName.endsWith( ".gz" );
      }
      
      public void run() {
        	//thread should end when we're no longer active
            while (reader == null && !terminated) {
            	int atIndex = getFileURL().indexOf("@");
            	int protocolIndex = getFileURL().indexOf("://");
            	
            	String loggableFileURL = atIndex > -1? getFileURL().substring(0, protocolIndex + "://".length()) + "username:password" + getFileURL().substring(atIndex) : getFileURL();
                getLogger().info("attempting to load file: " + loggableFileURL);
                try {
                    FileSystemManager fileSystemManager = VFS.getManager();
                    FileSystemOptions opts = new FileSystemOptions();
                    //if jsch not in classpath, can get NoClassDefFoundError here
                    try {
                    	SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
                    } catch (NoClassDefFoundError ncdfe) {
                    	getLogger().warn("JSch not on classpath!", ncdfe);
                    }

					synchronized(fileSystemManager) {
                        fileObject = fileSystemManager.resolveFile(getFileURL(), opts);
                        if (fileObject.exists()) {
                            reader = new InputStreamReader(fileObject.getContent().getInputStream() , "UTF-8");
                            //now that we have a reader, remove additional portions of the file url (sftp passwords, etc.)
                            //check to see if the name is a URLFileName..if so, set file name to not include username/pass
                            if (fileObject.getName() instanceof URLFileName) {
                                URLFileName urlFileName = (URLFileName) fileObject.getName();
                                setHost(urlFileName.getHostName());
                                setPath(urlFileName.getPath());
                            }
                        } else {
                            getLogger().info(loggableFileURL + " not available - will re-attempt to load after waiting " + MISSING_FILE_RETRY_MILLIS + " millis");
                        }
                    }
                } catch (FileSystemException fse) {
                    getLogger().info(loggableFileURL + " not available - may be due to incorrect credentials, but will re-attempt to load after waiting " + MISSING_FILE_RETRY_MILLIS + " millis", fse);
                } catch (UnsupportedEncodingException e) {
                    getLogger().info("UTF-8 not available", e);
                }
                if (reader == null) {
                    synchronized (this) {
                        try {
                            wait(MISSING_FILE_RETRY_MILLIS);
                        } catch (InterruptedException ie) {}
                    }
                }
            }
            if (terminated) {
                //shut down while waiting for a file
                return;
            }
            initialize();
            getLogger().debug(getPath() + " exists");
            boolean readingFinished = false;

            do {
                long lastFilePointer = 0;
                long lastFileSize = 0;
                createPattern();
                try {
                    do {
                        FileSystemManager fileSystemManager = VFS.getManager();
                        FileSystemOptions opts = new FileSystemOptions();
                        //if jsch not in classpath, can get NoClassDefFoundError here
                        try {
                            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
                        } catch (NoClassDefFoundError ncdfe) {
                            getLogger().warn("JSch not on classpath!", ncdfe);
                        }

                        //fileobject was created above, release it and construct a new one
                        synchronized(fileSystemManager) {
                            if (fileObject != null) {
                                fileObject.getFileSystem().getFileSystemManager().closeFileSystem(fileObject.getFileSystem());
                                fileObject.close();
                                fileObject = null;
                            }
                        
                        	fileObject = fileSystemManager.resolveFile(getFileURL(), opts);
                        }

                        //file may not exist..
                        boolean fileLarger = false;
                        if (fileObject != null && fileObject.exists()) {
                            try {
                                //available in vfs as of 30 Mar 2006 - will load but not tail if not available
                                fileObject.refresh();
                            } catch (Error err) {
                                getLogger().info(getPath() + " - unable to refresh fileobject", err);
                            }

                            if(IsGZip(getFileURL())) {
                                InputStream gzipStream = new GZIPInputStream(fileObject.getContent().getInputStream());
                                Reader decoder = new InputStreamReader(gzipStream,  "UTF-8");
                                BufferedReader bufferedReader = new BufferedReader(decoder);
                                process(bufferedReader);
                                readingFinished = true;
                            }
                            //could have been truncated or appended to (don't do anything if same size)
                             if (fileObject.getContent().getSize() < lastFileSize) {
                                reader = new InputStreamReader(fileObject.getContent().getInputStream(), "UTF-8");
                                getLogger().debug(getPath() + " was truncated");
                                lastFileSize = 0; //seek to beginning of file
                                lastFilePointer = 0;
                            } else if (fileObject.getContent().getSize() > lastFileSize) {
                                fileLarger = true;
                                RandomAccessContent rac = fileObject.getContent().getRandomAccessContent(RandomAccessMode.READ);
                                rac.seek(lastFilePointer);
                                reader = new InputStreamReader(rac.getInputStream(), "UTF-8");
                                BufferedReader bufferedReader = new BufferedReader(reader);
                                process(bufferedReader);
                                lastFilePointer = rac.getFilePointer();
                                lastFileSize = fileObject.getContent().getSize();
                                rac.close();
                            }
                            try {
                                if (reader != null) {
                                    reader.close();
                                    reader = null;
                                }
                            } catch (IOException ioe) {
                                getLogger().debug(getPath() + " - unable to close reader", ioe);
                            }
                        } else {
                            getLogger().info(getPath() + " - not available - will re-attempt to load after waiting " + getWaitMillis() + " millis");
                        }

                        try {
                            synchronized (this) {
                                wait(getWaitMillis());
                            }
                        } catch (InterruptedException ie) {}
                        if (isTailing() && fileLarger && !terminated) {
                            getLogger().debug(getPath() + " - tailing file - file size: " + lastFileSize);
                        }
                    } while (isTailing() && !terminated && !readingFinished);
                } catch (IOException ioe) {
                    getLogger().info(getPath() + " - exception processing file", ioe);
                    try {
                        if (fileObject != null) {
                            fileObject.close();
                        }
                    } catch (FileSystemException e) {
                        getLogger().info(getPath() + " - exception processing file", e);
                    }
                    try {
                        synchronized(this) {
                            wait(getWaitMillis());
                        }
                    } catch (InterruptedException ie) {}
                }
            } while (isAutoReconnect() && !terminated && !readingFinished);
            getLogger().debug(getPath() + " - processing complete");
        }

      public void terminate()
      {
          terminated = true;
      }
  }
  
  public class UserNamePasswordDialog extends JDialog {
	  private String userName;
	  private char[] password;
	  private UserNamePasswordDialog(Frame containerFrame) {
		  super(containerFrame, "Login", true);
	      JPanel panel = new JPanel(new GridBagLayout());
	      GridBagConstraints gc = new GridBagConstraints();
	      gc.fill=GridBagConstraints.NONE;

	      gc.anchor=GridBagConstraints.NORTH;
	      gc.gridx=0;
	      gc.gridy=0;
	      gc.gridwidth=3;
	      gc.insets=new Insets(7, 7, 7, 7);
	      panel.add(new JLabel("URI: " + getFileURL()), gc);
	      
	      gc.gridx=0;
	      gc.gridy=1;
	      gc.gridwidth=1;
	      gc.insets=new Insets(2, 2, 2, 2);
	      panel.add(new JLabel("Username"), gc);

		  gc.gridx=1;
		  gc.gridy=1;
		  gc.gridwidth=2;
		  gc.weightx=1.0;
	      gc.fill=GridBagConstraints.HORIZONTAL;

		  final JTextField userNameTextField = new JTextField(15);
		  panel.add(userNameTextField, gc);
		  
		  gc.gridx=0;
		  gc.gridy=2;
		  gc.gridwidth=1;
	      gc.fill=GridBagConstraints.NONE;

		  panel.add(new JLabel("Password"), gc);

		  gc.gridx=1;
		  gc.gridy=2;
		  gc.gridwidth=2;
	      gc.fill=GridBagConstraints.HORIZONTAL;

		  final JPasswordField passwordTextField = new JPasswordField(15);
		  panel.add(passwordTextField, gc);
		  
		  gc.gridy=3;
		  gc.anchor=GridBagConstraints.SOUTH;
	      gc.fill=GridBagConstraints.NONE;

		  JButton submitButton = new JButton(" Submit ");
		  panel.add(submitButton, gc);

		  getContentPane().add(panel);
		  submitButton.addActionListener(new ActionListener(){
			  public void actionPerformed(ActionEvent evt) {
				  userName = userNameTextField.getText();
				  password = passwordTextField.getPassword();
				  getContentPane().setVisible(false);
				  UserNamePasswordDialog.this.dispose();
			  }
		  });
	  }
	 
	  public String getUserName() {
		  return userName;
	  }
	  
	  public char[] getPassword() {
		  return password;
	  }
  }
}
