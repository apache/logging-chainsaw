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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

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

    public Action getAction() {
        return super.getAction();
    }

    public Object getActionName() {
        return getAction().getValue(Action.NAME);
    }

    public KeyStroke getActionAcceleratorKey() {
        return (KeyStroke)getAction().getValue(Action.ACCELERATOR_KEY);
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

    static public class Builder {
        Runnable action;
        String name = "";

        String text = "";

        boolean enabled = true;
        Icon icon;
        URL iconUrl;

        URL smallIconUrl;

        String shortDescription;

        private KeyStroke keyStroke;

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder enabled() {
            this.enabled = true;
            return this;
        }
        public Builder disabled() {
            this.enabled = false;
            return this;
        }
        public Builder icon(Icon icon) {
            this.icon = icon;
            return this;
        }
        public Builder iconUrl(URL iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }
        public Builder smallIconUrl(URL smallIconUrl) {
            this.smallIconUrl = smallIconUrl;
            return this;
        }
        public Builder shortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
            return this;
        }

        public Builder keyStroke(KeyStroke keyStroke) {
            this.keyStroke = keyStroke;
            return this;
        }

        public SmallButton build() {
            SmallButton button = new SmallButton();
            if (action == null) {
                throw new NullPointerException("Action must not be null for SmallButton");
            }
            button.setAction(new AbstractAction(name, icon) {
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });

            if (text != null) {
                button.setText(text);
            } else if (name != null) {
                button.setText(name);
            }

            if (keyStroke != null) {
                button.getAction().putValue(Action.ACCELERATOR_KEY, keyStroke);
            }
            if (icon != null) {
                button.setIcon(icon);
            } else  if (iconUrl != null) {
                button.setIcon(new ImageIcon(iconUrl));
            }
            if (name != null) {
                button.getAction().putValue(Action.NAME, name);
            }
            if (smallIconUrl != null) {
                button.getAction().putValue(Action.SMALL_ICON, new ImageIcon(smallIconUrl));
            }
            if (shortDescription != null) {
                button.getAction().putValue(Action.SHORT_DESCRIPTION, shortDescription);
            }
            button.setEnabled(enabled);
            return button;
        }
    }
}
