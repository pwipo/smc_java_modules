package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.ICommand;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
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
        SELECT,
        CHAT,
    }

    private Type type;
    private String title;
    private String message;
    // private String buttonName;
    // private Boolean showSecondField;
    private Map<String, String> dictionary;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = configurationTool.getSetting("type").map(ModuleUtils::toString).map(Type::valueOf).orElseThrow(() -> new ModuleException("type setting"));
        title = configurationTool.getSetting("title").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("title setting"));
        message = configurationTool.getSetting("message").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("message setting"));

        String lang = configurationTool.getSetting("lang").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("lang setting"));
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
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (id, messagesAll) -> {
            // String externalMessage = Stream.iterate(0, n -> n + 1)
            //         .limit(executionContextTool.countSource())
            //         .flatMap(i -> executionContextTool.getMessages(i).stream())
            //         .flatMap(a -> a.getMessages().stream())
            //         .map(m -> m.getValue().toString())
            //         .collect(Collectors.joining());
            List<String> inputLst = messagesAll.stream()
                    .flatMap(Collection::stream)
                    .map(ModuleUtils::toString)
                    .collect(Collectors.toList());
            String externalMessage = String.join("", inputLst);

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
                    case CHAT:
                        showChatDialog(executionContextTool, title, message, inputLst, "Add", "Clear");
                        break;
                }
            } finally {
                dummyFrame.dispose();
            }
        });
    }

    public static boolean isData(IMessage message) {
        return message != null && message.getMessageType() == MessageType.DATA;
    }

    public static boolean isError(IMessage message) {
        return message != null && (message.getMessageType() == MessageType.ACTION_ERROR || message.getMessageType() == MessageType.ERROR);
    }

    public static Optional<IAction> getActionWithDataOrErrorMessages(List<IAction> actions) {
        return actions.stream()
                .filter((a) -> ModuleUtils.hasErrors(a) || (a.getType() == ActionType.EXECUTE && a.getMessages().stream().anyMatch(Dialogs::isData)))
                .findFirst();
    }

    public static Optional<ICommand> getLastCommand(List<ICommand> commands) {
        return commands.stream().reduce((first, second) -> second);
    }

    public static Optional<List<IMessage>> getDataOrErrorMessagesFromLastCommand(List<ICommand> commands) {
        return getLastCommand(commands)
                .flatMap(c -> getActionWithDataOrErrorMessages(c.getActions()))
                .map(IAction::getMessages)
                .map(l -> l.stream().filter(m -> isData(m) || isError(m)).collect(Collectors.toList()));
    }

    private void showChatDialog(ExecutionContextTool executionContextTool, String title, String message,
                                List<String> inputLst, String nameButtonAdd, String nameButtonClear) {
        String inputLabel = inputLst != null && !inputLst.isEmpty() ? inputLst.get(0) : "";
        List<String> paths = inputLst != null && inputLst.size() > 1 ? inputLst.subList(1,  inputLst.size()) : null;
        DialogChat dialogChat = new DialogChat(title, message, inputLabel, nameButtonAdd, nameButtonClear, paths,
                executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0 ?
                        str -> {
                            long threadId = ModuleUtils.executeParallel(executionContextTool, 0, java.util.List.of(str));
                            List<ICommand> data = executionContextTool.getFlowControlTool().getCommandsFromExecuted(threadId, 0);
                            executionContextTool.getFlowControlTool().releaseThread(threadId);
                            return getDataOrErrorMessagesFromLastCommand(data);
                        } :
                        null);
        try {
            dialogChat.frame.setVisible(true);
            dialogChat.frame.requestFocus();
            do {
                try {
                    Thread.sleep(500);
                } catch (Exception ignored) {
                    // e.printStackTrace();
                }
            } while (dialogChat.frame.isVisible() && !executionContextTool.isNeedStop());
            if (dialogChat.frame.isVisible()) {
                dialogChat.frame.setVisible(false);
                executionContextTool.addMessage("force stop server");
            }
        } finally {
            dialogChat.frame.dispose();
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
