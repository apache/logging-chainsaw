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
