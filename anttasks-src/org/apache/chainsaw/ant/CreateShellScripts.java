package org.apache.chainsaw.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * This Ant task creates the necessary .bat and .sh files required to run Chainsaw
 * on the commandline.  This task was created because it was getting VERY annoying
 * to constantly maintain 2 script files as the Jar file names kept changing during
 * the log4j 1.3 alpha stage, and as we (the Chainsaw developers) added dependencies
 * as we added functionality.
 * 
 * This task takes and outputLocation as a , and a set of FileSets which form the
 * basis of the classpath that is required to run Chainsaw.
 * 
 * @author psmith
 *
 */
public class CreateShellScripts extends Task {


    private String outputLocation;

    private Vector fileSets = new Vector();

    public String getOutputLocation() {
        return outputLocation;
    }

    public void setOutputLocation(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    public void execute() throws BuildException {
        super.execute();
        File outputLocationDir = new File(getOutputLocation());
        if(!outputLocationDir.exists()) {
            log("Creating director(ies) ->" + getOutputLocation());
            outputLocationDir.mkdirs();
        }
        Collection filenames = getFilenames();
        try {
            createUnixShellScript(outputLocationDir, filenames);
            createBatShellScript(outputLocationDir, filenames);
        } catch (Exception e) {
            throw new BuildException("Failed to create Scripts",e);
        }        
    }

    public void addFileSet(FileSet fileSet) {
        fileSets.add(fileSet);
    }

    private void createBatShellScript(File outputLocationDir, Collection filenames) throws IOException {
        File unixScript = new File(outputLocationDir, "chainsaw.bat");
        
        log("Creating Windows .bat script: " + unixScript.getAbsolutePath());
        
        Writer writer = new FileWriter(unixScript);
        writeExecutionLine(filenames, writer, ';');
        writer.close();
    }

    private void createUnixShellScript(File outputLocationDir, Collection fileNames) throws IOException {
        File unixScript = new File(outputLocationDir, "chainsaw.sh");
        
        log("Creating Unix script: " + unixScript.getAbsolutePath());
        
        Writer writer = new FileWriter(unixScript);
        writer.write("#!/bin/sh\n");
        writeExecutionLine(fileNames, writer, ':');
        writer.close();
    }

    
    private void writeExecutionLine(Collection fileNames, Writer writer, char jarSeparator) throws IOException {
        writer.write("java -classpath ");
        for (Iterator iter = fileNames.iterator(); iter.hasNext();) {
            String fileName = (String) iter.next();
            writer.write(fileName);
            if(iter.hasNext()) {
                writer.write(jarSeparator);
            }
        }
        writer.write(" org.apache.log4j.chainsaw.LogUI" );
        writer.write("\n");
    }

    private Collection getFilenames() {
        List jars = new ArrayList();
        for (Iterator iter = fileSets.iterator(); iter.hasNext();) {
            FileSet fileSet = (FileSet) iter.next();
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject()); // 3
            String[] includedFiles = ds.getIncludedFiles();
            for (int i = 0; i < includedFiles.length; i++) {
                String filename = includedFiles[i].replace('\\', '/'); // 4
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                // if (foundLocation==null && file.equals(filename)) {
                // File base = ds.getBasedir(); // 5
                // File found = new File(base, includedFiles[i]);
                // foundLocation = found.getAbsolutePath();
                // }
                jars.add(filename);
            }
        }
        return jars;
    }

}
