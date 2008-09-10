/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.apache.log4j.chainsaw;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class CopyEventsToClipboardAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final int EVENTSIZE_FUDGE_FACTOR = 128; // guestimate 128 chars per event
    private final LogUI logUi;

    /**
     * Layout pattern uses a simple but concise format that reads well and has a fixed size set of
     * useful columns before the message. Nice format for pasting into issue trackers.
     */
    private final Layout layout = new EnhancedPatternLayout(
            "[%d{ISO8601} %-5p][%20.20c][%t] %m%n");

    public CopyEventsToClipboardAction(LogUI parent) {
        super("Copy events to clipboard");
        this.logUi = parent;
        layout.activateOptions();
        
        putValue(Action.SHORT_DESCRIPTION,
                "Copies to the clipboard currently visible events to a human-readable, log-like format");

    }
    
    
    public void actionPerformed(ActionEvent e) {
        List filteredEvents = logUi.getCurrentLogPanel().getFilteredEvents();
        StringBuffer writer = new StringBuffer(filteredEvents.size() * EVENTSIZE_FUDGE_FACTOR);
        for (Iterator iterator = filteredEvents.iterator(); iterator.hasNext();) {
            LoggingEvent event = (LoggingEvent) iterator.next();
            writer.append(layout.format(event));
        }

        StringSelection stringSelection = new StringSelection(writer.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection,
                stringSelection);
    }

}
