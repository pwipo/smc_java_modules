package ru.smcsystem.smcmodules.module;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Window implements Module {

    private MainForm mainForm;

    private enum Mode {
        ACTIVE,
        PASSIVE
    }

    private Mode mode;
    private volatile boolean started;
    private volatile boolean stop;
    private ConcurrentLinkedQueue<Object> actions;
    private Boolean getPressedKeys;
    private Boolean printElements;
    private Integer sleepTime;
    private Boolean useButtonEvents;
    private Boolean useMenuEvents;
    private Boolean useListSelectionEvents;
    private Boolean useTreeSelectionEvents;
    private String defaultButton;

    private Set<Integer> pressedKeys;
    private Set<Integer> releasedKeys;

    private Consumer<Object> func;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        mode = Mode.valueOf((String) configurationTool.getSetting("mode").orElseThrow(() -> new ModuleException("mode setting")).getValue());
        String configuration = (String) configurationTool.getSetting("configuration").orElseThrow(() -> new ModuleException("configuration setting")).getValue();
        getPressedKeys = Boolean.valueOf((String) configurationTool.getSetting("getPressedKeys").orElseThrow(() -> new ModuleException("getPressedKeys setting")).getValue());
        printElements = Boolean.valueOf((String) configurationTool.getSetting("printElements").orElseThrow(() -> new ModuleException("printElements setting")).getValue());
        sleepTime = (Integer) configurationTool.getSetting("sleep").orElseThrow(() -> new ModuleException("sleep setting")).getValue();
        useButtonEvents = Boolean.valueOf((String) configurationTool.getSetting("useButtonEvents").orElseThrow(() -> new ModuleException("useButtonEvents setting")).getValue());
        useMenuEvents = Boolean.valueOf((String) configurationTool.getSetting("useMenuEvents").orElseThrow(() -> new ModuleException("useMenuEvents setting")).getValue());
        useListSelectionEvents = Boolean.valueOf((String) configurationTool.getSetting("useListSelectionEvents").orElseThrow(() -> new ModuleException("useListSelectionEvents setting")).getValue());
        useTreeSelectionEvents = Boolean.valueOf((String) configurationTool.getSetting("useTreeSelectionEvents").orElseThrow(() -> new ModuleException("useTreeSelectionEvents setting")).getValue());
        defaultButton = (String) configurationTool.getSetting("defaultButton").orElseThrow(() -> new ModuleException("defaultButton setting")).getValue();

        String lang = (String) configurationTool.getSetting("lang").orElseThrow(() -> new ModuleException("lang setting")).getValue();
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
                configuration = StringUtils.replace(configuration, searchKey, strValue);
            }
        }

        try {
            mainForm = new MainForm(configuration);
            mainForm.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        } catch (Exception e) {
            throw new ModuleException(e.getMessage(), e);
        }
        started = false;
        stop = false;
        actions = new ConcurrentLinkedQueue<>();

        mainForm.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                stopWindow();
            }
        });

        if (getPressedKeys) {
            releasedKeys = new HashSet<>(200);
            pressedKeys = new HashSet<>(200);

            mainForm.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    pressedKeys.remove(e.getKeyCode());
                    releasedKeys.add(e.getKeyCode());
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    // if(!releasedKeys.contains(e.getKeyCode()))
                    pressedKeys.add(e.getKeyCode());
                }

                @Override
                public void keyTyped(KeyEvent e) {
                    super.keyTyped(e);
                }
            });
            mainForm.setFocusable(true);
            mainForm.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    pressedKeys.clear();
                    releasedKeys.clear();
                }

                @Override
                public void focusGained(FocusEvent e) {
                    pressedKeys.clear();
                    releasedKeys.clear();
                }
            });
        }
        func = null;

    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (stop) {
            executionContextTool.addError("server already stopped");
            return;
        }
        switch (mode) {
            case ACTIVE:
                if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0) {
                    if (!started) {
                        executionContextTool.addError("not started");
                        return;
                    }
                    if (executionContextTool.countSource() > 0) {
                        Stream.iterate(0, n -> n + 1)
                                .limit(executionContextTool.countSource())
                                .flatMap(n -> executionContextTool.getMessages(n).stream())
                                .map(IAction::getMessages)
                                .forEach(msgs -> executeCommands(configurationTool, executionContextTool, msgs));
                    } else {
                        // executionContextTool.addError("configuration already started");
                        stopWindow();
                    }
                } else {
                    if (started) {
                        executionContextTool.addError("already started");
                        return;
                    }

                    startWindow();

                    // try {
                    while (!stop) {
                        synchronized (this) {
                            try {
                                this.wait(sleepTime);
                                // Thread.sleep(sleepTime);
                            } catch (Exception e) {
                            }
                        }
                        if (executionContextTool.isNeedStop()) {
                            executionContextTool.addMessage("force stop server");
                            break;
                        }

                        // try {
                        while (!actions.isEmpty()) {
                            Object action = actions.poll();
                            processAction(configurationTool, executionContextTool, action);
                        }
                        if (getPressedKeys) {
                            Set<Integer> keys = printKeys();
                            if (!keys.isEmpty())
                                keys.forEach(key -> processAction(configurationTool, executionContextTool, key));
                        }
                        /*
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        */
                    }
                    // } finally {
                    stopWindow();
                    // }
                    stop = true;
                }
                break;
            case PASSIVE:
                if (!started) {
                    startWindow();
                    executionContextTool.addMessage("started");
                    return;
                }

                if (getPressedKeys) {
                    Set<Integer> keys = printKeys();
                    // executionContextTool.addMessage(keys.size());
                    if (!keys.isEmpty()) {
                        keys.forEach(actions::add);
                    }
                }

                if (!actions.isEmpty()) {
                    HashSet<Object> actionsTmp = new HashSet<>(actions);
                    actions.clear();
                    executionContextTool.addMessage(actionsTmp.size());
                    executionContextTool.addMessage(new ArrayList<>(actionsTmp));
                }

                if (printElements) {
                    Map<String, Object> idMap = mainForm.getEngine().getIdMap();
                    executionContextTool.addMessage(idMap.size());
                    idMap.forEach((k, v) -> {
                        executionContextTool.addMessage(k);
                        Object value = mainForm.getValue((Component) v);
                        executionContextTool.addMessage(List.of(value != null ? value : ""));
                    });
                }

                if (executionContextTool.countSource() > 0) {
                    Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .map(IAction::getMessages)
                            .forEach(msgs -> executeCommands(configurationTool, executionContextTool, msgs));
                }
                break;
        }
    }

    private void consumeAction(Object action) {
        /*
        if (func == null) {
            actions.add(action);
        } else {
            func.accept(action);
        }
        */
        actions.add(action);
        synchronized (this) {
            notifyAll();
        }
    }

    private void processAction(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, Object action) {
        LinkedList<Object> args = new LinkedList<>();
        if (action instanceof List) {
            args.addAll((List) action);
        } else if (action != null) {
            args.add(action);
        }
        if (printElements) {
            Map<String, Object> idMap = mainForm.getEngine().getIdMap();
            args.add(idMap.size());
            idMap.forEach((k, v) -> {
                args.add(k);
                Object value = mainForm.getValue((Component) v);
                if (value != null) {
                    args.add(value);
                }
            });
        }
        executeChild(configurationTool, executionContextTool, args);
    }

    private void stopWindow() {
        stop = true;
        synchronized (this) {
            this.notifyAll();
        }
        if (mainForm != null && mainForm.isVisible())
            mainForm.setVisible(false);
    }

    private void executeChild(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, LinkedList<Object> args) {
        if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0)
            return;
        long threadId = executionContextTool.getFlowControlTool().executeParallel(
                CommandType.EXECUTE,
                List.of(0),
                args,
                0,
                0);
        List<List<IMessage>> response = null;
        try {
            do {
                Thread.sleep(10);
            } while (!executionContextTool.isNeedStop() && executionContextTool.getFlowControlTool().isThreadActive(threadId));
            response = executionContextTool.getFlowControlTool().getMessagesFromExecuted(threadId, 0).stream()
                    .map(IAction::getMessages)
                    // .flatMap(a -> a.getMessages().stream().map(IValue::getValue))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ex.printStackTrace();
            // throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            executionContextTool.getFlowControlTool().releaseThread(threadId);
        }
        if (response != null)
            response.forEach(msgs -> executeCommands(configurationTool, executionContextTool, msgs));
    }

    private void executeCommands(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<IMessage> msgs) {
        if (CollectionUtils.isEmpty(msgs))
            return;

        if (stop)
            return;

        LinkedList<IMessage> messages = new LinkedList<>(msgs);
        try {
            while (!messages.isEmpty()) {
                int type = getNumber(messages.poll(), -1).intValue();
                switch (type) {
                    case 0: {
                        //close
                        stopWindow();
                        break;
                    }
                    case 1: {
                        //get value
                        String elementId = getText(messages.poll(), type);
                        Component component = mainForm.findElement(elementId);
                        Object value = mainForm.getValue(component);
                        if (value != null)
                            executionContextTool.addMessage(List.of(value));
                        break;
                    }
                    case 2: {
                        //set value
                        String elementId = getText(messages.poll(), type);

                        IMessage message = messages.poll();
                        if (message == null)
                            throw new Exception(String.format("need value for type %d", type));
                        Object value = message.getValue();
                        mainForm.setValue(elementId, value);
                        break;
                    }
                    case 3: {
                        //set enable
                        mainForm.elementEnable(
                                getText(messages.poll(), type),
                                getNumber(messages.poll(), type).intValue() > 0);
                        break;
                    }
                    case 4: {
                        //set visible
                        mainForm.elementVisible(
                                getText(messages.poll(), type),
                                getNumber(messages.poll(), type).intValue() > 0);
                        break;
                    }
                    case 5: {
                        // highlight words
                        String elementId = getText(messages.poll(), type);
                        List<String> words = new LinkedList<>();
                        while (!messages.isEmpty()) {
                            words.add(getText(messages.poll(), type));
                        }
                        mainForm.textAreaHighlight(
                                elementId,
                                words,
                                true
                        );
                        break;
                    }
                    case 6: {
                        //get selected text
                        String str = mainForm.textComponentGetSelectedText(getText(messages.poll(), type));
                        if (str == null)
                            str = "";
                        executionContextTool.addMessage(str);
                        break;
                    }
                    case 7: {
                        //replace selected text
                        mainForm.textComponentReplaceSelectedText(
                                getText(messages.poll(), type),
                                getText(messages.poll(), type));
                        break;
                    }
                    case 8: {
                        //insert into current cursor position
                        mainForm.textComponentInsertIntoCurrentCursorPosition(
                                getText(messages.poll(), type),
                                getText(messages.poll(), type));
                        break;
                    }
                    case 9: {
                        //replace all
                        mainForm.textComponentReplaceAll(
                                getText(messages.poll(), type),
                                getText(messages.poll(), type),
                                getText(messages.poll(), type));
                        break;
                    }
                    case 10: {
                        //list set all
                        String id = getText(messages.poll(), type);
                        List<Object> values = messages.stream()
                                .map(IValue::getValue)
                                .collect(Collectors.toList());
                        messages.clear();
                        mainForm.setListElements(
                                id,
                                values);
                        break;
                    }
                    case 11: {
                        //list add
                        mainForm.addListElement(
                                getText(messages.poll(), type),
                                messages.poll().getValue());
                        break;
                    }
                    case 12: {
                        //list remove
                        mainForm.removeListElement(
                                getText(messages.poll(), type),
                                getNumber(messages.poll(), type).intValue());
                        break;
                    }
                    case 13: {
                        //list get selected
                        executionContextTool.addMessage(mainForm.getListSelected(getText(messages.poll(), type)));
                        break;
                    }
                    case 14: {
                        //replace all
                        mainForm.textComponentEditable(
                                getText(messages.poll(), type),
                                getNumber(messages.poll(), type).intValue() > 0);
                        break;
                    }
                    case 15: {
                        // highlight words
                        String elementId = getText(messages.poll(), type);
                        List<String> words = new LinkedList<>();
                        while (!messages.isEmpty()) {
                            words.add(getText(messages.poll(), type));
                        }
                        mainForm.textAreaHighlight(
                                elementId,
                                words,
                                false
                        );
                        break;
                    }
                }
            }
        } catch (Exception e) {
            executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
            configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
        }
    }

    private Number getNumber(IMessage m, int type) throws Exception {
        if (m == null || !(ValueType.BYTE.equals(m.getType()) || ValueType.SHORT.equals(m.getType()) || ValueType.INTEGER.equals(m.getType()) || ValueType.LONG.equals(m.getType()) || ValueType.FLOAT.equals(m.getType()) || ValueType.DOUBLE.equals(m.getType()) || ValueType.BIG_INTEGER.equals(m.getType()) || ValueType.BIG_DECIMAL.equals(m.getType())))
            throw new Exception(String.format("wrong type for %d", type));
        return ((Number) m.getValue());
    }

    private String getText(IMessage m, int type) throws Exception {
        if (m == null || !ValueType.STRING.equals(m.getType()))
            throw new Exception(String.format("wrong type for %d", type));
        return ((String) m.getValue());
    }

    private void startWindow() {
        if (started)
            return;
        mainForm.setActions(
                mainForm,
                useButtonEvents || useMenuEvents ? e -> {
                    // if (e.getSource() instanceof JButton || e.getSource() instanceof JMenuItem) {
                    if (e.getSource() instanceof JButton && !useButtonEvents)
                        return;
                    if (e.getSource() instanceof JMenuItem && !useMenuEvents)
                        return;
                    Component component = (Component) e.getSource();
                    String name = component.getName();
                    if (name == null)
                        name = "";
                    consumeAction(name);
                    // }
                } : null,
                useListSelectionEvents ? e -> {
                    JList component = (JList) e.getSource();
                    if (e.getValueIsAdjusting())
                        return;
                    List selectedValuesList = component.getSelectedValuesList();
                    String name = component.getName();
                    if (name == null)
                        name = "";
                    if (selectedValuesList != null && !selectedValuesList.isEmpty()) {
                        LinkedList<Object> list = new LinkedList<>();
                        list.add(name);
                        list.add(selectedValuesList.size());
                        list.addAll(selectedValuesList);
                        consumeAction(list);
                    }
                } : null,
                useTreeSelectionEvents ? e -> {
                    JTree component = (JTree) e.getSource();
                    TreePath[] selectionPaths = component.getSelectionPaths();
                    String name = component.getName();
                    if (name == null)
                        name = "";
                    if (selectionPaths != null) {
                        LinkedList<Object> list = new LinkedList<>();
                        list.add(name);
                        list.add(selectionPaths.length);
                        list.addAll(
                                Arrays.stream(selectionPaths)
                                        .map(el -> ((DefaultMutableTreeNode) el.getLastPathComponent()).getUserObject().toString())
                                        .collect(Collectors.toList()));
                        consumeAction(list);
                    }
                } : null
        );
        started = true;

        if (StringUtils.isNotBlank(defaultButton))
            mainForm.getRootPane().setDefaultButton((JButton) mainForm.findElement(defaultButton));
        mainForm.setVisible(true);
        mainForm.requestFocus();
        mainForm.requestFocusInWindow();
        mainForm.toFront();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                mainForm.requestFocus();
                mainForm.requestFocusInWindow();
                mainForm.toFront();
            });
        }).start();
    }

    private Set<Integer> printKeys() {
        Set<Integer> keys = new HashSet<>(releasedKeys);
        releasedKeys.clear();
        // releasedKeys.addAll(pressedKeys);
        // pressedKeys.clear();
        keys.addAll(pressedKeys);

        /*
        executionContextTool.addMessage(keys.size());
        if (!keys.isEmpty()) {
            keys.forEach(executionContextTool::addMessage);
        }
        */
        return keys;
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        func = null;
        mode = null;
        if (mainForm != null) {
            stopWindow();
            SwingUtilities.invokeLater(() -> {
                mainForm.dispose();
                mainForm = null;
            });
        }
        started = false;
        stop = false;
        if (actions != null) {
            actions.clear();
            // actions = null;
        }
        getPressedKeys = false;
        printElements = false;

        if (pressedKeys != null) {
            pressedKeys.clear();
            // pressedKeys = null;
        }
        if (releasedKeys != null) {
            releasedKeys.clear();
            // releasedKeys = null;
        }
        // sleepTime = null;
    }

}
