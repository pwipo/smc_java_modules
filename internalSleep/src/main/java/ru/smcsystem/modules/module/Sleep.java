package ru.smcsystem.modules.module;

import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.Calendar;

public class Sleep implements Module {

    enum Type {
        FIXED,
        FLEXIBLE,
        NEW_MINUTE,
        NEW_HOUR
    }

    private Type type;
    private Long interval;
    private long lastTime;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("text setting")).getValue());
        interval = (Long) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("value setting")).getValue();
        lastTime = 0;
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        long startTime = System.currentTimeMillis();
        try {
            switch (type) {
                case FIXED:
                    Thread.sleep(interval);
                    lastTime = System.currentTimeMillis();
                    break;
                case FLEXIBLE: {
                    lastTime = sleepFlex();
                    break;
                }
                case NEW_MINUTE: {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MINUTE, interval.intValue());
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 1);
                    Thread.sleep(cal.getTimeInMillis() - startTime);
                    lastTime = System.currentTimeMillis();
                    break;
                }
                case NEW_HOUR: {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.HOUR, interval.intValue());
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 1);
                    Thread.sleep(cal.getTimeInMillis());
                    lastTime = System.currentTimeMillis();
                    break;
                }
            }
        } catch (InterruptedException e) {
            if (!executionContextTool.isNeedStop())
                e.printStackTrace();
        }
        executionContextTool.addMessage(lastTime - startTime);
    }

    private synchronized long sleepFlex() throws InterruptedException {
        if (lastTime == 0)
            lastTime = System.currentTimeMillis();
        Thread.sleep(interval - (System.currentTimeMillis() - lastTime));
        return System.currentTimeMillis();
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        interval = null;
        lastTime = 0;
    }

}
