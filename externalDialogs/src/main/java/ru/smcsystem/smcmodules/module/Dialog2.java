package ru.smcsystem.smcmodules.module;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Dialog2 extends JFrame {
    private JPanel panel;
    private JTextField textField1;
    private JTextField textField2;
    private JButton button1;
    private JLabel labelMessages;
    public boolean enterButton;

    public Dialog2(String title, String label, String buttonName, boolean showSecondField) {
        enterButton = false;
        setTitle(title);
        setContentPane(panel);
        setResizable(false);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        labelMessages.setText(label);
        button1.setText(buttonName);
        button1.addActionListener(e -> {
            enterButton = true;
            setVisible(false);
        });
        getTextField2().setVisible(showSecondField);
        this.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                textField1.requestFocus();
            }
        });
        // setSize(224, 122);
        this.setUndecorated(true);
        this.getRootPane().setWindowDecorationStyle(JRootPane.QUESTION_DIALOG);
        this.getRootPane().setDefaultButton(button1);
        pack();
    }

    public JPanel getPanel() {
        return panel;
    }

    public JTextField getTextField1() {
        return textField1;
    }

    public JTextField getTextField2() {
        return textField2;
    }

    public static void main(String[] args) {
        Dialog2 frame = new Dialog2("search", "test", "find", true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        panel.setLayout(new GridBagLayout());
        textField1 = new JTextField();
        textField1.setColumns(20);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(textField1, gbc);
        textField2 = new JTextField();
        textField2.setColumns(20);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(textField2, gbc);
        button1 = new JButton();
        button1.setText("Button");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(button1, gbc);
        labelMessages = new JLabel();
        labelMessages.setHorizontalAlignment(0);
        labelMessages.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(labelMessages, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }
}
