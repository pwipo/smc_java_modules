package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import org.swixml.SwingEngine;
import org.swixml.jsr.widgets.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainForm extends JFrame {

    private final SwingEngine<JFrame> engine;

    public MainForm(String configuration) throws Exception {
        for (Constructor ct : org.swixml.MacApp.class.getDeclaredConstructors())
            ct.setAccessible(true);
        engine = new SwingEngine<>(this);
        try (StringReader sr = new StringReader(configuration)) {
            engine.render(sr);
        }
        // setActions(this, actionListener);
    }

    public void setActions(Container parent, ActionListener action, ListSelectionListener listSelectionListener, TreeSelectionListener treeSelectionListener) {
        if (parent == null)
            return;
        for (Component c : parent.getComponents()) {
            // System.out.println(c.toString());
            if (c instanceof JButton) {
                ((JButton) c).addActionListener(action);
            } else if (c instanceof JMenu) {
                setActions(((JMenu) c).getPopupMenu(), action, listSelectionListener, treeSelectionListener);
            } else if (c instanceof JMenuItem) {
                ((JMenuItem) c).addActionListener(action);
            } else if (listSelectionListener != null && c instanceof JList) {
                ((JList) c).addListSelectionListener(listSelectionListener);
            } else if (c instanceof JComboBox) {
                ((JComboBox) c).addActionListener(action);
            } else if (treeSelectionListener != null && c instanceof JTree) {
                ((JTree) c).addTreeSelectionListener(treeSelectionListener);
            } else if (c instanceof Container) {
                setActions((Container) c, action, listSelectionListener, treeSelectionListener);
            }
        }
    }

    public SwingEngine<JFrame> getEngine() {
        return engine;
    }

    public Component findElement(String id) {
        Objects.requireNonNull(id);
        Component component = engine.find(id);
        if (component == null)
            throw new NoSuchElementException(id);
        return component;
    }

    public Object getValue(Component component) {
        Objects.requireNonNull(component);
        Object result = null;
        if (component instanceof JTextAreaEx) {
            result = ((JTextAreaEx) component).getText();
        } else if (component instanceof JTextFieldEx) {
            result = ((JTextFieldEx) component).getText();
        } else if (component instanceof JComboBoxEx) {
            Object item = ((JComboBoxEx) component).getSelectedItem();
            if (item != null)
                result = item.toString();
        } else if (component instanceof JCheckBoxEx) {
            result = ((JCheckBoxEx) component).isSelected() ? 1 : 0;
        } else if (component instanceof JRadioButtonEx) {
            result = ((JRadioButtonEx) component).isSelected() ? 1 : 0;
        } else if (component instanceof JListEx) {
            result = ((JListEx) component).getSelectedValue();
        } else if (component instanceof JTreeEx) {
            TreePath selectionPath = ((JTreeEx) component).getSelectionPath();
            if (selectionPath != null) {
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                Object userObject = lastPathComponent.getUserObject();
                result = userObject.toString();
            }
        } else if (component instanceof JLabelEx) {
            result = ((JLabelEx) component).getText();
        } else if (component instanceof JButton) {
            result = ((JButton) component).getText();
        } else if (component instanceof JEditorPane) {
            result = ((JEditorPane) component).getText();
        } else if (component instanceof JMenuItem) {
            result = ((JMenuItem) component).getText();
        }
        return result;
    }

    public void setValue(String id, Object value) {
        Objects.requireNonNull(value);
        Component component = findElement(id);
        SwingUtilities.invokeLater(() -> {
            if (component instanceof JTextAreaEx) {
                ((JTextAreaEx) component).setText(value.toString());
            } else if (component instanceof JTextFieldEx) {
                ((JTextFieldEx) component).setText(value.toString());
            } else if (component instanceof JComboBoxEx) {
                if (!(value instanceof Number))
                    throw new RuntimeException("need number value for set");
                ((JComboBoxEx) component).setSelectedIndex(((Number) value).intValue());
            } else if (component instanceof JCheckBoxEx) {
                if (!(value instanceof Number))
                    throw new RuntimeException("need number value for set");
                ((JCheckBoxEx) component).setSelected(((Number) value).intValue() > 0);
            } else if (component instanceof JRadioButtonEx) {
                if (!(value instanceof Number))
                    throw new RuntimeException("need number value for set");
                ((JRadioButtonEx) component).setSelected(((Number) value).intValue() > 0);
                // } else if (component instanceof JTreeEx) {
            } else if (component instanceof JLabelEx) {
                if (value instanceof byte[]) {
                    ((JLabelEx) component).setIcon(new ImageIcon((byte[]) value));
                } else {
                    ((JLabelEx) component).setText(value.toString());
                }
            } else if (component instanceof JButton) {
                ((JButton) component).setText(value.toString());
            } else if (component instanceof JEditorPane) {
                ((JEditorPane) component).setText(value.toString());
            } else if (component instanceof JMenuItem) {
                ((JMenuItem) component).setText(value.toString());
            }
        });
    }

    public void textAreaHighlight(String id, java.util.List<String> words, boolean onlyWords) {
        Component component = findElement(id);
        if (!(component instanceof JTextAreaEx))
            throw new NoSuchElementException(id);
        JTextAreaEx textArea = (JTextAreaEx) component;
        ;

        // search words
        Highlighter h = textArea.getHighlighter();
        h.removeAllHighlights();

        String text = textArea.getText();
        /*
        StyledDocument styledDocument = textPane1.getStyledDocument();
        String text = styledDocument.getText(0, styledDocument.getLength());
        */
        if (StringUtils.isBlank(text))
            return;

        SwingUtilities.invokeLater(() -> {
            words.forEach(value -> {
                Pattern pattern = onlyWords ? Pattern.compile("\\b" + value + "\\b") : Pattern.compile(value);
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
                    try {
                        h.addHighlight(start, end, painter);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            });
        });
    }

    public String textComponentGetSelectedText(String id) {
        Component component = findElement(id);
        if (!(component instanceof JTextComponent))
            throw new NoSuchElementException(id);
        JTextComponent textComponent = (JTextComponent) component;
        return textComponent.getSelectedText();
    }

    public void textComponentReplaceSelectedText(String id, String text) {
        Component component = findElement(id);
        if (!(component instanceof JTextComponent))
            throw new NoSuchElementException(id);
        JTextComponent textComponent = (JTextComponent) component;
        SwingUtilities.invokeLater(() -> {
            textComponent.replaceSelection(text);
        });
    }

    public void textComponentInsertIntoCurrentCursorPosition(String id, String text) throws BadLocationException {
        Component component = findElement(id);
        if (!(component instanceof JTextComponent))
            throw new NoSuchElementException(id);
        JTextComponent textComponent = (JTextComponent) component;
        // textArea.insert(text, textArea.getCaretPosition());
        SwingUtilities.invokeLater(() -> {
            try {
                textComponent.getDocument().insertString(textComponent.getCaretPosition(), text, null);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void textComponentReplaceAll(String id, String search, String replace) {
        Component component = findElement(id);
        if (!(component instanceof JTextComponent))
            throw new NoSuchElementException(id);
        JTextComponent textComponent = (JTextComponent) component;
        SwingUtilities.invokeLater(() -> {
            String text = textComponent.getText();
            text = text.replaceAll(search, replace);
            textComponent.setText(text);
        });
    }

    public void textComponentEditable(String id, boolean editable) {
        Component component = findElement(id);
        if (!(component instanceof JTextComponent))
            throw new NoSuchElementException(id);
        JTextComponent textComponent = (JTextComponent) component;
        SwingUtilities.invokeLater(() -> {
            textComponent.setEditable(editable);
        });
    }

    public void elementEnable(String id, boolean enable) {
        SwingUtilities.invokeLater(() -> {
            findElement(id).setEnabled(enable);
        });
    }

    public void elementVisible(String id, boolean visible) {
        SwingUtilities.invokeLater(() -> {
            findElement(id).setVisible(visible);
        });
    }

    private JListEx getList(String id) {
        Component component = findElement(id);
        if (!(component instanceof JListEx))
            throw new NoSuchElementException(id);
        JListEx listComponent = (JListEx) component;

        ListModel model = listComponent.getModel();
        if (!(model instanceof DefaultListModel)) {
            listComponent.clearSelection();
            listComponent.removeAll();
            DefaultListModel listModel = new DefaultListModel();
            listComponent.setModel(listModel);
        }
        return listComponent;
    }

    public void setListElements(String id, java.util.List<Object> values) {
        SwingUtilities.invokeLater(() -> {
            JListEx listComponent = getList(id);
            DefaultListModel listModel = (DefaultListModel) listComponent.getModel();
            listModel.clear();
            values.forEach(listModel::addElement);
        });
    }

    public void addListElement(String id, Object value) {
        SwingUtilities.invokeLater(() -> {
            JListEx listComponent = getList(id);
            DefaultListModel listModel = (DefaultListModel) listComponent.getModel();
            listModel.addElement(value);
        });
    }

    public void removeListElement(String id, Integer idElement) {
        SwingUtilities.invokeLater(() -> {
            JListEx listComponent = getList(id);
            DefaultListModel listModel = (DefaultListModel) listComponent.getModel();
            listModel.remove(idElement);
        });
    }

    public java.util.List<Object> getListSelected(String id) {
        JListEx listComponent = getList(id);
        LinkedList<Object> result = new LinkedList<>();
        DefaultListModel listModel = (DefaultListModel) listComponent.getModel();
        for (int index : listComponent.getSelectedIndices()) {
            result.add(index);
            result.add(listModel.get(index));
        }
        return result;
    }

}
