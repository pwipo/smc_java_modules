package ru.smcsystem.modules.module;

import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.swing.*;
import java.util.*;
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
        configuration = configurationTool.getSetting("configuration").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("configuration setting"));
        Integer width = configurationTool.getSetting("width").map(ModuleUtils::toNumber).map(Number::intValue).orElseThrow(() -> new ModuleException("width setting"));
        Integer height = configurationTool.getSetting("height").map(ModuleUtils::toNumber).map(Number::intValue).orElseThrow(() -> new ModuleException("height setting"));
        String title = configurationTool.getSetting("title").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("title setting"));
        String idsStr = configurationTool.getSetting("ids").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("ids setting"));
        ids = Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        String shapeId = configurationTool.getSetting("shapeId").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("shapeId setting"));
        Map<String, byte[]> mapImages = new HashMap<>();
        if (!shapeId.isBlank()) {
            // configurationTool.loggerTrace("start shape generator");
            List<Shape> shapes = configurationTool.getInfo("decorationShapes").map(ModuleUtils::getObjectArray)
                    .filter(ModuleUtils::isArrayContainObjectElements)
                    .map(a -> ModuleUtils.convertFromObjectArray(a, Shape.class, true, true))
                    .orElse(List.of());
            // configurationTool.loggerTrace("count shaps=" + shapes.size());
            Shape shape = shapes.stream().filter(s -> Objects.equals(s.getName(), shapeId)).findFirst().orElse(null);
            if (shape != null) {
                // try {
                List<ShapeHtmlElement> elements = shapes.stream()
                        .filter(s -> Objects.equals(s.getParentName(), shapeId))
                        // .sorted(Comparator.comparing(s -> s.getX() * s.getY()))
                        .sorted((s1, s2) -> Math.abs(s1.getY() - s2.getY()) < 6 ? s1.getX().compareTo(s2.getX()) : s1.getY().compareTo(s2.getY()))
                        .map(s -> new ShapeHtmlElement(s, shapes))
                        .filter(s -> s.getType() != null)
                        .collect(Collectors.toList());
                configurationTool.loggerTrace("count root shapes " + elements.size());
                elements.stream()
                        .flatMap(e -> e.getAllElements().stream())
                        .filter(e -> e.getBytes() != null)
                        .forEach(e -> mapImages.put(e.getName(), e.getBytes()));
                String bodyChildsHtml = elements.stream()
                        .map(ShapeHtmlElement::genHtml)
                        .collect(Collectors.joining("\n"));
                if (!bodyChildsHtml.isBlank()) {
                    if (!configuration.isBlank() && configuration.contains("<body>")) {
                        int indexOf = configuration.indexOf("<body>");
                        if (indexOf != -1) {
                            configuration = configuration.substring(0, indexOf);
                        }
                        configuration = configuration + String.format("<body>\n%s\n</body>\n</html>", bodyChildsHtml);
                    } else {
                        configuration = String.format("<html><head></head><body>\n%s\n</body>\n</html>", bodyChildsHtml);
                    }
                    configurationTool.loggerTrace(configuration);
                }
                // } catch (Throwable e) {
                //     configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                //     throw e;
                // }
            }
        }
        mainForm = new MainForm(title, configuration, width, height, mapImages);
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
