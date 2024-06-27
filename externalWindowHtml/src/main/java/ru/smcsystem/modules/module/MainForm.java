package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.Option;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MainForm {
    private ExecutionContextTool executionContextTool;
    private JPanel panel;
    public JEditorPane editorPane;
    public JScrollPane scrollPane;
    public JFrame frame;

    public enum ElementType {
        SELECT, TEXTAREA, SUBMIT, RESET, IMAGE, CHECKBOX, RADIO, TEXT, PASSWORD, FILE, OTHER
    }

    public final Map<String, FormElement> elements;
    private final Map<String, Integer> elementsToEcId;
    private String configuration;

    public MainForm(String title, String configuration, int width, int height) {
        this.executionContextTool = null;
        frame = new JFrame(title);
        this.configuration = configuration;

        $$$setupUI$$$();

        elements = new ConcurrentHashMap<>();
        elementsToEcId = new ConcurrentHashMap<>();
        init();

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setPreferredSize(new Dimension(width, height));
        frame.pack();
    }

    public static void main(String[] args) {
        MainForm window = new MainForm("MainForm", "<html></html>", 300, 300);
        window.frame.setVisible(true);
    }

    private void init() {
        editorPane.setEditorKit(new CompEditorKit(this)); // install our hook
        clean();
    }

    public void clean() {
        elements.clear();
        elementsToEcId.clear();
        editorPane.setText(configuration);
    }

    public void start() {
        frame.setVisible(true);
        frame.requestFocus();
        frame.requestFocusInWindow();
        frame.toFront();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            SwingUtilities.invokeLater(() -> {
                frame.requestFocus();
                frame.requestFocusInWindow();
                frame.toFront();
            });
        }).start();
    }

    public ExecutionContextTool getExecutionContextTool() {
        return executionContextTool;
    }

    public void setExecutionContextTool(ExecutionContextTool executionContextTool) {
        this.executionContextTool = executionContextTool;
    }

    private Element getFormElement(FormElement e) {
        Element elem = e.element;
        while (elem != null) {
            if (elem.getAttributes().getAttribute
                    (StyleConstants.NameAttribute) == HTML.Tag.FORM) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public Optional<Object> getValue(String id) {
        return Optional.ofNullable(elements.get(id)).map(e -> {
            Object m = e.element.getAttributes().getAttribute(StyleConstants.ModelAttribute);
            switch (e.type) {
                case SELECT: {
                    ObjectArray objectArray = new ObjectArray();
                    if (m instanceof ComboBoxModel) {
                        ComboBoxModel<Option> model = (ComboBoxModel) m;
                        Option option = (Option) model.getSelectedItem();
                        if (option != null) {
                            String value = option.getValue();
                            if (value != null) {
                                JComboBox<Option> component = (JComboBox<Option>) e.component.getComponent(0);
                                objectArray.add(new ObjectElement(new ObjectField("id", component.getSelectedIndex()), new ObjectField("value", value)));
                            }
                        }
                        // return component.getSelectedItem() != null ? java.util.List.of(component.getSelectedIndex(), component.getSelectedItem()) : -1;
                    } else {
                        // JList<Option> component = (JList<Option>) e.component.getComponent(0);
                        ListSelectionModel model = (ListSelectionModel) m;
                        DefaultListModel<Option> model2 = (DefaultListModel<Option>) m;
                        Arrays.stream(model.getSelectedIndices())
                                // .filter(model::isSelectedIndex)
                                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, model2.getElementAt(i)))
                                .filter(e2 -> e2.getValue() != null && e2.getValue().getValue() != null)
                                .map(e2 -> new ObjectElement(new ObjectField("id", e2.getKey()), new ObjectField("value", e2.getValue().getValue())))
                                .forEach(objectArray::add);
                        // return component.getSelectedIndex() > -1 ? java.util.List.of(component.getSelectedIndex(), component.getSelectedValue()) : -1;
                    }
                    return objectArray;
                }
                case TEXTAREA:
                case TEXT:
                case PASSWORD: {
                    try {
                        Document doc = (Document) m;
                        return doc.getText(0, doc.getLength());
                    } catch (BadLocationException ignore) {
                    }
                    break;
                }
                case SUBMIT: {
                    String v = (String) e.element.getAttributes().getAttribute(HTML.Attribute.VALUE);
                    if (v == null)
                        v = "";
                    return v;
                }
                case IMAGE: {
                    try {
                        return toJpg(iconToImage(((JButton) e.component).getIcon()));
                    } catch (Exception ignore) {
                    }
                    return null;
                }
                case RESET:
                    String v = (String) e.element.getAttributes().getAttribute(HTML.Attribute.VALUE);
                    if (v == null)
                        v = "";
                    return v;
                case CHECKBOX:
                case RADIO: {
                    JToggleButton.ToggleButtonModel model = (JToggleButton.ToggleButtonModel) m;
                    return model.isSelected();
                }
                case FILE: {
                    try {
                        Document doc = (Document) m;
                        return doc.getText(0, doc.getLength());
                    } catch (BadLocationException ignore) {
                    }
                    break;
                }
                case OTHER: {
                    ExtendedHTMLDocument document = (ExtendedHTMLDocument) e.element.getDocument();
                    if (e.element.getElementCount() == 0)
                        return "";
                    try {
                        Element elementFirst = e.element.getElement(0);
                        Element elementLast = e.element.getElement(e.element.getElementCount() - 1);
                        return getHtml(elementFirst, elementLast);
                    } catch (Exception ignore) {
                    }
                }
            }
            return null;
        });
    }

    private byte[] toJpg(Image img) throws IOException {
        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "jpg", b);
            return b.toByteArray();
        }
    }

    private Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    public void setValue(String id, Object value) {
        FormElement e = elements.get(id);
        if (e == null)
            return;
        Object m = e.element.getAttributes().getAttribute(StyleConstants.ModelAttribute);
        switch (e.type) {
            case SELECT: {
                if (m instanceof ComboBoxModel) {
                    ComboBoxModel<Option> model = (ComboBoxModel) m;
                    if (value instanceof Number) {
                        Option option = model.getElementAt(((Number) value).intValue());
                        model.setSelectedItem(option);
                    } else {
                        String valueString = value != null ? value.toString() : "";
                        for (int i = 0; i < model.getSize(); i++) {
                            Option option = model.getElementAt(i);
                            if (option != null && option.getValue() != null && Objects.equals(option.getValue(), valueString)) {
                                model.setSelectedItem(option);
                                break;
                            }
                        }
                    }
                } else {
                    ListSelectionModel model = (ListSelectionModel) m;
                    DefaultListModel<Option> model2 = (DefaultListModel<Option>) m;
                    if (value instanceof Number) {
                        int i = ((Number) value).intValue();
                        model.setSelectionInterval(i, i);
                    } else {
                        String valueString = value != null ? value.toString() : "";
                        for (int i = 0; i < model2.getSize(); i++) {
                            Option option = model2.getElementAt(i);
                            if (option != null && option.getValue() != null && Objects.equals(option.getValue(), valueString)) {
                                model.setSelectionInterval(i, i);
                                break;
                            }
                        }
                    }
                }
                break;
            }
            case TEXTAREA:
            case TEXT:
            case PASSWORD: {
                try {
                    PlainDocument doc = (PlainDocument) m;
                    doc.remove(0, doc.getLength());
                    if (value != null)
                        doc.insertString(0, value.toString(), null);
                } catch (BadLocationException ignore) {
                }
                break;
            }
            case SUBMIT: {
                String v = value != null ? value.toString() : "";
                ((SimpleAttributeSet) e.element.getAttributes()).addAttribute(HTML.Attribute.VALUE, v);
                ((JButton) e.component).setText(v);
            }
            case RESET:
                break;
            case IMAGE: {
                try {
                    Image image = ImageIO.read(new ByteArrayInputStream((byte[]) value));
                    Icon icon = new ImageIcon(image);
                    ((JButton) e.component).setIcon(icon);
                } catch (Exception ignore) {
                }
                break;
            }
            case CHECKBOX:
            case RADIO: {
                ButtonModel model = (ButtonModel) m;
                model.setSelected(value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(value.toString()));
                break;
            }
            case FILE: {
                try {
                    Document doc = (Document) m;
                    if (doc.getLength() > 0)
                        doc.remove(0, doc.getLength());
                    if (value != null)
                        doc.insertString(0, value.toString(), null);
                } catch (BadLocationException ignore) {
                }
            }
            case OTHER: {
                ExtendedHTMLDocument document = (ExtendedHTMLDocument) e.element.getDocument();
                try {
                    removeFromCache(e.element);
                    document.setInnerHTML(e.element, value != null ? value.toString() : "");
                } catch (Exception ignore) {
                }
                break;
            }
        }
    }

    public void addEC(String id, int ecId) {
        FormElement e = elements.get(id);
        if (e == null)
            return;

        elementsToEcId.put(id, ecId);
        switch (e.type) {
            case SELECT:
                if (e.component instanceof JComboBox) {
                    ((JComboBox) e.component).addItemListener(e2 -> processEcCallWithData(id, ecId));
                } else {
                    ((JList<Option>) e.component.getComponent(0)).addListSelectionListener(e2 -> processEcCallWithData(id, ecId));
                }
                break;
            case TEXTAREA:
                break;
            case SUBMIT:
            case IMAGE: {
                ((JButton) e.component).addActionListener(e3 -> {
                    Element formE = getFormElement(e);
                    if (formE != null) {
                        ElementIterator it = new ElementIterator(formE);
                        Element next;
                        ObjectElement objectElement = new ObjectElement();
                        ObjectArray objectArray = new ObjectArray(objectElement);
                        while ((next = it.next()) != null) {
                            if (!next.isLeaf())
                                continue;
                            String type = (String) next.getAttributes().getAttribute(HTML.Attribute.TYPE);
                            if (type != null && (type.equals("submit") || type.equals("reset")))
                                continue;
                            Element nextTmp = next;
                            elements.entrySet().stream()
                                    .filter(e2 -> Objects.equals(e2.getValue().element, nextTmp))
                                    .findAny()
                                    .ifPresent(e2 -> getValue(e2.getKey()).ifPresent(v ->
                                            objectElement.getFields().add(new ObjectField(
                                                    Optional.ofNullable((String) e2.getValue().element.getAttributes().getAttribute(HTML.Attribute.NAME)).orElse(e2.getKey()),
                                                    ModuleUtils.getObjectType(v), v))));
                        }
                        String name = Optional.ofNullable((String) e.element.getAttributes().getAttribute(HTML.Attribute.NAME)).orElse(id);
                        if (e.type == ElementType.SUBMIT) {
                            String v = (String) e.element.getAttributes().getAttribute(HTML.Attribute.VALUE);
                            if (v == null)
                                v = "";
                            objectElement.getFields().add(new ObjectField(name, ModuleUtils.getObjectType(v), v));
                        } else {
                            try {
                                byte[] data = toJpg(iconToImage(((JButton) e.component).getIcon()));
                                objectElement.getFields().add(new ObjectField(name, data));
                            } catch (Exception ignore) {
                            }
                        }
                        processEcCall(ecId, List.of(objectArray));
                    }
                });
                break;
            }
            case RESET:
                ((JButton) e.component).addActionListener(e2 -> processEcCallWithData(id, ecId));
                break;
            case CHECKBOX:
                ((JCheckBox) e.component).addActionListener(e2 -> processEcCallWithData(id, ecId));
                break;
            case RADIO:
                ((JRadioButton) e.component).addActionListener(e2 -> processEcCallWithData(id, ecId));
                break;
            case TEXT:
                ((JTextField) e.component.getComponent(0)).addActionListener(e2 -> processEcCallWithData(id, ecId));
                break;
            case PASSWORD:
                ((JPasswordField) e.component.getComponent(0)).addActionListener(e2 -> processEcCallWithData(id, ecId));
                break;
            case FILE:
                ((JButton) e.component.getComponent(2)).addActionListener(e2 -> processEcCallWithData(id, ecId));
                break;
            case OTHER:
                break;
        }
    }

    public Optional<String> getElement(String id) {
        FormElement e = elements.get(id);
        if (e == null)
            return Optional.empty();
        return getHtml(e.element, e.element);
    }

    private Optional<String> getHtml(Element elementFirst, Element elementLast) {
        ExtendedHTMLDocument document = (ExtendedHTMLDocument) elementFirst.getDocument();
        try {
            StringWriter writer = new StringWriter();
            HTMLElementWriter w = new HTMLElementWriter(writer, document, elementFirst, elementLast);
            w.write();
            // editorPane.getEditorKit().write(writer, document, element.getStartOffset(), element.getEndOffset() - element.getStartOffset();
            return Optional.of(writer.toString());
            // return Optional.ofNullable(document.getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset()));
        } catch (Exception ignore) {
        }
        return Optional.empty();
    }

    public void setElement(String id, String html) {
        FormElement e = elements.get(id);
        if (e == null || html == null)
            return;
        ExtendedHTMLDocument document = (ExtendedHTMLDocument) e.element.getDocument();
        try {
            removeFromCache(e.element);
            document.setOuterHTML(e.element, html);
            Integer ecId = elementsToEcId.get(id);
            if (ecId != null)
                addEC(id, ecId);
        } catch (Exception ignore) {
        }
    }

    private void removeFromCache(Element element) {
        for (int i = 0; i < element.getElementCount(); i++)
            removeFromCache(element.getElement(i));
        elements.entrySet().stream().filter(e -> Objects.equals(e.getValue().element, element)).findAny().ifPresent(e -> elements.remove(e.getKey()));
    }

    public void setAttribute(String id, String name, Object value) {
        FormElement e = elements.get(id);
        if (e == null)
            return;
        setAttribute(e.element, name, value);
    }

    public int countChilds(String id) {
        FormElement e = elements.get(id);
        if (e == null)
            return -1;
        return e.element.getElementCount();
    }

    public void addChildElement(String id, String html, int position) {
        FormElement e = elements.get(id);
        if (e == null || html == null)
            return;
        ExtendedHTMLDocument document = (ExtendedHTMLDocument) e.element.getDocument();
        try {
            if (position == 0) {
                document.insertAfterStart(e.element, html);
            } else if (position == -1) {
                document.insertBeforeEnd(e.element, html);
            } else {
                if (position < 0)
                    position = Math.max(0, e.element.getElementCount() + position);
                if (position >= e.element.getElementCount())
                    return;
                document.insertBeforeStart(e.element.getElement(position), html);
            }
            Integer ecId = elementsToEcId.get(id);
            if (ecId != null)
                addEC(id, ecId);
        } catch (Exception ignore) {
        }
    }

    public void removeChildElement(String id, int position) {
        FormElement e = elements.get(id);
        if (e == null)
            return;
        ExtendedHTMLDocument document = (ExtendedHTMLDocument) e.element.getDocument();
        try {
            if (position < 0)
                position = Math.max(0, e.element.getElementCount() + position);
            if (position >= e.element.getElementCount())
                return;
            document.removeElement(e.element.getElement(position));
        } catch (Exception ignore) {
        }
    }

    /*
    public void clearChilds(String id) {
        FormElement e = elements.get(id);
        if (e == null)
            return;
        ExtendedHTMLDocument document = (ExtendedHTMLDocument) e.element.getDocument();
        if(e.element.getElementCount()>0) {
            try {
                for (int i = e.element.getElementCount() - 1; i >= 0; i--) {
                    document.removeElement(e.element.getElement(i));
                    //Element element = e.element.getElement(i);
                    //document.remove(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());
                }
                // Element elementFirst = e.element.getElement(0);
                // Element elementLast = e.element.getElement(0);
            } catch (Exception ignore) {
            }
        }
    }
    */

    public void setChildAttribute(String id, String name, Object value, int position) {
        FormElement e = elements.get(id);
        if (e == null)
            return;
        if (position < 0)
            position = Math.max(0, e.element.getElementCount() + position);
        if (position >= e.element.getElementCount())
            return;
        setAttribute(e.element.getElement(position), name, value);
    }

    private void setAttribute(Element element, String name, Object value) {
        if (element == null || value == null || name == null)
            return;
        HTMLDocument doc = (HTMLDocument) element.getDocument();
        MutableAttributeSet attributeSet = new SimpleAttributeSet();
        attributeSet.addAttribute(name, value);
        doc.setCharacterAttributes(element.getStartOffset(), element.getEndOffset() - element.getStartOffset(), attributeSet, false);
    }

    private void processEcCallWithData(String id, int ecId) {
        getValue(id).ifPresent(v -> processEcCall(ecId, List.of(v)));
    }

    private void processEcCall(int ecId, List<Object> values) {
        if (executionContextTool == null)
            return;
        long threadId = executionContextTool.getFlowControlTool().executeParallel(CommandType.EXECUTE, List.of(ecId), values, 0, 0);
        executionContextTool.getFlowControlTool().releaseThreadCache(threadId);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        scrollPane = new JScrollPane();
        panel.add(scrollPane, BorderLayout.CENTER);
        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        scrollPane.setViewportView(editorPane);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }

}
