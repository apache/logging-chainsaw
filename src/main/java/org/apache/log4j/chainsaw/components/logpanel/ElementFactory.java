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
package org.apache.log4j.chainsaw.components.logpanel;

import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ElementFactory {
    public static SmallButton createFindNextButton(Runnable action) {
        SmallButton button = new SmallButton.Builder()
            .action(action)
            .name("Find next")
            .text("")
            .smallIconUrl(ChainsawIcons.DOWN)
            .shortDescription("Find the next occurrence of the rule from the current row")
            .keyStroke(KeyStroke.getKeyStroke("F3"))
            .build();

        button.getActionMap().put(button.getActionName(), button.getAction());
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(button.getActionAcceleratorKey(), button.getActionName());
        return button;
    }

    public static SmallButton createFindPreviousButton(Runnable action) {
        SmallButton button = new SmallButton.Builder()
            .action(action)
            .name("Find previous")
            .text("")
            .smallIconUrl(ChainsawIcons.UP)
            .shortDescription("Find the previous occurrence of the rule from the current row")
            .keyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK))
            .build();

        button.getActionMap().put(button.getActionName(), button.getAction());
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(button.getActionAcceleratorKey(), button.getActionName());
        return button;
    }
}
