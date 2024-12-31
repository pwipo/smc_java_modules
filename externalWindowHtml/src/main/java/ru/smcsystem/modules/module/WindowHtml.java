package ru.smcsystem.modules.module;

import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WindowHtml implements Module {
    private MainForm mainForm;

    private enum OperationType {
        SERVER, GET_VALUE, SET_VALUE, GET_ELEMENT, SET_ELEMENT, SET_ATTRIBUTE, COUNT_CHILDS, ADD_CHILD_ELEMENT, REMOVE_CHILD_ELEMENT, SET_CHILD_ATTRIBUTE, SET_POSITION,
        SET_INNER_HTML, SET_SELECT_OPTIONS
    }

    private String configuration;
    private List<String> ids;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        configuration = (String) configurationTool.getSetting("configuration").orElseThrow(() -> new ModuleException("configuration setting")).getValue();
        Integer width = (Integer) configurationTool.getSetting("width").orElseThrow(() -> new ModuleException("width setting")).getValue();
        Integer height = (Integer) configurationTool.getSetting("height").orElseThrow(() -> new ModuleException("height setting")).getValue();
        String title = (String) configurationTool.getSetting("title").orElseThrow(() -> new ModuleException("title setting")).getValue();
        String idsStr = (String) configurationTool.getSetting("ids").orElseThrow(() -> new ModuleException("ids setting")).getValue();
        ids = Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        mainForm = new MainForm(title, configuration, width, height);
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (mainForm == null) {
            executionContextTool.addError("window not exist");
            return;
        }
        OperationType operationType = OperationType.valueOf(executionContextTool.getType().toUpperCase());
        if (operationType != OperationType.SERVER && !mainForm.frame.isVisible()) {
            executionContextTool.addError("window not started");
            return;
        }
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (id, messages) -> {
            switch (operationType) {
                case SERVER: {
                    if (mainForm.frame.isVisible()) {
                        executionContextTool.addError("window already started");
                        break;
                    }
                    mainForm.prepare(configurationTool, executionContextTool);
                    if (ids != null && !ids.isEmpty()) {
                        configurationTool.loggerTrace(String.format("window started, count ids=%d, ecs=%d", ids.size(), executionContextTool.getFlowControlTool().countManagedExecutionContexts()));
                        for (int i = 0; i < ids.size(); i++) {
                            String idStr = ids.get(i);
                            if (i >= executionContextTool.getFlowControlTool().countManagedExecutionContexts())
                                break;
                            mainForm.addEC(idStr, i);
                        }
                    }
                    mainForm.start();
                    configurationTool.loggerDebug(String.format("window started, count ids=%d", ids != null ? ids.size() : 0));
                    do {
                        try {
                            Thread.sleep(500);
                        } catch (Exception ignore) {
                        }
                    } while (mainForm.frame.isVisible() && !executionContextTool.isNeedStop());
                    mainForm.clean();
                    configurationTool.loggerDebug("window stopped");
                    break;
                }
                case GET_VALUE:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .flatMap(idStr -> mainForm.getValue(idStr))
                            .ifPresent(v -> executionContextTool.addMessage(List.of(v)));
                    break;
                case SET_VALUE:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.setValue(idStr, messages.get(0).poll().getValue())));
                    break;
                case GET_ELEMENT:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .flatMap(idStr -> mainForm.getElement(idStr))
                            .ifPresent(executionContextTool::addMessage);
                    break;
                case SET_ELEMENT:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.setElement(idStr, ModuleUtils.getString(messages.get(0).poll()))));
                    break;
                case SET_ATTRIBUTE:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.setAttribute(idStr, ModuleUtils.getString(messages.get(0).poll()), messages.get(0).poll().getValue())));
                    break;
                case COUNT_CHILDS:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> executionContextTool.addMessage(mainForm.countChilds(idStr)));
                    break;
                case ADD_CHILD_ELEMENT:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.addChildElement(idStr, ModuleUtils.getString(messages.get(0).poll()), ModuleUtils.getNumber(messages.get(0).poll()).intValue())));
                    break;
                case REMOVE_CHILD_ELEMENT:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.removeChildElement(idStr, ModuleUtils.getNumber(messages.get(0).poll()).intValue())));
                    break;
                case SET_CHILD_ATTRIBUTE:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.setChildAttribute(idStr, ModuleUtils.getString(messages.get(0).poll()), messages.get(0).poll().getValue(), ModuleUtils.getNumber(messages.get(0).poll()).intValue())));
                    break;
                case SET_POSITION:
                    Optional.ofNullable(ModuleUtils.getNumber(messages.get(0).poll()))
                            .map(Number::intValue)
                            .ifPresent(position -> SwingUtilities.invokeLater(() -> {
                                int positionTmp = position;
                                // int height = mainForm.editorPane.getBounds(null).height;
                                int length = mainForm.editorPane.getDocument().getLength();
                                if (positionTmp < 0)
                                    positionTmp = Math.max(0, length + positionTmp);
                                if (positionTmp > length)
                                    positionTmp = length - 1;
                                // mainForm.scrollPane.scrollRectToVisible(new Rectangle(0, positionTmp, 1, 1));
                                mainForm.editorPane.setCaretPosition(positionTmp);
                            }));
                    break;
                case SET_INNER_HTML:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() ->
                                    mainForm.setInnerHtml(idStr, ModuleUtils.getString(messages.get(0).poll()))));
                    break;
                case SET_SELECT_OPTIONS:
                    Optional.ofNullable(ModuleUtils.getString(messages.get(0).poll()))
                            .ifPresent(idStr -> SwingUtilities.invokeLater(() -> {
                                List<String> strings = messages.get(0).stream().map(ModuleUtils::toString).collect(Collectors.toList());
                                mainForm.setValue(idStr, strings);
                            }));
                    break;
            }
        });
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        if (mainForm != null) {
            JFrame frame = mainForm.frame;
            if (frame.isVisible())
                frame.setVisible(false);
            SwingUtilities.invokeLater(frame::dispose);
            mainForm = null;
        }
        ids = null;
    }

}
