package ru.smcsystem.modules.internalSymbolsGenerator;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SymbolsGenerator implements Module {

    // private Integer type;
    // private Integer size;
    private Integer sizeNumber;
    private Integer sizeAlphaUp;
    private Integer sizeAlphaLow;
    private Integer sizeNonAlphaNum;
    private Random rnd;

    static private final int intTypeNumber = 1;
    static private final int intTypeAlphaUp = 2;
    static private final int intTypeAlphaLow = 3;
    static private final int intTypeAlphaAll = 4;
    static private final int intTypeNumAlpha = 5;
    static private final int intTypeChars = 6;

    final private char[] charNonAlphaNum = {33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 58, 59, 60, 61, 62, 63, 64, 91, 92, 93, 94, 95, 96, 123, 124, 125, 126};

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        // type = (Integer) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue();
        // size = (Integer) configurationTool.getSetting("size").orElseThrow(() -> new ModuleException("size setting")).getValue();
        sizeNumber = (Integer) configurationTool.getSetting("sizeNumber").orElseThrow(() -> new ModuleException("sizeNumber setting")).getValue();
        sizeAlphaUp = (Integer) configurationTool.getSetting("sizeAlphaUp").orElseThrow(() -> new ModuleException("sizeAlphaUp setting")).getValue();
        sizeAlphaLow = (Integer) configurationTool.getSetting("sizeAlphaLow").orElseThrow(() -> new ModuleException("sizeAlphaLow setting")).getValue();
        sizeNonAlphaNum = (Integer) configurationTool.getSetting("sizeNonAlphaNum").orElseThrow(() -> new ModuleException("NonAlphaNum setting")).getValue();
        rnd = new Random();
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.countSource() > 0) {
            for (int i = 0; i < executionContextTool.countSource(); i++) {
                executionContextTool.getMessages(i).stream()
                        .filter(a -> a.getMessages().size() > 3)
                        .forEach(a -> {
                            for (int j = 0; j < a.getMessages().size(); j = j + 4) {
                                executionContextTool.addMessage(
                                        /*
                                        genString(
                                                rnd,
                                                ((Number) a.getMessages().get(j).getValue()).intValue(),
                                                ((Number) a.getMessages().get(j + 1).getValue()).intValue())
                                        */
                                        genString(
                                                rnd,
                                                ((Number) a.getMessages().get(j).getValue()).intValue(),
                                                ((Number) a.getMessages().get(j + 1).getValue()).intValue(),
                                                ((Number) a.getMessages().get(j + 2).getValue()).intValue(),
                                                ((Number) a.getMessages().get(j + 3).getValue()).intValue())
                                );
                            }
                        });
            }
        } else
            executionContextTool.addMessage(/*genString(rnd, type, size)*/genString(rnd, sizeNumber, sizeAlphaUp, sizeAlphaLow, sizeNonAlphaNum));
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        // type = null;
        // size = null;
        sizeNumber = null;
        sizeAlphaUp = null;
        sizeAlphaLow = null;
        sizeNonAlphaNum = null;
    }

    private String genString(Random rnd, int sizeNumber, int sizeAlphaUp, int sizeAlphaLow, int sizeNonAlphaNum) throws ModuleException {
        StringBuilder result = new StringBuilder();
        if (sizeNumber > 0) {
            for (int i = 0; i < sizeNumber; i++)
                result.append(randomNumCharacter(rnd));
        }
        if (sizeAlphaUp > 0) {
            for (int i = 0; i < sizeAlphaUp; i++)
                result.append(randomAlphaUpperCharacter(rnd));
        }
        if (sizeAlphaLow > 0) {
            for (int i = 0; i < sizeAlphaLow; i++)
                result.append(randomAlphaLowerCharacter(rnd));
        }
        if (sizeNonAlphaNum > 0) {
            for (int i = 0; i < sizeNonAlphaNum; i++)
                result.append(randomNonAlphaNumCharacter(rnd));
        }
        //shuffle
        // List<char[]> chars = Arrays.asList(result.toString().split(""));
        List<Character> chars = new ArrayList<>();
        for (char c : result.toString().toCharArray())
            chars.add(c);
        Collections.shuffle(chars, rnd);
        return StringUtils.join(chars, "");
    }

    private String genString(Random rnd, int type, int size) throws ModuleException {
        StringBuilder result = new StringBuilder();

        switch (type) {
            case intTypeNumber:
                for (int i = 0; i < size; i++)
                    result.append(randomNumCharacter(rnd));
                //result += Math.round(rnd.nextFloat()*10);
                break;
            case intTypeAlphaUp:
                for (int i = 0; i < size; i++)
                    result.append(randomAlphaUpperCharacter(rnd));
                break;
            case intTypeAlphaLow:
                for (int i = 0; i < size; i++)
                    result.append(randomAlphaLowerCharacter(rnd));
                break;
            case intTypeAlphaAll:
                for (int i = 0; i < size; i++) {
                    int intTmp = rnd.nextInt(2);
                    if (intTmp == 0) {
                        result.append(randomAlphaLowerCharacter(rnd));
                    } else {
                        result.append(randomAlphaUpperCharacter(rnd));
                    }
                }
                break;
            case intTypeNumAlpha:
                for (int i = 0; i < size; i++) {
                    int intTmp = rnd.nextInt(3);
                    if (intTmp == 0) {
                        result.append(randomAlphaLowerCharacter(rnd));
                    } else if (intTmp == 1) {
                        result.append(randomAlphaUpperCharacter(rnd));
                    } else {
                        result.append(randomNumCharacter(rnd));
                    }
                }
                break;
            case intTypeChars:
                for (int i = 0; i < size; i++) {
                    int intTmp = rnd.nextInt(4);
                    if (intTmp == 0) {
                        result.append(randomAlphaLowerCharacter(rnd));
                    } else if (intTmp == 1) {
                        result.append(randomAlphaUpperCharacter(rnd));
                    } else if (intTmp == 2) {
                        result.append(randomNumCharacter(rnd));
                    } else {
                        result.append(randomNonAlphaNumCharacter(rnd));
                    }
                }
                break;
            default:
                throw new ModuleException("wrong type " + type);
        }

        return result.toString();
    }

    private char randomAlphaUpperCharacter(Random r) {
        return (char) (65 + r.nextInt(26));
    }

    private char randomAlphaLowerCharacter(Random r) {
        return (char) (97 + r.nextInt(26));
    }

    private char randomNumCharacter(Random r) {
        return (char) (48 + r.nextInt(10));
    }

    private char randomNonAlphaNumCharacter(Random r) {
        return charNonAlphaNum[r.nextInt(charNonAlphaNum.length)];
    }

}
