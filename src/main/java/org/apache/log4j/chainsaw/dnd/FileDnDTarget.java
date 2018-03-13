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
package org.apache.log4j.chainsaw.dnd;


import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides all the functionality to work out when files are dragged onto
 * a particular JComponent instance, and then notifies listeners via
 * the standard PropertyChangesListener semantics to indicate that a list of
 * files have been dropped onto the target.
 * <p>
 * If you wish to know whan the files have been dropped, subscribe to the "fileList" property change.
 *
 * @author psmith
 */
public class FileDnDTarget implements DropTargetListener {
    /**
     * Logger for this class
     */
    private static final Logger LOG = Logger.getLogger(FileDnDTarget.class);

    protected int acceptableActions = DnDConstants.ACTION_COPY;

    private List fileList;

    private JComponent guiTarget;
    private Map<JComponent, DropTarget> dropTargets = new HashMap<>();


    private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    /**
     *
     */
    public FileDnDTarget(JComponent c) {
        this.guiTarget = c;
    }

    public void addDropTargetToComponent(JComponent c) {
        dropTargets.put(c, new DropTarget(c, this));
    }

    /**
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    /**
     * @param propertyName
     * @param listener
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     *
     */
    private void decorateComponent() {
//        TODO work out a better way of decorating a component
        guiTarget.setBorder(BorderFactory.createLineBorder(Color.black));
    }


    public void dragEnter(DropTargetDragEvent e) {
        //LOG.debug(dtde);
        if (isDragOk(e) == false) {
            e.rejectDrag();
            return;
        }
        decorateComponent();

        e.acceptDrag(acceptableActions);
    }


    public void dragExit(DropTargetEvent dte) {
        removeComponentDecoration();
    }


    public void dragOver(DropTargetDragEvent e) {
        //LOG.debug(dtde);

        if (isDragOk(e) == false) {
            e.rejectDrag();
            return;
        }
        e.acceptDrag(acceptableActions);
    }

    public void drop(DropTargetDropEvent dtde) {
        Transferable transferable = dtde.getTransferable();
        LOG.debug(transferable);
        dtde.acceptDrop(acceptableActions);
        try {
            List list = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
            LOG.debug(list);
            setFileList(list);
            dtde.getDropTargetContext().dropComplete(true);
            removeComponentDecoration();

        } catch (Exception e) {
            LOG.error("Error with DnD", e);
        }

    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        //LOG.debug(dtde);
    }

    /**
     * @return Returns the fileList.
     */
    public final List getFileList() {
        return fileList;
    }

    private boolean isDragOk(DropTargetDragEvent e) {
        DataFlavor[] flavors = new DataFlavor[]{DataFlavor.javaFileListFlavor};
        DataFlavor chosen = null;
        for (DataFlavor flavor : flavors) {
            if (e.isDataFlavorSupported(flavor)) {
                chosen = flavor;
                break;
            }
        }
        /*
         * the src does not support any of the StringTransferable flavors
         */
        if (chosen == null) {
            return false;
        }
        // the actions specified when the source
        // created the DragGestureRecognizer
        int sa = e.getSourceActions();

        // we're saying that these actions are necessary
        return (sa & acceptableActions) != 0;
    }

    /**
     *
     */
    private void removeComponentDecoration() {
        this.guiTarget.setBorder(null);
    }

    /**
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    /**
     * @param propertyName
     * @param listener
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * @param fileList The fileList to set.
     */
    private final void setFileList(List fileList) {
        Object oldValue = this.fileList;
        this.fileList = fileList;
        propertySupport.firePropertyChange("fileList", oldValue, this.fileList);
    }
}
