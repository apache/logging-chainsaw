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

package org.apache.log4j.chainsaw.layout;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.logging.log4j.Level;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.JTextComponentFormatter;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;


/**
 * An editor Pane that allows a user to Edit a Pattern Layout and preview the output it would
 * generate with an example LogEvent
 * 
 * @author Paul Smith &lt;psmith@apache.org&gt;
 *
 */
public final class LayoutEditorPane extends JPanel {
  private final Action copyAction;
  private final Action cutAction;
  private final JToolBar editorToolbar = new JToolBar();
  private final JToolBar okCancelToolbar = new JToolBar();
  private final JButton okButton = new JButton(" OK ");
  private final JButton cancelButton = new JButton(" Cancel ");

  //  private final JButton applyButton = new JButton();
  private final JEditorPane patternEditor = new JEditorPane("text/plain", "");
  private final JEditorPane previewer =
    new JEditorPane(ChainsawConstants.DETAIL_CONTENT_TYPE, "");
  private final JScrollPane patternEditorScroll =
    new JScrollPane(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
  private final JScrollPane previewEditorScroll =
    new JScrollPane(
    		ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
    		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
  private LogEvent event;
  private EventDetailLayout layout = new EventDetailLayout();

  /**
   *
   */
  public LayoutEditorPane() {
    super();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    createEvent();
    copyAction = createCopyAction();
    cutAction = createCutAction();
    initComponents();
    setupListeners();
  }

  private Action createCutAction() {
    final Action action =
      new AbstractAction("Cut", ChainsawIcons.ICON_CUT) {
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
        }
      };

    action.setEnabled(false);

    return action;
  }

  private Action createCopyAction() {
    final Action action =
      new AbstractAction("Copy", ChainsawIcons.ICON_COPY) {
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
        }
      };

    action.setEnabled(false);

    return action;
  }

  private void setupListeners() {
    patternEditor.getDocument().addDocumentListener(
      new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
          updatePreview();
        }

        public void insertUpdate(DocumentEvent e) {
          updatePreview();
        }

        public void removeUpdate(DocumentEvent e) {
          updatePreview();
        }
      });

    patternEditor.addCaretListener(
      new CaretListener() {
        public void caretUpdate(CaretEvent e) {
          updateTextActions(e.getMark() != e.getDot());
        }
      });
  }

  private void updatePreview() {
    String pattern = patternEditor.getText();
    layout.setConversionPattern(pattern);

    previewer.setText(layout.format(event));
  }

  /**
  *
  */
  private void updateTextActions(boolean enabled) {
    cutAction.setEnabled(enabled);
    copyAction.setEnabled(enabled);
  }

  /**
      *
      */
  private void createEvent() {
    Map<String, String> contextMap = new HashMap<>(3);
    contextMap.put("key1", "val1");
    contextMap.put("key2", "val2");
    contextMap.put("key3", "val3");

    StackTraceElement li =
      new StackTraceElement(
        "myfile.java", "com.mycompany.util.MyClass", "myMethod", 321);

    ThrowableProxy tsr = new ThrowableProxy(new Exception());

    event = Log4jLogEvent.newBuilder()
                .setLoggerFqcn("org.apache.log4j.Logger")
                .setLoggerName("com.mycompany.mylogger")
                .setTimeMillis(new Date().getTime())
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage("The quick brown fox jumped over the lazy dog"))
                .setThreadName("Thread-1")
                .setThrownProxy(tsr)
                .setContextStack(new MutableThreadContextStack(Arrays.asList("NDC", "string")))
                .setContextMap(contextMap)
                .setSource(li)
                .build();
  }

  /**
     *
     */
  private void initComponents() {
    editorToolbar.setFloatable(false);
    okCancelToolbar.setFloatable(false);
    okButton.setToolTipText("Accepts the current Pattern layout and will apply it to the Log Panel");
    cancelButton.setToolTipText("Closes this dialog and discards your changes");

    JTextComponentFormatter.applySystemFontAndSize(previewer);
      
    previewer.setEditable(false);
    patternEditor.setPreferredSize(new Dimension(240, 240));
    patternEditor.setMaximumSize(new Dimension(320, 240));
    previewer.setPreferredSize(new Dimension(360, 240));
    patternEditorScroll.setViewportView(patternEditor);
    previewEditorScroll.setViewportView(previewer);

    patternEditor.setToolTipText("Edit the Pattern here");
    previewer.setToolTipText(
      "The result of the layout of the pattern is shown here");

    patternEditorScroll.setBorder(
      BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Pattern Editor"));
    previewEditorScroll.setBorder(
      BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Pattern Preview"));

//    editorToolbar.add(new JButton(copyAction));
//    editorToolbar.add(new JButton(cutAction));

    editorToolbar.add(Box.createHorizontalGlue());

    okCancelToolbar.add(Box.createHorizontalGlue());
    okCancelToolbar.add(okButton);
    okCancelToolbar.addSeparator();
    okCancelToolbar.add(cancelButton);

    //    okCancelToolbar.addSeparator();
    //    okCancelToolbar.add(applyButton);
    add(editorToolbar);
    add(patternEditorScroll);
    add(previewEditorScroll);
    add(okCancelToolbar);
  }

  public void setConversionPattern(String pattern) {
    patternEditor.setText(pattern);
  }

  public String getConversionPattern() {
    return patternEditor.getText();
  }

  public void addOkActionListener(ActionListener l) {
    okButton.addActionListener(l);
  }

  public void addCancelActionListener(ActionListener l) {
    cancelButton.addActionListener(l);
  }

  public static void main(String[] args) {
    JDialog dialog = new JDialog((Frame) null, "Pattern Editor");
    dialog.getContentPane().add(new LayoutEditorPane());
    dialog.setResizable(true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    //    dialog.pack();
    dialog.setSize(new Dimension(640, 480));
    dialog.setVisible(true);
  }
}
