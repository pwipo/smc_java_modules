package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Dialogs implements Module {

    private enum Type {
        INPUT,
        TWO_INPUT,
        CONFIRM_OK_CANCEL,
        CONFIRM_YES_NO,
        CONFIRM_YES_NO_CANCEL,
        MESSAGE,
        FILE_READ,
        FILE_SAVE,
        SELECT
    }

    private Type type;
    private String title;
    private String message;
    // private String buttonName;
    // private Boolean showSecondField;
    private Map<String, String> dictionary;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        title = (String) configurationTool.getSetting("title").orElseThrow(() -> new ModuleException("title setting")).getValue();
        message = (String) configurationTool.getSetting("message").orElseThrow(() -> new ModuleException("message setting")).getValue();

        String lang = (String) configurationTool.getSetting("lang").orElseThrow(() -> new ModuleException("lang setting")).getValue();
        dictionary = new HashMap<>();
        if (!lang.isBlank()) {
            String[] arrMessages = lang.split(";;");
            Locale locale = Locale.getDefault();
            for (String str : arrMessages) {
                String[] arrStr = str.split("::");
                if (arrStr.length < 3)
                    continue;
                String searchKey = "[[" + arrStr[0].trim() + "]]";
                String strValue = arrStr[2];
                for (int i = 1; i < arrStr.length - 1; i = i + 2) {
                    if (locale.getLanguage().equals(arrStr[i].trim())) {
                        strValue = arrStr[i + 1];
                        break;
                    }
                }
                dictionary.put(searchKey, strValue);
            }
        }
        message = translate(message);
        title = translate(title);

        // buttonName = (String) configurationTool.getSetting("buttonName").orElseThrow(() -> new ModuleException("buttonName setting")).getValue();
        // showSecondField = Boolean.valueOf((String) configurationTool.getSetting("showSecondField").orElseThrow(() -> new ModuleException("showSecondField setting")).getValue());
    }

    private String translate(String str) {
        if (StringUtils.isBlank(str) || dictionary.isEmpty())
            return str;
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            str = StringUtils.replace(str, entry.getKey(), entry.getValue());
        }
        return str;
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        String externalMessage = Stream.iterate(0, n -> n + 1)
                .limit(executionContextTool.countSource())
                .flatMap(i -> executionContextTool.getMessages(i).stream())
                .flatMap(a -> a.getMessages().stream())
                .map(m -> m.getValue().toString())
                .collect(Collectors.joining());
        String resultMessage = StringUtils.isNoneBlank(externalMessage) ? translate(externalMessage) : message;
        JFrame dummyFrame = createDummyFrame();
        try {
            switch (type) {
                case INPUT:
                    // executionContextTool.addMessage(JOptionPane.showInputDialog(null, externalMessage, title, JOptionPane.QUESTION_MESSAGE));
                    twoInputDialog(executionContextTool, dummyFrame, title, message, StringUtils.isNoneBlank(externalMessage) ? resultMessage : "", null);
                    break;
                case TWO_INPUT:
                    twoInputDialog(executionContextTool, dummyFrame, title, message, StringUtils.isNoneBlank(externalMessage) ? resultMessage : "", "");
                    break;
                case CONFIRM_OK_CANCEL:
                    showConfirmDialog(executionContextTool, dummyFrame, title, resultMessage, JOptionPane.OK_CANCEL_OPTION);
                    break;
                case CONFIRM_YES_NO:
                    showConfirmDialog(executionContextTool, dummyFrame, title, resultMessage, JOptionPane.YES_NO_OPTION);
                    break;
                case CONFIRM_YES_NO_CANCEL:
                    showConfirmDialog(executionContextTool, dummyFrame, title, resultMessage, JOptionPane.YES_NO_CANCEL_OPTION);
                    break;
                case MESSAGE: {
                    // JLabel label = new JLabel(currentModuleDefinitionDTO.getDescription());
                    JTextArea textArea = new JTextArea(resultMessage);
                    // textArea.setMinimumSize(new Dimension(100, 100));
                    // label.setFont(new Font("Arial", Font.BOLD, 18));
                    textArea.setEditable(false);
                    // textArea.setLineWrap(true);
                    // textArea.setWrapStyleWord(true);
                    textArea.setBackground(new Color(-1052689));
                    int panelWidth = Math.min(800, Math.max(100, (int) textArea.getPreferredSize().getWidth()));
                    int panelHeight = Math.min(600, Math.max(50, (int) textArea.getPreferredSize().getHeight()));

                    JScrollPane jScrollPane = new JScrollPane();
                    // jScrollPane.setMinimumSize(new Dimension(800, 600));
                    jScrollPane.setPreferredSize(new Dimension(panelWidth, panelHeight));
                    jScrollPane.setMaximumSize(new Dimension(panelWidth, panelHeight));
                    jScrollPane.setViewportView(textArea);
                    JOptionPane.showMessageDialog(dummyFrame, jScrollPane, title, JOptionPane.INFORMATION_MESSAGE);
                    // JOptionPane.showMessageDialog(createDummyFrame(), resultMessage, title, JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
                case FILE_READ:
                    fileChooser(executionContextTool, dummyFrame, false, JFileChooser.FILES_ONLY, false);
                    break;
                case FILE_SAVE:
                    fileChooser(executionContextTool, dummyFrame, true, JFileChooser.FILES_ONLY, false);
                    break;
                case SELECT:
                    showSelectDialog(executionContextTool, dummyFrame, title, message);
                    break;
            }
        } finally {
            dummyFrame.dispose();
        }
    }

    private void twoInputDialog(ExecutionContextTool executionContextTool, JFrame dummyFrame, String title, String label, String field1Value, String field2Value) {
        JPanel panelTmp = new JPanel();
        panelTmp.setLayout(new BoxLayout(panelTmp, BoxLayout.Y_AXIS));
        JLabel jlabel = new JLabel(label);
        panelTmp.add(jlabel);
        JTextField textField1 = new JTextField(field1Value);
        panelTmp.add(textField1);
        JTextField textField2 = null;
        if (field2Value != null) {
            textField2 = new JTextField(field2Value);
            panelTmp.add(textField2);
        }
        if (JOptionPane.showConfirmDialog(dummyFrame, panelTmp, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
            executionContextTool.addMessage(textField1.getText());
            if (textField2 != null)
                executionContextTool.addMessage(textField2.getText());
        }

        /*
        Dialog2 dialog = new Dialog2(title, label, buttonName, field2Value != null);
        dialog.getTextField1().setText(field1Value);
        if (field2Value != null)
            dialog.getTextField2().setText(field2Value);
        try {
            dialog.setVisible(true);
            dialog.requestFocus();
            while (dialog.isVisible()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // e.printStackTrace();
                }
                if (executionContextTool.isNeedStop()) {
                    dialog.setVisible(false);
                    executionContextTool.addMessage("force stop server");
                    break;
                }
            }
            if (dialog.enterButton) {
                executionContextTool.addMessage(dialog.getTextField1().getText());
                if (field2Value != null)
                    executionContextTool.addMessage(dialog.getTextField2().getText());
            }
        } finally {
            dialog.dispose();
        }
        */
    }

    private JFrame createDummyFrame() {
        JFrame jf = new JFrame(String.format("Dialogs - %s", title));
        jf.setSize(1, 1);
        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jf.setUndecorated(true);
        jf.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        jf.setVisible(true);
        return jf;
    }

    private void showConfirmDialog(ExecutionContextTool executionContextTool, JFrame dummyFrame, String title, String message, int optionType) {
        executionContextTool.addMessage(JOptionPane.showConfirmDialog(dummyFrame, message, title, optionType));
    }

    private void showSelectDialog(ExecutionContextTool executionContextTool, JFrame dummyFrame, String title, String message) {
        JPanel panelTmp = new JPanel();
        panelTmp.setLayout(new BorderLayout());
        JLabel label = new JLabel(message);
        JComboBox<String> comboBox = new JComboBox<>();
        Stream.iterate(0, n -> n + 1)
                .limit(executionContextTool.countSource())
                .flatMap(i -> executionContextTool.getMessages(i).stream())
                .flatMap(a -> a.getMessages().stream())
                .map(m -> m.getValue().toString())
                .forEach(comboBox::addItem);
        if (comboBox.getItemCount() <= 0) {
            executionContextTool.addError("wrong input");
            return;
        }
        comboBox.setSelectedIndex(0);
        panelTmp.add(label, BorderLayout.NORTH);
        panelTmp.add(comboBox, BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(dummyFrame, panelTmp, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION && comboBox.getSelectedItem() != null)
            executionContextTool.addMessage(java.util.List.of(comboBox.getSelectedItem()));
    }

    private void fileChooser(ExecutionContextTool executionContextTool, JFrame dummyFrame, boolean save, int fileSelectionMode, boolean multiSelection) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(fileSelectionMode);
        fileChooser.setMultiSelectionEnabled(multiSelection);

        int result = -1;
        if (save) {
            result = fileChooser.showSaveDialog(dummyFrame);
        } else {
            result = fileChooser.showOpenDialog(dummyFrame);
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File files[] = multiSelection ? fileChooser.getSelectedFiles() : new File[]{fileChooser.getSelectedFile()};
            if (files != null)
                Arrays.stream(files).forEach(f -> executionContextTool.addMessage(f.getAbsolutePath()));
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        title = null;
        // buttonName = null;
        // showSecondField = null;
    }

}
