package ru.smcsystem.smcmodules.module;

import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DialogChat {
    private final Function<String, Optional<IValue>> func;
    public final JFrame frame;
    private JPanel panel;
    private JButton buttonAdd;
    private JButton buttonClear;
    private JTextArea textArea1;
    private JPanel panelContent;
    private JLabel labelHeader;
    private JScrollPane scrollPane;
    private GridBagConstraints gbc;

    public DialogChat(String title, String label, String nameButtonAdd, String nameButtonClear, Function<String, Optional<IValue>> func) {
        this.func = func;
        frame = new JFrame(title);
        $$$setupUI$$$();

        labelHeader.setText(label);
        buttonAdd.setText(nameButtonAdd);
        buttonClear.setText(nameButtonClear);
        // scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding

        // panelContent.setLayout(new BoxLayout(panelContent, BoxLayout.Y_AXIS));
        // panelContent.setLayout(new GridBagLayout());

        init();

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // frame.getRootPane().setDefaultButton(buttonAdd);
        frame.pack();
    }

    public static void main(String[] args) {
        DialogChat dialogChat = new DialogChat("DialogChat", "Chat", "Add", "Clear", null);
        dialogChat.frame.setVisible(true);
    }

    private void cleanPanelContent() {
        panelContent.removeAll();
        // gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0; // Does not expand vertically
    }

    private void init() {
        cleanPanelContent();

        buttonAdd.addActionListener(e -> {
            addPanel(textArea1.getText(), false);
            if (func != null)
                func.apply(textArea1.getText()).ifPresent(v -> addPanel(v, true));
            textArea1.setText("");
            frame.repaint();
        });
        buttonClear.addActionListener(e -> {
            cleanPanelContent();
            textArea1.setText("");
        });

        textArea1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown())
                    buttonAdd.doClick();
            }
        });
    }

    private void addPanel(Object obj, boolean isAnswer) {
        if (obj == null)
            return;
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        // jPanel.setMaximumSize(new Dimension(200, 50));
        jPanel.add(new Label(String.format("%s %s", isAnswer ? "<<" : ">>", Instant.now().toString())), BorderLayout.NORTH);
        JPanel jPanelValue = null;
        // gbc.weighty = 0;
        // gbc.fill = GridBagConstraints.HORIZONTAL;
        if (obj instanceof IValue) {
            IValue value = (IValue) obj;
            if (ModuleUtils.isObjectArray(value)) {
                ObjectArray objectArray = ModuleUtils.getObjectArray(value);
                if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
                    jPanelValue = new JPanel(new BorderLayout());
                    JTable table = new JTable();
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    initTable(table, objectArray, true);
                    JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    // scrollPane.setViewportView(table);
                    jPanelValue.add(scrollPane, BorderLayout.CENTER);
                    // gbc.weighty = 1;
                    // gbc.fill = GridBagConstraints.BOTH;
                } else if (objectArray.isSimple()) {
                    jPanelValue = createPanelValue(Stream.iterate(0, i -> i + 1)
                            .limit(objectArray.size())
                            .map(n -> objectArray.get(n).toString())
                            .collect(Collectors.joining(" , ")));
                }
            }
            if (jPanelValue == null)
                jPanelValue = createPanelValue(ModuleUtils.toString(value));
        } else {
            jPanelValue = createPanelValue(obj.toString());
        }
        jPanel.add(jPanelValue, BorderLayout.CENTER);
        gbc.gridy++;
        // JPanel jPanel2 = new JPanel();
        // jPanel2.setAlignmentX(Component.LEFT_ALIGNMENT);
        // jPanel2.add(jPanel);
        panelContent.add(jPanel, gbc);
        // panelContent.add(Box.createVerticalGlue());
        // jPanel.revalidate();
        panelContent.revalidate();
        // scrollPane.scrollRectToVisible(panelContent.getComponent(panelContent.getComponentCount() - 1).getBounds());
        // SwingUtilities.invokeLater(() -> jPanel.setPreferredSize(jPanel.getBounds().getSize()));
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()));
        // textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private JPanel createPanelValue(String text) {
        text = text.trim();
        if (!text.toLowerCase().startsWith("<html>"))
            text = "<html>" + text;
        if (!text.toLowerCase().endsWith("</html>"))
            text = text + "</html>";
        text = text.replace("\n", "<br/>");
        JLabel jLabel = new JLabel(text);
        // jLabel.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel jPanelValue = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jPanelValue.add(jLabel);
        return jPanelValue;
    }

    private int initTable(JTable table, ObjectArray objectArray, boolean loadAllFields) {
        if (table == null)
            return 0;
        if (objectArray == null || objectArray.size() == 0 || objectArray.getType() != ObjectType.OBJECT_ELEMENT) {
            table.setModel(new DefaultTableModel(
                    new Vector<>(),
                    new Vector<>(List.of("name", "value"))));
            return 0;
        }
        List<String> headers;
        if (loadAllFields) {
            headers = Stream.iterate(0, i -> i + 1)
                    .limit(objectArray.size())
                    .map(n -> (ObjectElement) objectArray.get(n))
                    .flatMap(e -> e.getFields().stream().map(ObjectField::getName))
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            headers = ((ObjectElement) objectArray.get(0)).getFields().stream()
                    .map(ObjectField::getName).collect(Collectors.toList());
        }
        if (headers.isEmpty()) {
            headers = List.of("name", "value");
        } else {
            List<String> headersPrimary = List.of("id", "ID", "name", "NAME", "key", "KEY");
            List<String> headersNew = new ArrayList<>(headers.size() + 1);
            List<String> headersTmp = headers;
            headersPrimary.forEach(h -> {
                if (headersTmp.remove(h))
                    headersNew.add(h);
            });
            headersNew.addAll(headers);
            headers = headersNew;
        }
        int countValuesInTable = headers.size();
        Vector<Vector<String>> data = new Vector<>(objectArray.size() + 1);
        for (int i = 0; i < objectArray.size(); i++) {
            ObjectElement objectElement2 = (ObjectElement) objectArray.get(i);
            data.add(headers.stream()
                    .flatMap(s -> objectElement2.findFieldIgnoreCase(s).stream())
                    .map(ObjectField::getValue)
                    .map(v -> v != null ? v.toString() : "NULL")
                    .collect(Collectors.toCollection(Vector::new)));
        }
        table.setModel(new DefaultTableModel(
                data,
                new Vector<>(headers)));
        return countValuesInTable;
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
        panel.setPreferredSize(new Dimension(300, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel.add(panel1, BorderLayout.SOUTH);
        buttonAdd = new JButton();
        buttonAdd.setBackground(new Color(-15817239));
        buttonAdd.setText("Add");
        panel1.add(buttonAdd);
        buttonClear = new JButton();
        buttonClear.setText("Clear");
        panel1.add(buttonClear);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel.add(panel2, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.SOUTH);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setPreferredSize(new Dimension(300, 100));
        panel3.add(scrollPane1, BorderLayout.CENTER);
        textArea1 = new JTextArea();
        scrollPane1.setViewportView(textArea1);
        scrollPane = new JScrollPane();
        panel2.add(scrollPane, BorderLayout.CENTER);
        panelContent = new JPanel();
        panelContent.setLayout(new GridBagLayout());
        scrollPane.setViewportView(panelContent);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel.add(panel4, BorderLayout.NORTH);
        labelHeader = new JLabel();
        labelHeader.setText("");
        panel4.add(labelHeader);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }

}
