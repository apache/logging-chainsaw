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

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.List;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logui.LogUI;

public class CopyEventsToClipboardAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final int EVENTSIZE_FUDGE_FACTOR = 128; // guestimate 128 chars per event
    private final LogUI logUi;

    /**
     * Layout pattern uses a simple but concise format that reads well and has a fixed size set of
     * useful columns before the message. Nice format for pasting into issue trackers.
     */
//    private final Layout layout = new EnhancedPatternLayout(
//        "[%d{ISO8601} %-5p][%20.20c][%t] %m%n");

    public CopyEventsToClipboardAction(LogUI parent) {
        super("Copy events to clipboard");
        this.logUi = parent;

        putValue(Action.SHORT_DESCRIPTION,
            "Copies to the clipboard currently visible events to a human-readable, log-like format");

    }


    public void actionPerformed(ActionEvent e) {
        List filteredEvents = logUi.getCurrentLogPanel().getFilteredEvents();
        StringBuilder writer = new StringBuilder(filteredEvents.size() * EVENTSIZE_FUDGE_FACTOR);
        for (Object filteredEvent : filteredEvents) {
            ChainsawLoggingEvent event = ((LoggingEventWrapper) filteredEvent).getLoggingEvent();
            writer.append(event.m_message);
        }

        StringSelection stringSelection = new StringSelection(writer.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection,
            stringSelection);
    }

}
