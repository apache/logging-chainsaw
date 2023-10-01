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
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
public class SmallToggleButton extends JToggleButton implements ItemListener,
    MouseListener {
    protected Border m_highlighted = new SoftBevelBorder(BevelBorder.RAISED);
    protected Border m_raised = BorderFactory.createEmptyBorder(3, 3, 3, 3);
    protected Border m_inactive = m_raised;
    protected Border m_border = m_inactive;
    protected Border m_lowered = new SoftBevelBorder(BevelBorder.LOWERED);
    protected Insets m_ins = new Insets(4, 4, 4, 4);

    public SmallToggleButton() {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(isSelected() ? m_lowered : m_raised);
        setMargin(m_ins);
        setRequestFocusEnabled(false);
        addItemListener(this);
        addMouseListener(this);
        setText("");
    }

    public SmallToggleButton(Action action) {
        this();
        setAction(action);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public float getAlignmentY() {
        return 0.5f;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Border getBorder() {
        return m_border;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Insets getInsets() {
        return m_ins;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void itemStateChanged(ItemEvent e) {
        setBorder(isSelected() ? m_lowered : m_raised);
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseEntered(MouseEvent e) {
        if (isEnabled()) {
            m_border = m_highlighted;
            setBorder(m_highlighted);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseExited(MouseEvent e) {
        m_border = m_inactive;
        setBorder(m_inactive);
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mousePressed(MouseEvent e) {
        m_border = m_lowered;
        setBorder(m_lowered);
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseReleased(MouseEvent e) {
        m_border = m_inactive;
        setBorder(m_inactive);
    }
}
