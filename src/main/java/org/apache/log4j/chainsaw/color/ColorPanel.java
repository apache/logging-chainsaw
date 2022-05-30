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

package org.apache.log4j.chainsaw.color;

import org.apache.log4j.chainsaw.ApplicationPreferenceModel;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.ExpressionRuleContext;
import org.apache.log4j.chainsaw.filter.FilterModel;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Panel which updates a RuleColorizer, allowing the user to build expression-based
 * color rules.
 * <p>
 * TODO: examine ColorPanel/RuleColorizer/LogPanel listeners and interactions
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class ColorPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();
    private static final String DEFAULT_STATUS = "<html>Double click a rule field to edit the rule</html>";
    private final String currentRuleSet = "Default";

    private RuleColorizer colorizer;
    private JPanel rulesPanel;
    private FilterModel filterModel;
    private RulesTableModel rulesTableModel;
    private JScrollPane tableScrollPane;
    private JTable rulesTable;
    private ActionListener closeListener;
    private JLabel statusBar;
    private Vector<String> columns;
    private final String noTab = "None";
    private DefaultComboBoxModel logPanelColorizersModel;
    private Map<String, RuleColorizer> allLogPanelColorizers;
    private RuleColorizer currentLogPanelColorizer;
    private JTable searchTable;
    private DefaultTableModel searchTableModel;
    private Vector<String> searchColumns;
    private Vector<Vector<Color>> searchDataVector;
    private Vector<Color> searchDataVectorEntry;

    private JTable alternatingColorTable;
    private DefaultTableModel alternatingColorTableModel;
    private Vector<String> alternatingColorColumns;
    private Vector<Vector<Color>> alternatingColorDataVector;
    private Vector<Color> alternatingColorDataVectorEntry;
    private ApplicationPreferenceModel applicationPreferenceModel;
    private JCheckBox bypassSearchColorsCheckBox;

    public ColorPanel(final RuleColorizer currentLogPanelColorizer, final FilterModel filterModel,
                      final Map<String, RuleColorizer> allLogPanelColorizers, final ApplicationPreferenceModel applicationPreferenceModel) {
        super(new BorderLayout());

        this.currentLogPanelColorizer = currentLogPanelColorizer;
        this.colorizer = currentLogPanelColorizer;
        this.filterModel = filterModel;
        this.allLogPanelColorizers = allLogPanelColorizers;
        this.applicationPreferenceModel = applicationPreferenceModel;

        currentLogPanelColorizer.addPropertyChangeListener(
            "colorrule",
            evt -> updateColors());

        rulesTableModel = new RulesTableModel();
        rulesTable = new JTable(rulesTableModel);
        rulesTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        rulesTable.getColumnModel().getColumn(0)
                .setCellEditor(new RulesTableCellEditor());

        searchTableModel = new DefaultTableModel();
        searchTable = new JTable(searchTableModel);
        searchTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        searchTable.setPreferredScrollableViewportSize(new Dimension(30, 30));

        alternatingColorTableModel = new DefaultTableModel();
        alternatingColorTable = new JTable(alternatingColorTableModel);
        alternatingColorTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        alternatingColorTable.setPreferredScrollableViewportSize(new Dimension(30, 30));

        columns = new Vector<>();
        columns.add("Expression");
        columns.add("Background");
        columns.add("Foreground");

        searchColumns = new Vector<>();
        searchColumns.add("Background");
        searchColumns.add("Foreground");

        alternatingColorColumns = new Vector<>();
        alternatingColorColumns.add("Background");
        alternatingColorColumns.add("Foreground");

        //searchtable contains only a single-entry vector containing a two-item vector (foreground, background)
        searchDataVector = new Vector<>();
        searchDataVectorEntry = new Vector<>();
        searchDataVectorEntry.add(applicationPreferenceModel.getSearchBackgroundColor());
        searchDataVectorEntry.add(applicationPreferenceModel.getSearchForegroundColor());
        searchDataVector.add(searchDataVectorEntry);
        searchTableModel.setDataVector(searchDataVector, searchColumns);

        alternatingColorDataVector = new Vector<>();
        alternatingColorDataVectorEntry = new Vector<>();
        alternatingColorDataVectorEntry.add(applicationPreferenceModel.getAlternatingColorBackgroundColor());
        alternatingColorDataVectorEntry.add(applicationPreferenceModel.getAlternatingColorForegroundColor());
        alternatingColorDataVector.add(alternatingColorDataVectorEntry);
        alternatingColorTableModel.setDataVector(alternatingColorDataVector, alternatingColorColumns);

        rulesTable.setPreferredScrollableViewportSize(new Dimension(525, 200));
        tableScrollPane = new JScrollPane(rulesTable);

        rulesTable.sizeColumnsToFit(0);
        rulesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        rulesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        rulesTable.getColumnModel().getColumn(1).setMaxWidth(80);
        rulesTable.getColumnModel().getColumn(2).setMaxWidth(80);

        searchTable.sizeColumnsToFit(0);
        searchTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        searchTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        searchTable.getColumnModel().getColumn(0).setMaxWidth(80);
        searchTable.getColumnModel().getColumn(1).setMaxWidth(80);
        //building color choosers needs to be done on the EDT
        SwingHelper.invokeOnEDT(() -> configureSingleEntryColorTable(searchTable));

        alternatingColorTable.sizeColumnsToFit(0);
        alternatingColorTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        alternatingColorTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        alternatingColorTable.getColumnModel().getColumn(0).setMaxWidth(80);
        alternatingColorTable.getColumnModel().getColumn(1).setMaxWidth(80);
        //building color choosers needs to be done on the EDT
        SwingHelper.invokeOnEDT(() -> configureSingleEntryColorTable(alternatingColorTable));

        configureTable();

        statusBar = new JLabel(DEFAULT_STATUS);

        rulesPanel = buildRulesPanel();
        rulesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rightPanel = new JPanel(new BorderLayout());

        JPanel rightOuterPanel = new JPanel();
        rightOuterPanel.setLayout(
            new BoxLayout(rightOuterPanel, BoxLayout.X_AXIS));
        rightOuterPanel.add(Box.createHorizontalStrut(10));

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(Box.createVerticalStrut(5));
        southPanel.add(Box.createVerticalStrut(5));
        JPanel searchAndAlternatingColorPanel = buildSearchAndAlternatingColorPanel();
        JPanel bypassSearchColorsPanel = buildBypassSearchColorsPanel();
        bypassSearchColorsCheckBox.setSelected(applicationPreferenceModel.isBypassSearchColors());

        JPanel globalLabelPanel = new JPanel();
        globalLabelPanel.setLayout(new BoxLayout(globalLabelPanel, BoxLayout.X_AXIS));
        JLabel globalLabel = new JLabel("Global colors:");
        globalLabelPanel.add(globalLabel);
        globalLabelPanel.add(Box.createHorizontalGlue());
        southPanel.add(globalLabelPanel);
        southPanel.add(searchAndAlternatingColorPanel);
        southPanel.add(bypassSearchColorsPanel);
        southPanel.add(Box.createVerticalStrut(5));
        JPanel closePanel = buildClosePanel();
        southPanel.add(closePanel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusBar);
        southPanel.add(statusPanel);
        rightPanel.add(rulesPanel, BorderLayout.CENTER);
        rightPanel.add(southPanel, BorderLayout.SOUTH);
        rightOuterPanel.add(rightPanel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

        JLabel selectText = new JLabel("Apply a tab's colors");
        topPanel.add(selectText);
        topPanel.add(Box.createHorizontalStrut(5));

        logPanelColorizersModel = new DefaultComboBoxModel();
        final JComboBox loadPanelColorizersComboBox = new JComboBox(logPanelColorizersModel);
        loadLogPanelColorizers();

        topPanel.add(loadPanelColorizersComboBox);

        topPanel.add(Box.createHorizontalStrut(5));
        final Action copyRulesAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rulesTableModel.clear();
                Object selectedItem = loadPanelColorizersComboBox.getSelectedItem();
                if (selectedItem != null) {
                    RuleColorizer sourceColorizer = allLogPanelColorizers.get(selectedItem.toString());
                    colorizer.setRules(sourceColorizer.getRules());
                    updateColors();
                }
            }
        };

        loadPanelColorizersComboBox.addActionListener(e -> {
            Object selectedItem = loadPanelColorizersComboBox.getSelectedItem();
            if (selectedItem != null) {
                String selectedColorizerName = selectedItem.toString();
                copyRulesAction.setEnabled(!(noTab.equals(selectedColorizerName)));
            }
        });

        copyRulesAction.putValue(Action.NAME, "Copy color rules");
        copyRulesAction.setEnabled(!(noTab.equals(loadPanelColorizersComboBox.getSelectedItem())));

        JButton copyRulesButton = new JButton(copyRulesAction);
        topPanel.add(copyRulesButton);

        add(topPanel, BorderLayout.NORTH);
        add(rightOuterPanel, BorderLayout.CENTER);
        if (rulesTable.getRowCount() > 0) {
            rulesTable.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    public void loadLogPanelColorizers() {
        if (logPanelColorizersModel.getIndexOf(noTab) == -1) {
            logPanelColorizersModel.addElement(noTab);
        }
        for (Object o : allLogPanelColorizers.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (!entry.getValue().equals(currentLogPanelColorizer) && (logPanelColorizersModel.getIndexOf(entry.getKey()) == -1)) {
                logPanelColorizersModel.addElement(entry.getKey());
            }
        }
        //update search and alternating colors, since they may have changed from another color panel
        searchDataVectorEntry.set(0, applicationPreferenceModel.getSearchBackgroundColor());
        searchDataVectorEntry.set(1, applicationPreferenceModel.getSearchForegroundColor());
        alternatingColorDataVectorEntry.set(0, applicationPreferenceModel.getAlternatingColorBackgroundColor());
        alternatingColorDataVectorEntry.set(1, applicationPreferenceModel.getAlternatingColorForegroundColor());
    }

    public JPanel buildBypassSearchColorsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        bypassSearchColorsCheckBox = new JCheckBox("Don't use a search color for matching rows");
        panel.add(bypassSearchColorsCheckBox);
        return panel;
    }

    public JPanel buildSearchAndAlternatingColorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JLabel defineSearchColorsLabel = new JLabel("Find colors");

        panel.add(defineSearchColorsLabel);

        panel.add(Box.createHorizontalStrut(10));
        JScrollPane searchPane = new JScrollPane(searchTable);
        searchPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(searchPane);

        panel.add(Box.createHorizontalStrut(10));
        JLabel defineAlternatingColorLabel = new JLabel("Alternating colors");

        panel.add(defineAlternatingColorLabel);

        panel.add(Box.createHorizontalStrut(10));
        JScrollPane alternatingColorPane = new JScrollPane(alternatingColorTable);
        alternatingColorPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(alternatingColorPane);
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    public void updateColors() {
        rulesTableModel.clear();
        rulesTableModel.addDefaultRules();
        rulesTableModel.fireTableDataChanged();
    }

    private void configureTable() {
        rulesTable.setToolTipText("Double click to edit");
        rulesTable.setRowHeight(20);
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rulesTable.setColumnSelectionAllowed(false);

        Vector backgroundColors = colorizer.getDefaultColors();
        Vector foregroundColors = colorizer.getDefaultColors();
        backgroundColors.add("Browse...");
        foregroundColors.add("Browse...");

        JComboBox background = new JComboBox(backgroundColors);
        background.setMaximumRowCount(15);
        background.setRenderer(new ColorListCellRenderer());

        JComboBox foreground = new JComboBox(foregroundColors);
        foreground.setMaximumRowCount(15);
        foreground.setRenderer(new ColorListCellRenderer());

        DefaultCellEditor backgroundEditor = new DefaultCellEditor(background);
        DefaultCellEditor foregroundEditor = new DefaultCellEditor(foreground);
        JTextField textField = new JTextField();
        textField.addKeyListener(
            new ExpressionRuleContext(filterModel, textField));
        rulesTable.getColumnModel().getColumn(1).setCellEditor(backgroundEditor);
        rulesTable.getColumnModel().getColumn(2).setCellEditor(foregroundEditor);

        background.addItemListener(new ColorItemListener(background));
        foreground.addItemListener(new ColorItemListener(foreground));

        rulesTable.getColumnModel().getColumn(0).setCellRenderer(
            new ExpressionTableCellRenderer());
        rulesTable.getColumnModel().getColumn(1).setCellRenderer(
            new ColorTableCellRenderer());
        rulesTable.getColumnModel().getColumn(2).setCellRenderer(
            new ColorTableCellRenderer());
    }

    private void configureSingleEntryColorTable(JTable thisTable) {
        thisTable.setToolTipText("Double click to edit");
        thisTable.setRowHeight(20);
        thisTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        thisTable.setColumnSelectionAllowed(false);

        Vector backgroundColors = colorizer.getDefaultColors();
        Vector foregroundColors = colorizer.getDefaultColors();
        backgroundColors.add("Browse...");
        foregroundColors.add("Browse...");

        JComboBox background = new JComboBox(backgroundColors);
        background.setMaximumRowCount(15);
        background.setRenderer(new ColorListCellRenderer());

        JComboBox foreground = new JComboBox(foregroundColors);
        foreground.setMaximumRowCount(15);
        foreground.setRenderer(new ColorListCellRenderer());

        DefaultCellEditor backgroundEditor = new DefaultCellEditor(background);
        DefaultCellEditor foregroundEditor = new DefaultCellEditor(foreground);
        thisTable.getColumnModel().getColumn(0).setCellEditor(backgroundEditor);
        thisTable.getColumnModel().getColumn(1).setCellEditor(foregroundEditor);

        background.addItemListener(new ColorItemListener(background));
        foreground.addItemListener(new ColorItemListener(foreground));

        thisTable.getColumnModel().getColumn(0).setCellRenderer(
            new ColorTableCellRenderer());
        thisTable.getColumnModel().getColumn(1).setCellRenderer(
            new ColorTableCellRenderer());
    }

    public void setCloseActionListener(ActionListener listener) {
        closeListener = listener;
    }

    public void hidePanel() {
        if (closeListener != null) {
            closeListener.actionPerformed(null);
        }
    }

    void applyRules(String ruleSet, RuleColorizer applyingColorizer) {
        rulesTable.getColumnModel().getColumn(0).getCellEditor().stopCellEditing();

//        List list = new ArrayList();
//        Vector vector = rulesTableModel.getDataVector();
        StringBuilder result = new StringBuilder();
//
//        for (int i = 0; i < vector.size(); i++) {
//            Vector v = (Vector) vector.elementAt(i);
//
//            try {
//                Rule expressionRule = ExpressionRule.getRule((String) v.elementAt(0));
//                Color background = getBackground();
//                Color foreground = getForeground();
//
//                if (v.elementAt(1) instanceof Color) {
//                    background = (Color) v.elementAt(1);
//                }
//
//                if (v.elementAt(2) instanceof Color) {
//                    foreground = (Color) v.elementAt(2);
//                }
//
//                ColorRule r = new ColorRule((String) v.elementAt(0), expressionRule, background, foreground);
//                list.add(r);
//            } catch (IllegalArgumentException iae) {
//                if (!result.toString().equals("")) {
//                    result.append("<br>");
//                }
//
//                result.append(iae.getMessage());
//            }
//        }

        //all rules are valid, they can be applied
        if (result.toString().equals("")) {
            ((ExpressionTableCellRenderer) rulesTable.getColumnModel().getColumn(0).getCellRenderer())
                .setToolTipText("Double click to edit");
            statusBar.setText(DEFAULT_STATUS);

            //only update rules if there were no errors
            Map map = new HashMap();
            List<ColorRule> newRules = new ArrayList<>(rulesTableModel.rules());
            map.put(ruleSet, newRules);
            applyingColorizer.setRules(map);

        } else {
            statusBar.setText("Errors - see expression tooltip (color filters won't be active until errors are resolved)");
            ((ExpressionTableCellRenderer) rulesTable.getColumnModel().getColumn(0).getCellRenderer())
                .setToolTipText("<html>" + result.toString() + "</html>");
        }
    }

    JPanel buildClosePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());

        JButton saveAsDefaultButton = new JButton(" Save as default ");

        saveAsDefaultButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    RuleColorizer defaultColorizer = allLogPanelColorizers.get(ChainsawConstants.DEFAULT_COLOR_RULE_NAME);
                    applyRules(currentRuleSet, defaultColorizer);
                }
            });

        panel.add(saveAsDefaultButton);

        JButton applyButton = new JButton(" Apply ");

        applyButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    applyRules(currentRuleSet, colorizer);
                    saveSearchColors();
                    saveAlternatingColors();
                    saveBypassFlag();
                }
            });

        panel.add(Box.createHorizontalStrut(10));
        panel.add(applyButton);

        JButton closeButton = new JButton(" Close ");

        closeButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    hidePanel();
                }
            });
        panel.add(Box.createHorizontalStrut(10));
        panel.add(closeButton);

        return panel;
    }

    private void saveSearchColors() {
        Vector thisVector = (Vector) searchTableModel.getDataVector().get(0);
        applicationPreferenceModel.setSearchBackgroundColor((Color) thisVector.get(0));
        applicationPreferenceModel.setSearchForegroundColor((Color) thisVector.get(1));
    }

    private void saveAlternatingColors() {
        Vector thisVector = (Vector) alternatingColorTableModel.getDataVector().get(0);
        applicationPreferenceModel.setAlternatingBackgroundColor((Color) thisVector.get(0));
        Color alternatingColorForegroundColor = (Color) thisVector.get(1);
        applicationPreferenceModel.setAlternatingForegroundColor(alternatingColorForegroundColor);
    }

    private void saveBypassFlag() {
        applicationPreferenceModel.setBypassSearchColors(bypassSearchColorsCheckBox.isSelected());
    }

    JPanel buildUpDownPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout(5, 1));

        final JButton upButton = new JButton(ChainsawIcons.ICON_UP);
        upButton.setToolTipText("Move selected rule up");

        final JButton downButton = new JButton(ChainsawIcons.ICON_DOWN);
        downButton.setToolTipText("Move selected rule down");
        upButton.setEnabled(false);
        downButton.setEnabled(false);

        rulesTable.getSelectionModel().addListSelectionListener(
            e -> {
                if (!e.getValueIsAdjusting()) {
                    int index = rulesTable.getSelectionModel().getMaxSelectionIndex();

                    if (index < 0) {
                        downButton.setEnabled(false);
                        upButton.setEnabled(false);
                    } else if ((index == 0) && (rulesTableModel.getRowCount() == 1)) {
                        downButton.setEnabled(false);
                        upButton.setEnabled(false);
                    } else if ((index == 0) && (rulesTableModel.getRowCount() > 1)) {
                        downButton.setEnabled(true);
                        upButton.setEnabled(false);
                    } else if (index == (rulesTableModel.getRowCount() - 1)) {
                        downButton.setEnabled(false);
                        upButton.setEnabled(true);
                    } else {
                        downButton.setEnabled(true);
                        upButton.setEnabled(true);
                    }
                }
            });

        JPanel upPanel = new JPanel();

        upPanel.add(upButton);

        JPanel downPanel = new JPanel();
        downPanel.add(downButton);

        innerPanel.add(new JLabel(""));
        innerPanel.add(upPanel);
        innerPanel.add(new JLabel(""));
        innerPanel.add(downPanel);
        panel.add(innerPanel, BorderLayout.CENTER);

        upButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    int index = rulesTable.getSelectionModel().getMaxSelectionIndex();
                    rulesTableModel.moveRowAtIndexUp(index);
                }
            });

        downButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    int index = rulesTable.getSelectionModel().getMaxSelectionIndex();
                    rulesTableModel.moveRowAtIndexDown(index);
                }
            });

        return panel;
    }

    JPanel buildRulesPanel() {
        JPanel listPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Box.createVerticalStrut(10));

        JLabel rulesLabel = new JLabel("Rules:");

        panel.add(rulesLabel);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 2));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel newPanel = new JPanel();
        JButton newButton = new JButton(" New ");
        newButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    rulesTableModel.addNewRule();
                }
            });

        newPanel.add(newButton);

        JPanel deletePanel = new JPanel();
        final JButton deleteButton = new JButton(" Delete ");
        deleteButton.setEnabled(false);

        deleteButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent evt) {
                    int index = rulesTable.getSelectionModel().getMaxSelectionIndex();
                    rulesTableModel.deleteRuleAtIndex(index);
                }
            });

        rulesTable.getSelectionModel().addListSelectionListener(
            e -> {
                if (!e.getValueIsAdjusting()) {
                    int index = rulesTable.getSelectionModel().getMaxSelectionIndex();

                    if (index < 0) {
                        deleteButton.setEnabled(false);
                    } else {
                        deleteButton.setEnabled(true);
                    }
                }
            });

        deletePanel.add(deleteButton);

        buttonPanel.add(newPanel);
        buttonPanel.add(deletePanel);

        listPanel.add(panel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tableScrollPane.setBorder(BorderFactory.createEtchedBorder());
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        tablePanel.add(buildUpDownPanel(), BorderLayout.EAST);
        listPanel.add(tablePanel, BorderLayout.CENTER);
        listPanel.add(buttonPanel, BorderLayout.SOUTH);

        return listPanel;
    }

    class ColorListCellRenderer extends JLabel implements ListCellRenderer {
        ColorListCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(
            JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
            setText(" ");

            if (isSelected && (index > -1)) {
                setBorder(BorderFactory.createLineBorder(Color.black, 2));
            } else {
                setBorder(BorderFactory.createEmptyBorder());
            }

            if (value instanceof Color) {
                setBackground((Color) value);
            } else {
                setBackground(Color.white);

                if (value != null) {
                    setText(value.toString());
                }
            }

            return this;
        }
    }

    class ColorItemListener implements ItemListener {
        JComboBox box;
        JDialog dialog;
        JColorChooser colorChooser;
        Color lastColor;

        ColorItemListener(final JComboBox box) {
            this.box = box;
            colorChooser = new JColorChooser();
            dialog =
                JColorChooser.createDialog(
                    box, "Pick a Color", true, //modal
                    colorChooser,
                    e -> {
                        box.insertItemAt(colorChooser.getColor(), 0);
                        box.setSelectedIndex(0);
                    }, //OK button handler
                    e -> box.setSelectedItem(lastColor)); //CANCEL button handler
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (box.getSelectedItem() instanceof Color) {
                    box.setBackground((Color) box.getSelectedItem());
                    repaint();
                } else {
                    box.setBackground(Color.white);
                    int selectedRow = rulesTable.getSelectedRow();
                    int selectedColumn = rulesTable.getSelectedColumn();
                    if (selectedRow != -1 && selectedColumn != -1) {
                        colorChooser.setColor((Color) rulesTable.getValueAt(selectedRow, selectedColumn));
                        lastColor = (Color) rulesTable.getValueAt(selectedRow, selectedColumn);
                    }
                    dialog.setVisible(true);
                }
            }
        }
    }

    class ColorTableCellRenderer implements TableCellRenderer {
        Border border;
        JPanel panel;

        ColorTableCellRenderer() {
            panel = new JPanel();
            panel.setOpaque(true);
        }

        public Color getCurrentColor() {
            return panel.getBackground();
        }

        public Component getTableCellRendererComponent(
            JTable thisTable, Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {
            if (value instanceof Color) {
                panel.setBackground((Color) value);
            }
            if (border == null) {
                border = BorderFactory.createMatteBorder(2, 2, 2, 2, rulesTable.getBackground());
            }

            panel.setBorder(border);

            return panel;
        }
    }

    class ExpressionTableCellRenderer implements TableCellRenderer {
        JPanel panel = new JPanel();
        JLabel expressionLabel = new JLabel();
        JLabel iconLabel = new JLabel();
        Icon selectedIcon = new SelectedIcon(true);
        Icon unselectedIcon = new SelectedIcon(false);

        ExpressionTableCellRenderer() {
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setOpaque(true);
            panel.add(iconLabel);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(expressionLabel);
        }

        void setToolTipText(String text) {
            panel.setToolTipText(text);
        }

        public Component getTableCellRendererComponent(
            JTable thisTable, Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {
            if (value == null) {
                return panel;
            }

            List<ColorRule> rules = rulesTableModel.rules();
            ColorRule rule = rules.get(row);
            expressionLabel.setText(value.toString());

            expressionLabel.setBackground(rule.getBackgroundColor());
            panel.setBackground(rule.getBackgroundColor());
            expressionLabel.setForeground(rule.getForegroundColor());
            panel.setForeground(rule.getForegroundColor());

            if (isSelected) {
                iconLabel.setIcon(selectedIcon);
            } else {
                iconLabel.setIcon(unselectedIcon);
            }

            return panel;
        }
    }

    class SelectedIcon implements Icon {
        private boolean isSelected;
        private int width = 9;
        private int height = 18;
        private int[] xPoints = new int[4];
        private int[] yPoints = new int[4];

        public SelectedIcon(boolean isSelected) {
            this.isSelected = isSelected;
            xPoints[0] = 0;
            yPoints[0] = -1;
            xPoints[1] = 0;
            yPoints[1] = height;
            xPoints[2] = width;
            yPoints[2] = height / 2;
            xPoints[3] = width;
            yPoints[3] = (height / 2) - 1;
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (isSelected) {
                int length = xPoints.length;
                int[] newXPoints = new int[length];
                int[] newYPoints = new int[length];

                for (int i = 0; i < length; i++) {
                    newXPoints[i] = xPoints[i] + x;
                    newYPoints[i] = yPoints[i] + y;
                }

                g.setColor(Color.black);

                g.fillPolygon(newXPoints, newYPoints, length);
            }
        }
    }

    private class RulesTableModel extends AbstractTableModel{
        
        private List<ColorRule> m_data;

        RulesTableModel(){
            m_data = new ArrayList<>();
            addDefaultRules();
        }

        List<ColorRule> rules(){
            return m_data;
        }

        void clear(){
            m_data.clear();
            fireTableDataChanged();
        }

        void addNewRule(){
            Rule expressionRule = ExpressionRule.getRule("msg=='sample'");
            ColorRule newRule =
                    new ColorRule("msg=='sample'", expressionRule, Color.WHITE, Color.BLACK);
            m_data.add(newRule);
            fireTableRowsInserted(m_data.size() - 1, m_data.size() - 1);
        }
        
        void deleteRuleAtIndex(int i){
            m_data.remove(i);
            fireTableRowsDeleted(i, i);
        }

        void moveRowAtIndexDown(int row){
            if( row <= m_data.size() - 2 && row >= 0){
                Collections.swap(m_data, row, row + 1);
                fireTableRowsUpdated(row, row + 1);
            }
        }

        void moveRowAtIndexUp(int row){
            if( row <= m_data.size() && row > 0 ){
                Collections.swap(m_data, row, row - 1);
                fireTableRowsUpdated(row - 1, row );
            }
        }

        void addDefaultRules(){
            Map map = colorizer.getRules();
            for (Object o1 : map.entrySet()) {
                Map.Entry entry = (Map.Entry) o1;
                logger.debug( "entry: {}", o1 );
                //update ruleset list
                if (entry.getKey().equals(currentRuleSet)) {
                    m_data.addAll( (List<ColorRule>)entry.getValue() );
                }
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if( rowIndex >= m_data.size() ){
                return;
            }
            String expression = m_data.get(rowIndex).getExpression();
            Rule rule = m_data.get(rowIndex).getRule();
            Color backgroundColor = m_data.get(rowIndex).getBackgroundColor();
            Color foregroundColor = m_data.get(rowIndex).getForegroundColor();

            switch( columnIndex ){
                case 0:
                    expression = aValue.toString();
                    rule = ExpressionRule.getRule(expression);
                    break;
                case 1:
                    backgroundColor = (Color)aValue;
                    break;
                case 2:
                    foregroundColor = (Color)aValue;
                    break;
            }

            m_data.set(rowIndex, new ColorRule(expression, rule, backgroundColor, foregroundColor) );
            // Since the value has been set, resize all of the rows(if required)
            rulesTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }

        @Override
        public int getRowCount() {
            return m_data.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int row, int col) {
            ColorRule rule = m_data.get(row);
            switch( col ){
                case 0:
                    return rule.getExpression();
                case 1:
                    return rule.getBackgroundColor();
                case 2:
                    return rule.getForegroundColor();
            }
            return "foo";
        }

        @Override
        public String getColumnName(int col){
            switch( col ){
                case 0:
                    return "Expression";
                case 1:
                    return "Background Color";
                case 2:
                    return "Foreground Color";
            }
            return "BAD COL NAME";
        }

        @Override
        public boolean isCellEditable(int row, int col){
            return true;
        }
    }

    private class RulesTableCellEditor extends AbstractCellEditor
        implements TableCellEditor{

        private DefaultCellEditor defaultEditor = new DefaultCellEditor(
            new JTextField());

        @Override
        public Object getCellEditorValue() {
            return defaultEditor.getCellEditorValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            Component c = defaultEditor.getTableCellEditorComponent(table, value,
                isSelected, row, column);
            table.setRowHeight( row, c.getPreferredSize().height );
            return c;
        }

        @Override
        public boolean isCellEditable(EventObject e){
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                return me.getClickCount() >= 2;
            }
            return false;
        }

    }
}
