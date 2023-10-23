package ru.smcsystem.smcmodules.module;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class File implements Module {

    private final int intCountByte = 1000;

    private enum HashAlgType {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256");

        HashAlgType(String algName) {
            this.algName = algName;
        }

        private String algName;

        public String getAlgName() {
            return algName;
        }
    }

    private enum Type {
        info,
        fullInfo,
        treeInfo,
        treeFullInfo,
        copy,
        read,
        readText,
        tmpFolder,
        remove,
        dir,
        create,
        writePart,
        write,
        changeLastModified,
        mkdir,
        move,
        useArgument,
        readPart,
        readTextPart,
        pwd,
        dirNames,
        createPath,
        parent,
        rename,
        readTextNew,
        readNew,
        readTextLastNLines,
        waitFile,
        copyForce,
        writeText,
        writeTextBOM,
        dirSortDate,
        dirNamesSortDate,
        removeOrWait,
        readOrWait,
        writeOrWait
    }

    private Type type;
    private HashAlgType hashAlgType;
    private java.io.File workDirectory;
    private java.io.File rootFolder;
    private boolean isFile;
    private String arguments;
    private java.io.File paramFile;
    private Long paramLong;
    private Integer paramInt;
    private String paramStr;
    private Boolean printAbsolutePath;
    private Boolean useOnlyWorkDirectory;
    private long tmpLongVariable;
    private String tmpStrVariable;

    @Override
    public void start(ConfigurationTool externalConfigurationTool) throws ModuleException {
        type = Type.valueOf((String) externalConfigurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        hashAlgType = HashAlgType.valueOf((String) externalConfigurationTool.getSetting("hashAlgType").orElseThrow(() -> new ModuleException("type setting")).getValue());
        String rootFolderString = (String) externalConfigurationTool.getSetting("rootFolder").orElseThrow(() -> new ModuleException("rootFolder setting")).getValue();
        workDirectory = new java.io.File(externalConfigurationTool.getWorkDirectory());
        rootFolder = workDirectory;
        useOnlyWorkDirectory = Boolean.valueOf((String) externalConfigurationTool.getSetting("useOnlyWorkDirectory").orElseThrow(() -> new ModuleException("useOnlyWorkDirectory setting")).getValue());
        if (!useOnlyWorkDirectory && StringUtils.isNotBlank(rootFolderString)) {
            java.io.File file = new java.io.File(rootFolderString);
            if (file.isAbsolute()) {
                rootFolder = file;
            } else {
                rootFolder = new java.io.File(workDirectory, rootFolderString);
            }
        }
        isFile = rootFolder.exists() ? rootFolder.isFile() : StringUtils.isNotBlank(FilenameUtils.getExtension(rootFolderString));
        arguments = (String) externalConfigurationTool.getSetting("arguments").orElseThrow(() -> new ModuleException("arguments setting")).getValue();
        printAbsolutePath = Boolean.valueOf((String) externalConfigurationTool.getSetting("printAbsolutePath").orElseThrow(() -> new ModuleException("printAbsolutePath setting")).getValue());

        paramFile = null;
        paramLong = null;
        paramInt = null;
        paramStr = null;
        tmpLongVariable = 0;
        tmpStrVariable = null;

        updateSettings();
    }

    private void updateSettings() {
        if (StringUtils.isNotBlank(arguments)) {
            if (Type.copy.equals(type) || Type.move.equals(type) || Type.copyForce.equals(type)) {
                paramFile = new java.io.File(arguments);
            } else if (Type.changeLastModified.equals(type)) {
                paramLong = Long.valueOf(arguments);
            } else if (Type.readPart.equals(type) || Type.readTextPart.equals(type)) {
                String[] split = arguments.split(",");
                paramLong = Long.valueOf(split[0].trim());
                paramInt = Integer.valueOf(split[1].trim());
            } else if (Type.writePart.equals(type)) {
                String[] split = arguments.split(",", 2);
                paramLong = Long.valueOf(split[0].trim());
                paramStr = split[1].trim();
            } else if (Type.readTextLastNLines.equals(type)) {
                paramLong = Long.valueOf(arguments);
            } else if (Type.waitFile.equals(type)) {
                paramLong = Long.valueOf(arguments);
            } else if (Type.removeOrWait.equals(type) || Type.writeOrWait.equals(type) || Type.readOrWait.equals(type)) {
                String[] split = arguments.split(",");
                if (split.length >= 2) {
                    paramLong = Long.valueOf(split[0].trim());
                    paramInt = Integer.valueOf(split[1].trim());
                }
            }
        }
    }

    @Override
    public void update(ConfigurationTool externalConfigurationTool) throws ModuleException {
        stop(externalConfigurationTool);
        start(externalConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool) throws ModuleException {
        try {
            if (Objects.equals(externalExecutionContextTool.getType(), "default")) {
                if (externalExecutionContextTool.countSource() > 0) {
                    for (int i = 0; i < externalExecutionContextTool.countSource(); i++) {
                        List<IAction> messagesLst1 = externalExecutionContextTool.getMessages(i);
                        for (int j = 0; j < messagesLst1.size(); j++) {
                            LinkedList<IMessage> messages = new LinkedList<>(messagesLst1.get(j).getMessages());
                            while (!messages.isEmpty()) {
                                Type currentType;
                                if (Type.useArgument.equals(type)) {
                                    IMessage value = messages.poll();
                                    if (ModuleUtils.isNumber(value)) {
                                        currentType = Type.values()[((Number) value.getValue()).intValue()];
                                    } else if (ModuleUtils.isString(value)) {
                                        currentType = Type.valueOf((String) value.getValue());
                                    } else {
                                        externalExecutionContextTool.addError("for type useArgument need type id");
                                        return;
                                    }
                                } else {
                                    currentType = type;
                                }
                                process(externalExecutionContextTool, currentType, messages);
                            }
                        }
                    }
                } else if (!Type.useArgument.equals(type)) {
                    switch (type) {
                        case info:
                        case treeInfo:
                            if (isFile) {
                                getFileAttrFromFile(externalExecutionContextTool, rootFolder, false);
                            } else {
                                getFileAttrsFromFolder(externalExecutionContextTool, rootFolder, false);
                            }
                            break;
                        case fullInfo:
                        case treeFullInfo:
                            if (isFile) {
                                getFileAttrFromFile(externalExecutionContextTool, rootFolder, true);
                            } else {
                                getFileAttrsFromFolder(externalExecutionContextTool, rootFolder, true);
                            }
                            break;
                        case copy:
                            copy(externalExecutionContextTool, rootFolder, paramFile, false, null);
                            break;
                        case copyForce:
                            copy(externalExecutionContextTool, rootFolder, paramFile, true, null);
                            break;
                        case read:
                        case readOrWait:
                            readAll(externalExecutionContextTool, rootFolder, false, 0, 0, type == Type.readOrWait);
                            break;
                        case readText:
                            readAll(externalExecutionContextTool, rootFolder, true, 0, 0, false);
                            break;
                        case tmpFolder:
                            createTmpFolder(externalExecutionContextTool, rootFolder);
                            break;
                        case remove:
                        case removeOrWait:
                            remove(externalExecutionContextTool, rootFolder, type == Type.removeOrWait);
                            break;
                        case dir:
                        case dirSortDate:
                            dir(externalExecutionContextTool, rootFolder, true, type == Type.dirSortDate);
                            break;
                        case create:
                            create(externalExecutionContextTool, rootFolder);
                            break;
                        case writePart:
                            write(externalExecutionContextTool, rootFolder, paramLong, paramStr, false, null, false, false);
                            break;
                        case write:
                        case writeOrWait:
                            write(externalExecutionContextTool, rootFolder, 0, arguments, true, null, false, type == Type.writeOrWait);
                            break;
                        case changeLastModified:
                            changeLastModified(externalExecutionContextTool, rootFolder, paramLong);
                            break;
                        case mkdir:
                            createDir(externalExecutionContextTool, rootFolder);
                            break;
                        case move:
                            move(externalExecutionContextTool, rootFolder, paramFile);
                            break;
                        case readPart:
                            read(externalExecutionContextTool, rootFolder, false, paramLong, paramInt, false);
                            break;
                        case readTextPart:
                            read(externalExecutionContextTool, rootFolder, true, paramLong, paramInt, false);
                            break;
                        case pwd:
                            externalExecutionContextTool.addMessage(FileUtils.getUserDirectory().getAbsolutePath());
                            break;
                        case dirNames:
                        case dirNamesSortDate:
                            dir(externalExecutionContextTool, rootFolder, false, type == Type.dirNamesSortDate);
                            break;
                        case parent:
                            parent(externalExecutionContextTool, rootFolder);
                            break;
                        case readTextNew:
                            readNew(externalExecutionContextTool, rootFolder, true);
                            break;
                        case readNew:
                            readNew(externalExecutionContextTool, rootFolder, false);
                            break;
                        case readTextLastNLines:
                            readTextLastNLines(externalExecutionContextTool, rootFolder, paramLong);
                            break;
                        case waitFile:
                            waitFile(externalExecutionContextTool, rootFolder);
                            break;
                        case writeText:
                            write(externalExecutionContextTool, rootFolder, paramLong, paramStr, true, StandardCharsets.UTF_8, false, false);
                        case writeTextBOM:
                            write(externalExecutionContextTool, rootFolder, paramLong, paramStr, true, StandardCharsets.UTF_8, true, false);
                            break;
                    }
                }
            } else {
                Optional<LinkedList<IMessage>> optionalMessages = ModuleUtils.getLastActionWithData(externalExecutionContextTool.getMessages(0)).map(IAction::getMessages).map(LinkedList::new);
                if (optionalMessages.isPresent()) {
                    Type type = Type.valueOf(externalExecutionContextTool.getType());
                    if (type != this.type)
                        updateSettings();
                    process(externalExecutionContextTool, type, optionalMessages.get());
                }
            }
        } catch (Exception e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    private void process(ExecutionContextTool externalExecutionContextTool, Type currentType, LinkedList<IMessage> messages) throws Exception {
        if (messages == null || currentType == null)
            return;
        switch (currentType) {
            case info:
            case fullInfo:
                // messages.forEach(m -> {
                                    /*
                                    if (srcFile.isDirectory()) {
                                        getFileAttrsFromFolder(externalExecutionContextTool, srcFile, false);
                                    } else {
                                    */
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                getFileAttrFromFile(externalExecutionContextTool, getFile(messages), Type.fullInfo.equals(currentType));
                // }
                // });
                break;
            case treeInfo:
            case treeFullInfo: {
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                java.io.File srcFile = getFile(messages);
                if (isFile) {
                    getFileAttrFromFile(externalExecutionContextTool, srcFile, Type.fullInfo.equals(currentType));
                } else {
                    getFileAttrsFromFolder(externalExecutionContextTool, srcFile, Type.treeFullInfo.equals(currentType));
                }
                // });
                break;
            }
            case copy:
                // while (!messages.isEmpty()) {
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                copy(externalExecutionContextTool, getFile(messages), getFile(messages), false, messages);
                // }
                break;
            case copyForce:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                copy(externalExecutionContextTool, getFile(messages), getFile(messages), true, messages);
                break;
            case read:
            case readOrWait:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                readAll(externalExecutionContextTool, getFile(messages), false, 0, 0, type == Type.readOrWait);
                // });
                break;
            case readText:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                readAll(externalExecutionContextTool, getFile(messages), true, 0, 0, false);
                // });
                break;
            case tmpFolder:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                createTmpFolder(externalExecutionContextTool, getFile(messages));
                // });
                break;
            case remove:
            case removeOrWait:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                remove(externalExecutionContextTool, getFile(messages), currentType == Type.removeOrWait);
                // });
                break;
            case dir:
            case dirSortDate:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                dir(externalExecutionContextTool, getFile(messages), true, currentType == Type.dirSortDate);
                // });
                break;
            case create:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                create(externalExecutionContextTool, getFile(messages));
                // });
                break;
            case writePart: {
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                java.io.File srcFile = getFile(messages);
                Object v = messages.poll().getValue();
                write(externalExecutionContextTool, srcFile, !messages.isEmpty() ? ModuleUtils.getNumber(messages.poll()).longValue() : 0, v, false, null, false, false);
                break;
            }
            case write:
            case writeOrWait:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                write(externalExecutionContextTool, getFile(messages), 0, messages.poll().getValue(), true, null, false, currentType == Type.writeOrWait);
                break;
            case changeLastModified:
                // while (!messages.isEmpty()) {
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                changeLastModified(externalExecutionContextTool, getFile(messages), ModuleUtils.getNumber(messages.poll()).longValue());
                // }
                break;
            case mkdir:
                // messages.forEach(m -> {
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                createDir(externalExecutionContextTool, getFile(messages));
                // });
                break;
            case move:
                // while (!messages.isEmpty()) {
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                move(externalExecutionContextTool, getFile(messages), getFile(messages));
                // }
                break;
            case useArgument:
                break;
            case readPart:
                if (!checkCountMessages(externalExecutionContextTool, messages, 3))
                    break;
                read(externalExecutionContextTool, getFile(messages), false, ModuleUtils.getNumber(messages.poll()).longValue(), ModuleUtils.getNumber(messages.poll()).intValue(), false);
                break;
            case readTextPart:
                if (!checkCountMessages(externalExecutionContextTool, messages, 3))
                    break;
                read(externalExecutionContextTool, getFile(messages), true, ModuleUtils.getNumber(messages.poll()).longValue(), ModuleUtils.getNumber(messages.poll()).intValue(), false);
                break;
            case pwd:
                externalExecutionContextTool.addMessage(FileUtils.getUserDirectory().getAbsolutePath());
                break;
            case dirNames:
            case dirNamesSortDate:
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                dir(externalExecutionContextTool, getFile(messages), false, currentType == Type.dirNamesSortDate);
                break;
            case createPath:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                externalExecutionContextTool.addMessage(new java.io.File(getFile(messages), ModuleUtils.getString(messages.poll())).getAbsolutePath());
                break;
            case parent:
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                parent(externalExecutionContextTool, getFile(messages));
                break;
            case rename:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                rename(externalExecutionContextTool, getFile(messages), ModuleUtils.getString(messages.poll()));
                break;
            case readTextNew:
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                readNew(externalExecutionContextTool, getFile(messages), true);
                break;
            case readNew:
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                readNew(externalExecutionContextTool, getFile(messages), false);
                break;
            case readTextLastNLines:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                readTextLastNLines(externalExecutionContextTool, getFile(messages), ModuleUtils.getNumber(messages.poll()).longValue());
                break;
            case waitFile:
                if (!checkCountMessages(externalExecutionContextTool, messages, 1))
                    break;
                waitFile(externalExecutionContextTool, getFile(messages));
                break;
            case writeText:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                write(externalExecutionContextTool, getFile(messages), 0, messages.poll().getValue(), true,
                        !messages.isEmpty() && ModuleUtils.isString(messages.peek()) ? Charset.forName(ModuleUtils.getString(messages.poll())) : null, false, false);
                break;
            case writeTextBOM:
                if (!checkCountMessages(externalExecutionContextTool, messages, 2))
                    break;
                write(externalExecutionContextTool, getFile(messages), 0, messages.poll().getValue(), true,
                        !messages.isEmpty() && ModuleUtils.isString(messages.peek()) ? Charset.forName(ModuleUtils.getString(messages.poll())) : null, true, false);
                break;
        }
    }

    private void readAll(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, boolean isText, long offset, int size, boolean wait) throws IOException {
        if (!srcFile.exists())
            return;
        if (srcFile.isFile()) {
            read(externalExecutionContextTool, srcFile, isText, offset, size, wait);
        } else {
            TreeSet<Path> paths = new TreeSet<>();
            Files.walkFileTree(srcFile.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    paths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            paths.forEach(file -> {
                try {
                    read(externalExecutionContextTool, file.toFile(), isText, offset, size, wait);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private java.io.File getFile(LinkedList<IMessage> messages) {
        if (isFile || messages == null || messages.isEmpty()) {
            return rootFolder;
        } else {
            String fileName = (String) messages.poll().getValue();
            java.io.File file = new java.io.File(rootFolder, fileName);
            if (!file.exists()) {
                java.io.File fileTmp = new java.io.File(fileName);
                if (fileTmp.isAbsolute() && !useOnlyWorkDirectory)
                    file = fileTmp;
            } else if (fileName.contains("..") && useOnlyWorkDirectory) {
                file = rootFolder;
            }
            return file;
        }
    }

    @Override
    public void stop(ConfigurationTool externalConfigurationTool) throws ModuleException {
        type = null;
        hashAlgType = null;
        workDirectory = null;
        rootFolder = null;
        arguments = null;
        paramFile = null;
        paramLong = null;
        paramInt = null;
        paramStr = null;
        printAbsolutePath = null;
        useOnlyWorkDirectory = null;
        tmpLongVariable = 0;
        tmpStrVariable = null;
    }

    private void dir(ExecutionContextTool externalExecutionContextTool, java.io.File folder, boolean getPath, boolean sortDate) {
        if (!folder.exists() || !folder.isDirectory())
            return;
        java.io.File[] files = folder.listFiles();
        if (files == null)
            return;

        List<java.io.File> fileList = Arrays.stream(files).collect(Collectors.toList());
        if (sortDate)
            fileList.sort(Comparator.comparing(java.io.File::lastModified));
        fileList.forEach(f -> {
            try {
                externalExecutionContextTool.addMessage(getPath ? getAbsolutePath(f) : f.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        /*
        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                externalExecutionContextTool.addMessage(getAbsolutePath(file.toFile()) + "/");
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                externalExecutionContextTool.addMessage(getAbsolutePath(dir.toFile()));
                return super.preVisitDirectory(dir, attrs);
            }
        });
        */
    }

    private void getFileAttrsFromFolder(ExecutionContextTool externalExecutionContextTool, java.io.File folder, boolean getHash) throws Exception {
        if (folder == null || !folder.exists() || !folder.isDirectory())
            return;

        //List<IData> lst = null;
        //result = new ArrayList<IData>();
        // getFileAttrFromFile(externalExecutionContextTool, folder, getHash);

        java.io.File[] files = folder.listFiles();
        if (files == null)
            return;

        /*
        Arrays.stream(files).forEach(f -> {
            try {
                getFileAttrFromFile(externalExecutionContextTool, f, getHash);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        */
        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                try {
                    getFileAttrFromFile(externalExecutionContextTool, new java.io.File(file.toUri()), getHash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                try {
                    getFileAttrFromFile(externalExecutionContextTool, new java.io.File(dir.toUri()), getHash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return super.preVisitDirectory(dir, attrs);
            }
        });

    }

    private void getFileAttrFromFile(ExecutionContextTool externalExecutionContextTool, java.io.File file, boolean getHash) throws Exception {
        //File fileTmp = new File(path);
        if (!file.exists() && !file.canRead())
            return;

        String hash = null;
        if (getHash) {
            hash = buildHexStringFromByteArray(createChecksum(file));
            if (hash == null)
                return;
        }

        //result = new FileAttr(fileSource.getName() + File.separator + path, fileTmp.lastModified(), fileTmp.length(), hash);
        //System.out.println(Utils.exportFileAttr(result));

        externalExecutionContextTool.addMessage(file.getAbsolutePath());
        externalExecutionContextTool.addMessage(file.length());
        externalExecutionContextTool.addMessage(file.lastModified());
        externalExecutionContextTool.addMessage(file.canWrite() ? 2 : (file.canRead() ? 1 : 0));
        externalExecutionContextTool.addMessage(file.isFile() ? 1 : 0);

        if (getHash)
            externalExecutionContextTool.addMessage(hash);
    }

    private byte[] createChecksum(java.io.File file) throws Exception {
        byte[] result = null;

        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[intCountByte];
            MessageDigest complete = MessageDigest.getInstance(hashAlgType.getAlgName());
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            result = complete.digest();
        }

        return result;
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    private String buildHexStringFromByteArray(byte b[]) {
        String result = "";
        if (b == null)
            return result;

        for (int i = 0; i < b.length; i++) {
            String str = Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            //System.out.println(str.length() + " " + str);
            result += str;
        }

        return result;
    }

    private void copyFolder(java.io.File src, java.io.File dest, boolean force, List<Pattern> excludePaths) throws IOException {
        // checks
        if (src == null || dest == null)
            return;
        if (!src.isDirectory())
            return;
        if (dest.exists()) {
            if (!dest.isDirectory()) {
                if (force) {
                    if (!dest.delete() || !dest.mkdir())
                        return;
                } else {
                    //System.out.println("destination not a folder " + dest);
                    return;
                }
            }
        } else {
            dest.mkdir();
        }
        java.io.File[] listFiles = src.listFiles();
        if (listFiles == null || listFiles.length == 0)
            return;

        String strAbsPathSrc = src.getAbsolutePath();
        String strAbsPathDest = dest.getAbsolutePath();

        //try {
        Files.walkFileTree(src.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                String srcPath = file.toAbsolutePath().toString();
                java.io.File dstFile = new java.io.File(strAbsPathDest + srcPath.substring(strAbsPathSrc.length()));
                if (!force && dstFile.exists())
                    return FileVisitResult.CONTINUE;
                if (excludePaths != null && excludePaths.stream().anyMatch(p -> p.matcher(srcPath).matches()))
                    return FileVisitResult.CONTINUE;

                if (!dstFile.getParentFile().exists())
                    dstFile.getParentFile().mkdirs();

                //System.out.println(file + " " + dstFile.getAbsolutePath());
                if (force) {
                    Files.copy(file, dstFile.toPath(), REPLACE_EXISTING);
                } else {
                    Files.copy(file, dstFile.toPath());
                }

                return FileVisitResult.CONTINUE;
            }
        });
			/*
		} catch (IOException e) {
			//e.printStackTrace();
			return;
		}
		*/

        return;
    }

    private void delete(java.io.File srcFile, boolean wait) /*throws IOException*/ {
        if (srcFile == null || !srcFile.exists())
            return;
        boolean needExit = false;
        long startTime = System.currentTimeMillis();
        do {
            FileUtils.deleteQuietly(srcFile);
            if (!srcFile.exists())
                needExit = true;
            if (wait && !needExit && paramInt != null) {
                try {
                    Thread.sleep(paramInt);
                } catch (Exception e) {
                }
            }
        } while (wait && !needExit && paramLong != null && (startTime + paramLong) < System.currentTimeMillis());

        /*
        Files.walkFileTree(srcFile.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        */
    }

    private void write(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, long offset, Object value, boolean rewrite, Charset charset, boolean useBom, boolean wait) throws IOException {
        if (!srcFile.exists()) {
            if (srcFile.getParentFile() != null && !srcFile.getParentFile().exists())
                srcFile.getParentFile().mkdirs();
            if (srcFile.createNewFile())
                externalExecutionContextTool.addMessage(getAbsolutePath(srcFile));
        }
        if (srcFile.exists() && !srcFile.canWrite()) {
            externalExecutionContextTool.addError("cannot write in file " + srcFile.getAbsolutePath());
            return;
        }
        boolean isBites = value instanceof byte[];

        if (!isBites && charset == null) {
            if (srcFile.exists()) {
                charset = detectCharset(srcFile);
            } else {
                charset = Charset.defaultCharset();
            }
        }

        long changeSize = 0;
        if (rewrite) {
            if (isBites) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(srcFile, false)) {
                    byte[] bytes = (byte[]) value;
                    fileOutputStream.write(bytes);
                    changeSize = bytes.length;
                }
            } else {
                boolean needExit = false;
                long startTime = System.currentTimeMillis();
                do {
                    try (FileWriter fileWriter = new FileWriter(srcFile, charset, false)) {
                        if (useBom) {
                            if (charset == StandardCharsets.UTF_8) {
                                fileWriter.write('\ufeff');
                            } else if (charset == StandardCharsets.UTF_16BE) {
                                fileWriter.write('\ufeff');
                            } else if (charset == StandardCharsets.UTF_16LE) {
                                fileWriter.write('\ufeff');
                            }
                        }
                        String str = value.toString();
                        fileWriter.append(str);
                        changeSize = str.length();
                        needExit = true;
                    } catch (Exception e) {
                        if (!wait)
                            throw e;
                    }
                    if (wait && !needExit && paramInt != null) {
                        try {
                            Thread.sleep(paramInt);
                        } catch (Exception e) {
                        }
                    }
                } while (wait && !needExit && paramLong != null && (startTime + paramLong) < System.currentTimeMillis());
            }
        } else {
            boolean needExit = false;
            long startTime = System.currentTimeMillis();
            do {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(srcFile, "rw")) {
                    long startPosition = Math.max(0, offset > 0 ? offset : srcFile.length() + offset);
                    if (startPosition > 0)
                        randomAccessFile.seek(startPosition);
                    if (isBites) {
                        byte[] bytes = (byte[]) value;
                        randomAccessFile.write(bytes);
                        changeSize = bytes.length;
                    } else {
                        String str = value.toString();
                        randomAccessFile.write(str.getBytes(charset));
                        // randomAccessFile.writeBytes(str);
                        changeSize = str.length();
                    }
                    needExit = true;
                } catch (Exception e) {
                    if (!wait)
                        throw e;
                }
                if (wait && !needExit && paramInt != null) {
                    try {
                        Thread.sleep(paramInt);
                    } catch (Exception e) {
                    }
                }
            } while (wait && !needExit && paramLong != null && (startTime + paramLong) < System.currentTimeMillis());
        }
        externalExecutionContextTool.addMessage(changeSize);
    }

    private void copy(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, java.io.File dstFile, boolean force, LinkedList<IMessage> messages) throws Exception {
        if (!srcFile.exists())
            return;
        if (srcFile.isDirectory()) {
            List<Pattern> excludePaths = null;
            if (messages != null) {
                String excludePath;
                while ((excludePath = ModuleUtils.getString(messages.poll())) != null) {
                    if (excludePaths == null)
                        excludePaths = new LinkedList<>();
                    excludePaths.add(Pattern.compile(excludePath));
                }
            }
            copyFolder(srcFile, dstFile, force, excludePaths);
            getFileAttrFromFile(externalExecutionContextTool, dstFile, false);
        } else {
            if (!force && dstFile.exists())
                return;

            if (!dstFile.getParentFile().exists())
                dstFile.getParentFile().mkdirs();

            if (force) {
                Files.copy(srcFile.toPath(), dstFile.toPath(), REPLACE_EXISTING);
            } else {
                Files.copy(srcFile.toPath(), dstFile.toPath());
            }
            getFileAttrFromFile(externalExecutionContextTool, dstFile, false);
        }
    }

    private Charset detectCharset(java.io.File srcFile) throws IOException {
        Charset charset = Charset.defaultCharset();
        try (FileInputStream fis = new FileInputStream(srcFile); BufferedInputStream bis = new BufferedInputStream(fis)) {
            CharsetDetector cd = new CharsetDetector();
            cd.setText(bis);
            CharsetMatch cm = cd.detect();

            if (cm != null) {
                charset = Charset.forName(cm.getName());
                // Reader reader = cm.getReader();
            } else {
                // throw new UnsupportedCharsetException();
                // externalExecutionContextTool.addError("UnsupportedCharset");
            }
        }
        return charset;
    }

    private void read(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, boolean isText, long offset, int size, boolean wait) throws IOException {
        if (srcFile.isDirectory() || !srcFile.canRead()/* || srcFile.length() == 0*/)
            return;
        //loggerDebug("read");
        long fileSize = srcFile.length();
        long startPosition = Math.max(0, offset >= 0 ? offset : fileSize + offset);
        int newSize = size > 0 ? size : (int) (fileSize - startPosition);
        if (isText) {
            /*
            if (size == 0) {
                externalExecutionContextTool.addMessage(FileUtils.readFileToString(srcFile));
            } else {
            */
            Charset charset = detectCharset(srcFile);

            boolean needExit = false;
            long startTime = System.currentTimeMillis();
            do {
                try (FileReader fileReader = new FileReader(srcFile, charset)) {
                    if (startPosition > 0)
                        fileReader.skip(startPosition);
                    char[] arr = new char[newSize];
                    int readLength = fileReader.read(arr);
                    if (readLength > 0) {
                        if (readLength != newSize)
                            arr = ArrayUtils.subarray(arr, 0, readLength);
                        externalExecutionContextTool.addMessage(new String(arr));
                    }
                    needExit = true;
                } catch (IOException e) {
                    if (!wait)
                        throw e;
                }
                if (wait && !needExit && paramInt != null) {
                    try {
                        Thread.sleep(paramInt);
                    } catch (Exception e) {
                    }
                }
            } while (wait && !needExit && paramLong != null && (startTime + paramLong) < System.currentTimeMillis());
            // }
        } else {
            /*
            if (size == 0) {
                byte bVar[] = Files.readAllBytes(srcFile.toPath());
                externalExecutionContextTool.addMessage(bVar);
            } else {
            */
            boolean needExit = false;
            long startTime = System.currentTimeMillis();
            do {
                try (FileInputStream fileInputStream = new FileInputStream(srcFile)) {
                    if (startPosition > 0)
                        fileInputStream.skip(startPosition);
                    byte[] arr = new byte[newSize];
                    int readLength = fileInputStream.read(arr);
                    if (readLength > 0) {
                        if (readLength != newSize)
                            arr = ArrayUtils.subarray(arr, 0, readLength);
                        externalExecutionContextTool.addMessage(arr);
                    }
                    needExit = true;
                } catch (IOException e) {
                    if (!wait)
                        throw e;
                }
                if (wait && !needExit && paramInt != null) {
                    try {
                        Thread.sleep(paramInt);
                    } catch (Exception e) {
                    }
                }
            } while (wait && !needExit && paramLong != null && (startTime + paramLong) < System.currentTimeMillis());
            // }
        }
    }

    private void createTmpFolder(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile) throws IOException {
        if (!srcFile.exists() || !srcFile.isDirectory())
            return;
        Path pathNewFolder = Files.createTempDirectory(srcFile.toPath(), "Folder");
        if (pathNewFolder == null)
            return;
        externalExecutionContextTool.addMessage(getAbsolutePath(pathNewFolder.toFile()));
    }

    private void remove(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, boolean wait) {
        if (!srcFile.exists())
            return;
        delete(srcFile, wait);
        externalExecutionContextTool.addMessage(getAbsolutePath(srcFile));
    }

    private void create(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile) throws IOException {
        if (!srcFile.exists()) {
            srcFile.getParentFile().mkdirs();
            if (srcFile.createNewFile())
                externalExecutionContextTool.addMessage(getAbsolutePath(srcFile));
        }
    }

    private void changeLastModified(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, long value) {
        long l = srcFile.lastModified();
        srcFile.setLastModified(value);
        externalExecutionContextTool.addMessage(l);
    }

    private void createDir(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile) {
        if (srcFile.exists())
            return;
        if (srcFile.mkdirs())
            externalExecutionContextTool.addMessage(getAbsolutePath(srcFile));
    }

    private void move(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, java.io.File dstFile) throws Exception {
        if (!srcFile.exists())
            return;
        if (srcFile.isDirectory()) {
            copyFolder(srcFile, dstFile, false, null);
            delete(srcFile, false);
            getFileAttrFromFile(externalExecutionContextTool, dstFile, false);
        } else {
            if (dstFile.exists())
                return;

            if (!dstFile.getParentFile().exists())
                dstFile.getParentFile().mkdirs();

            FileUtils.moveFile(srcFile, dstFile);
            getFileAttrFromFile(externalExecutionContextTool, dstFile, false);
        }
    }

    private void parent(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile) {
        if (srcFile == null || !srcFile.exists() || !srcFile.isDirectory())
            return;
        java.io.File parentFile = srcFile.getParentFile();
        if (parentFile == null || !parentFile.isDirectory())
            return;
        externalExecutionContextTool.addMessage(getAbsolutePath(parentFile));
    }

    private void rename(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, String newName) throws IOException {
        if (srcFile == null || !srcFile.exists())
            return;
        // java.io.File newFile = new java.io.File(srcFile.getParent(), newName);
        // Files.move(oldFile.toPath(), newFile.toPath());
        // externalExecutionContextTool.addMessage(srcFile.renameTo(newFile) ? 1 : 0);

        Path source = srcFile.toPath();//Paths.get("from/path");
        Path newPath = source.resolveSibling(newName);//source.getParent().resolve(newName);//Paths.get("to/path");
        Files.move(source, newPath, REPLACE_EXISTING);
        externalExecutionContextTool.addMessage(getAbsolutePath(newPath.toFile()));
    }

    private String getAbsolutePath(java.io.File file) {
        String result = printAbsolutePath ? file.getAbsolutePath() : file.getAbsolutePath().replace(isFile ? workDirectory.getAbsolutePath() : rootFolder.getAbsolutePath(), "");
        if (!printAbsolutePath && (result.startsWith("\\") || result.startsWith("/")))
            result = result.substring(1);
        return result;
    }

    private void readTextLastNLines(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, long n_lines) throws IOException {
        Charset charset = detectCharset(srcFile);
        try (ReversedLinesFileReader object = new ReversedLinesFileReader(srcFile, charset)) {
            int counter = 0;
            StringBuilder sb = new StringBuilder();
            while (counter < n_lines) {
                counter++;
                sb.append(object.readLine());
                sb.append(System.lineSeparator());
            }
            externalExecutionContextTool.addMessage(sb.toString());
        }
    }

    private boolean checkCountMessages(ExecutionContextTool externalExecutionContextTool, List<IMessage> messages, int count) {
        boolean result = messages.size() < count;
        if (result) {
            messages.clear();
            externalExecutionContextTool.addError(String.format("need at least %d params", count));
        }
        return !result;
    }

    private void readNew(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile, boolean isText) throws IOException {
        long len = srcFile.length();
        if (tmpStrVariable == null || !Objects.equals(tmpStrVariable, srcFile.getAbsolutePath())) {
            tmpStrVariable = srcFile.getAbsolutePath();
            tmpLongVariable = len;
        }
        if (len < tmpLongVariable) {
            // Log must have been jibbled or deleted.
            tmpLongVariable = len;
        } else if (len > tmpLongVariable) {
            read(externalExecutionContextTool, srcFile, isText, tmpLongVariable, 0, false);
            tmpLongVariable = len;
        }
    }

    private void waitFile(ExecutionContextTool externalExecutionContextTool, java.io.File srcFile) {
        Instant end = Instant.now().plusMillis(paramLong);
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (!srcFile.exists() && !externalExecutionContextTool.isNeedStop() && Instant.now().isBefore(end));
        if (srcFile.exists())
            externalExecutionContextTool.addMessage(1);
    }

}
