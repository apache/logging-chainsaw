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
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A better button class that has nice roll over effects.
 * <p>
 * This class is borrowed (quite heavily, but with modifications)
 * from the "Swing: Second Edition"
 * book by Matthew Robinson and Pavel Vorobeiv. An excellent book on Swing.
 *
 * @author Matthew Robinson
 * @author Pavel Vorobeiv
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class SmallButton extends JButton implements MouseListener {
    protected Border inactiveBorder = new EmptyBorder(3, 3, 3, 3);
    protected Border currentBorder = inactiveBorder;
    protected Border loweredBorder = new SoftBevelBorder(BevelBorder.LOWERED);
    protected Border raisedBorder = new SoftBevelBorder(BevelBorder.RAISED);
    protected Insets insets = new Insets(4, 4, 4, 4);

    public SmallButton() {
        super();
        setBorder(inactiveBorder);
        setMargin(insets);
        setRequestFocusEnabled(false);
        addMouseListener(this);
    }

    public SmallButton(Action act) {
        this();
        setAction(act);
        setRequestFocusEnabled(false);
        addMouseListener(this);
    }

    /**
     * {@inheritDoc}
     * Set to 0.5
     */
    @Override
    public float getAlignmentY() {
        return 0.5f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Border getBorder() {
        return currentBorder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Insets getInsets() {
        return insets;
    }

    /**
     * {@inheritDoc}
     *
     * This component currently ignores click-events and leaves
     * it's implementation to sub-components. Usually, the constructor
     * is called with an action.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (isEnabled()) {
            currentBorder = raisedBorder;
            setBorder(raisedBorder);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
        currentBorder = inactiveBorder;
        setBorder(inactiveBorder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (isEnabled()) {
            currentBorder = loweredBorder;
            setBorder(loweredBorder);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        currentBorder = inactiveBorder;
        setBorder(inactiveBorder);
    }
}
