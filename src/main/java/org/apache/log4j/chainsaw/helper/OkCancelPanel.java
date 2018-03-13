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
package org.apache.log4j.chainsaw.helper;

import javax.swing.*;
import java.awt.*;


public class OkCancelPanel extends JPanel {
    private final JButton cancelButton = new JButton(" Cancel ");
    private final JButton okButton = new JButton(" OK ");

    public OkCancelPanel() {
        setLayout(new GridBagLayout());

        cancelButton.setDefaultCapable(true);

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        add(Box.createHorizontalGlue(), c);

        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;

        add(okButton, c);
        add(cancelButton, c);
//    add(Box.createHorizontalStrut(6));
    }

    /**
     * @return Returns the cancelButton.
     */
    public final JButton getCancelButton() {
        return cancelButton;
    }

    /**
     * @return Returns the okButton.
     */
    public final JButton getOkButton() {
        return okButton;
    }

}
