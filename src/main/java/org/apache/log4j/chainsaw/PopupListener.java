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

package org.apache.log4j.chainsaw;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Ensures that a specific popupMenu is displayed when the relevant
 * mouse events are trapped.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class PopupListener extends MouseAdapter {
    final JPopupMenu popupMenu;

    public PopupListener(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    public void mousePressed(MouseEvent e) {
        checkPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        checkPopup(e);
    }

    public void mouseClicked(MouseEvent e) {
        checkPopup(e);
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
