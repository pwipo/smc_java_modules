package ru.smcsystem.smcmodules.module;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Realm;
import org.apache.catalina.Service;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server implements Module {

    private Map<Long, Exchanger<List<Object>>> requestMap;
    private LinkedList<List<Object>> newRequests;

    // private AtomicLong requestCounter;
    private volatile Tomcat tomcat;
    private Map<String, VirtualServerInfo> virtualServerInfoMap;
    private Protocol protocol;
    private RequestType requestType;
    private AtomicLong reqIdGenerator;
    private Map<Long, Response> mapResponse;
    private Map<Long/*threadId*/, Long/*reqId*/> threadReqMap;
    private MimetypesFileTypeMap mimetypesFileTypeMap;

    @Override
    public void start(ConfigurationTool externalConfigurationTool) throws ModuleException {
        protocol = externalConfigurationTool.getSetting("protocol").map(ModuleUtils::toString).map(Protocol::valueOf)
                .orElseThrow(() -> new ModuleException("protocol setting"));
        Integer port = externalConfigurationTool.getSetting("port").map(ModuleUtils::toNumber).map(Number::intValue).orElseThrow(() -> new ModuleException("port setting"));
        Integer requestTimeout = externalConfigurationTool.getSetting("requestTimeout").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("requestTimeout setting"));
        Integer countThreads = externalConfigurationTool.getSetting("countThreads").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("countThreads setting"));
        Integer backlog = externalConfigurationTool.getSetting("backlog").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("backlog setting"));
        String availablePaths = externalConfigurationTool.getSetting("availablePaths").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("availablePaths setting"));
        String keyStoreFileName = externalConfigurationTool.getSetting("keyStoreFileName").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("keyStoreFileName setting"));
        String keyStorePass = externalConfigurationTool.getSetting("keyStorePass").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("keyStorePass setting"));
        String keyAlias = externalConfigurationTool.getSetting("keyAlias").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("keyAlias setting"));
        String keyPass = externalConfigurationTool.getSetting("keyPass").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("keyPass setting"));
        Integer sessionTimeout = externalConfigurationTool.getSetting("sessionTimeout").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("sessionTimeout setting"));
        Integer maxPostSize = externalConfigurationTool.getSetting("maxPostSize").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("maxPostSize setting"));
        Boolean allowMultipartParsing = externalConfigurationTool.getSetting("allowMultipartParsing").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("allowMultipartParsing setting"));
        String strAddress = externalConfigurationTool.getSetting("bindAddress").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("bindAddress setting"));
        Integer fileResponsePieceSize = externalConfigurationTool.getSetting("fileResponsePieceSize").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("fileResponsePieceSize setting"));
        ObjectArray headersArr = externalConfigurationTool.getSetting("headers").map(ModuleUtils::toObjectArray)
                .orElseThrow(() -> new ModuleException("headers setting"));
        List<String> availablePathsList = Arrays.stream(availablePaths.split("\n"))
                .flatMap(s -> Arrays.stream(s.split("::")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        ObjectArray virtualServerSettings = externalConfigurationTool.getSetting("virtualServerSettings").map(ModuleUtils::toObjectArray)
                .orElseThrow(() -> new ModuleException("virtualServerSettings setting"));
        requestType = externalConfigurationTool.getSetting("requestType").map(ModuleUtils::toString).map(RequestType::valueOf)
                .orElseThrow(() -> new ModuleException("requestType setting"));
        Integer maxFileSizeFull = externalConfigurationTool.getSetting("maxFileSizeFull").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("maxFileSizeFull setting"));
        ObjectArray corsAccessListArr = externalConfigurationTool.getSetting("corsAccessList").map(ModuleUtils::toObjectArray)
                .orElseThrow(() -> new ModuleException("corsAccessList setting"));
        reqIdGenerator = new AtomicLong();
        reqIdGenerator.compareAndSet(Long.MAX_VALUE, 0);
        mapResponse = new ConcurrentHashMap<>();
        threadReqMap = new ConcurrentHashMap<>();

        virtualServerInfoMap = new HashMap<>();
        if (protocol == Protocol.VIRTUAL) {
            if (!ModuleUtils.isArrayContainObjectElements(virtualServerSettings))
                throw new ModuleException("missing virtualServerSettings for virtual servers");
            for (int i = 0; i < virtualServerSettings.size(); i++) {
                ObjectElement objectElement = (ObjectElement) virtualServerSettings.get(i);
                Protocol protocolVar = objectElement.findField("protocol").map(ModuleUtils::getString).map(Protocol::valueOf).orElse(Protocol.HTTP);
                String hostname = objectElement.findField("hostname").map(ModuleUtils::getString).orElse("localhost");
                Integer portVar = objectElement.findField("port").map(ModuleUtils::getNumber).map(Number::intValue).orElse(port);
                String keyStoreFileNameVar = objectElement.findField("keyStoreFileName").map(ModuleUtils::getString).orElse(keyStoreFileName);
                String keyStorePassVar = objectElement.findField("keyStorePass").map(ModuleUtils::getString).orElse(keyStorePass);
                String keyAliasVar = objectElement.findField("keyAlias").map(ModuleUtils::getString).orElse(keyAlias);
                String keyPassVar = objectElement.findField("keyPass").map(ModuleUtils::getString).orElse(keyPass);
                String strAddressVar = objectElement.findField("bindAddress").map(ModuleUtils::getString).orElse(strAddress);
                Integer requestTimeoutVar = objectElement.findField("requestTimeout").map(ModuleUtils::getNumber).map(Number::intValue).orElse(requestTimeout);
                Integer countThreadsVar = objectElement.findField("countThreads").map(ModuleUtils::getNumber).map(Number::intValue).orElse(countThreads);
                Integer backlogVar = objectElement.findField("backlog").map(ModuleUtils::getNumber).map(Number::intValue).orElse(backlog);
                Integer sessionTimeoutVar = objectElement.findField("sessionTimeout").map(ModuleUtils::getNumber).map(Number::intValue).orElse(sessionTimeout);
                Integer maxPostSizeVar = objectElement.findField("maxPostSize").map(ModuleUtils::getNumber).map(Number::intValue).orElse(maxPostSize);
                Boolean allowMultipartParsingVar = objectElement.findField("allowMultipartParsing").map(ModuleUtils::getString).map(Boolean::parseBoolean).orElse(allowMultipartParsing);
                ObjectArray headersArrVar = objectElement.findField("headers").map(ModuleUtils::toObjectArray).orElse(headersArr);
                RequestType requestTypeVar = objectElement.findField("requestType").map(ModuleUtils::getString).map(RequestType::valueOf).orElse(requestType);
                Integer maxFileSizeFullVar = objectElement.findField("maxFileSizeFull").map(ModuleUtils::getNumber).map(Number::intValue).orElse(maxFileSizeFull);
                Integer fileResponsePieceSizeVar = objectElement.findField("fileResponsePieceSize").map(ModuleUtils::getNumber).map(Number::intValue).orElse(fileResponsePieceSize);
                ObjectArray corsAccessListArrVar = objectElement.findField("corsAccessList").map(ModuleUtils::toObjectArray).orElse(corsAccessListArr);
                URL url = null;
                try {
                    url = new URL(protocolVar.name().toLowerCase(), hostname, portVar, "");
                } catch (Exception e) {
                    throw new ModuleException("error", e);
                }
                String urlHeader = String.format("%s://%s:%d", url.getProtocol(), url.getHost(), url.getPort());
                List<Map.Entry<Integer, String>> paths = new LinkedList<>();
                for (int j = 0; j < availablePathsList.size(); j++) {
                    String path = availablePathsList.get(j);
                    if (path.isBlank())
                        continue;
                    if (path.startsWith("/") || path.startsWith("^/") || !path.toUpperCase().startsWith("HTTP")) {
                        paths.add(Map.entry(j, path));
                    } else if (path.toUpperCase().startsWith(urlHeader.toUpperCase())) {
                        paths.add(Map.entry(j, path.substring(urlHeader.length())));
                    }
                }
                externalConfigurationTool.loggerDebug(String.format("add virtualServer=%s, paths=%s", url, paths.stream().map(Map.Entry::getValue).collect(Collectors.joining(","))));
                VirtualServerInfo virtualServerInfo = buildVirtualInfo(externalConfigurationTool, urlHeader, url, keyStoreFileNameVar, keyStorePassVar,
                        keyAliasVar, keyPassVar, strAddressVar, paths, requestTimeoutVar, countThreadsVar, backlogVar, sessionTimeoutVar, maxPostSizeVar,
                        allowMultipartParsingVar, headersArrVar, requestTypeVar, maxFileSizeFullVar, fileResponsePieceSizeVar, corsAccessListArrVar);
                virtualServerInfoMap.put(virtualServerInfo.getUrlHeader(), virtualServerInfo);
            }
        } else {
            String hostname = "localhost";
            try {
                List<Map.Entry<Integer, String>> paths = new LinkedList<>();
                Stream.iterate(0, n -> n + 1)
                        .limit(availablePathsList.size())
                        .forEach(id -> paths.add(Map.entry(id, availablePathsList.get(id))));
                VirtualServerInfo virtualServerInfo = buildVirtualInfo(externalConfigurationTool, hostname, new URL(protocol.name().toLowerCase(), hostname, port, ""),
                        keyStoreFileName, keyStorePass, keyAlias, keyPass, strAddress, paths, requestTimeout, countThreads, backlog, sessionTimeout,
                        maxPostSize, allowMultipartParsing, headersArr, requestType, maxFileSizeFull, fileResponsePieceSize, corsAccessListArr);
                virtualServerInfoMap = Map.of(virtualServerInfo.getUrlHeader(), virtualServerInfo);
            } catch (Exception e) {
                throw new ModuleException("error", e);
            }
        }

        // requestCounter = new AtomicLong();
        requestMap = new ConcurrentHashMap<>(countThreads * 10);
        newRequests = new LinkedList<>();

        mimetypesFileTypeMap = (MimetypesFileTypeMap) MimetypesFileTypeMap.getDefaultFileTypeMap();
        // if (!mimetypesFileTypeMap.getContentType("1.css").trim().equals("text/css")) {
        mimetypesFileTypeMap.addMimeTypes("text/csv csv CSV");
        mimetypesFileTypeMap.addMimeTypes("text/php php PHP");
        mimetypesFileTypeMap.addMimeTypes("text/css css CSS");
        mimetypesFileTypeMap.addMimeTypes("application/javascript js JS");
        mimetypesFileTypeMap.addMimeTypes("application/json json JSON");
        mimetypesFileTypeMap.addMimeTypes("application/pdf pdf PDF");
        mimetypesFileTypeMap.addMimeTypes("application/postscript ps PS");
        mimetypesFileTypeMap.addMimeTypes("application/zip zip ZIP");
        mimetypesFileTypeMap.addMimeTypes("application/gzip gzip GZIP");
        mimetypesFileTypeMap.addMimeTypes("application/xml xml XML");
        mimetypesFileTypeMap.addMimeTypes("audio/ogg ogg OGG");
        mimetypesFileTypeMap.addMimeTypes("video/webm webm WEBM");
        mimetypesFileTypeMap.addMimeTypes("image/svg+xml svg SVG");
        // }
    }

    private VirtualServerInfo buildVirtualInfo(ConfigurationTool externalConfigurationTool, String urlOrigin, URL url,
                                               String keyStoreFileName, String keyStorePass, String keyAlias, String keyPass,
                                               String strAddress, List<Map.Entry<Integer, String>> paths, Integer requestTimeout, Integer countThreads,
                                               Integer backlog, Integer sessionTimeout, Integer maxPostSize, Boolean allowMultipartParsing,
                                               ObjectArray headersArr, RequestType requestTypeVar, Integer maxFileSizeFullVar, Integer fileResponsePieceSizeVar,
                                               ObjectArray corsAccessListArr) {
        File keyStore = null;
        if (StringUtils.isNotBlank(keyStoreFileName)) {
            keyStore = new File(externalConfigurationTool.getWorkDirectory(), keyStoreFileName);
            if (!keyStore.exists() || !keyStore.canRead())
                throw new IllegalArgumentException("wrong keyStoreFileName");
        }
        InetAddress address = null;
        try {
            if (StringUtils.isNotBlank(strAddress))
                address = InetAddress.getByName(strAddress);
        } catch (UnknownHostException e) {
            throw new ModuleException("wrong bindAddress");
        }
        List<Map.Entry<Integer, Pattern>> patterns = null;
        if (paths != null && !paths.isEmpty()) {
            patterns = paths.stream()
                    .filter(e -> StringUtils.isNotBlank(e.getValue()))
                    .map(e -> Map.entry(e.getKey(), Pattern.compile(e.getValue())))
                    .collect(Collectors.toList());
        }
        List<String> headers = null;
        if (headersArr != null && headersArr.size() > 0 && headersArr.isSimple()) {
            headers = new ArrayList<>();
            for (int i = 0; i < headersArr.size(); i++)
                headers.add(headersArr.get(i).toString());
        }
        List<Pattern> corsAccessList = null;
        if (corsAccessListArr != null && corsAccessListArr.size() > 0 && corsAccessListArr.isSimple()) {
            corsAccessList = new ArrayList<>();
            for (int i = 0; i < corsAccessListArr.size(); i++)
                corsAccessList.add(Pattern.compile(corsAccessListArr.get(i).toString()));
        }
        return new VirtualServerInfo(urlOrigin, url, keyStore, keyStorePass, keyAlias, keyPass, address, patterns,
                requestTimeout, countThreads, backlog, sessionTimeout, maxPostSize, allowMultipartParsing,
                headers, requestTypeVar, maxFileSizeFullVar, fileResponsePieceSizeVar, corsAccessList);
    }

    @Override
    public void update(ConfigurationTool externalConfigurationTool) throws ModuleException {
        stop(externalConfigurationTool);
        start(externalConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        ModuleUtils.executor(configurationTool, executionContextTool, -1, null, (ignore, ignore2) -> {
            if (Objects.equals(executionContextTool.getType(), "default")) {
                int countManagedExecutionContexts = executionContextTool.getFlowControlTool().countManagedExecutionContexts();
                if (countManagedExecutionContexts == 0) {
                    stop(configurationTool, executionContextTool);
                } else {
                    start(configurationTool, executionContextTool);
                }
            } else {
                Type type = Type.valueOf(executionContextTool.getType().toUpperCase());
                switch (type) {
                    case START:
                        start(configurationTool, executionContextTool);
                        break;
                    case STOP:
                        stop(configurationTool, executionContextTool);
                        break;
                    case FAST_RESPONSE:
                        fastResponse(configurationTool, executionContextTool);
                        break;
                    case FILE_FAST_RESPONSE:
                        fileFastResponse(configurationTool, executionContextTool);
                        break;
                    case GET_FILE_PART:
                        getFilePart(configurationTool, executionContextTool);
                        break;
                    case GET_PART_AS_OBJECT:
                        getPartAsObject(configurationTool, executionContextTool);
                        break;
                }
            }
        });
    }

    private void getPartAsObject(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws IOException {
        if (tomcat == null) {
            executionContextTool.addError("server not started");
            return;
        }
        Optional<LinkedList<IMessage>> messagesOpt = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0));
        if (messagesOpt.isEmpty()) {
            executionContextTool.addError("need 3 messages");
            return;
        }
        LinkedList<IMessage> messages = messagesOpt.get();
        ObjectArray objectArray = ModuleUtils.getObjectArray(messages.poll());
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need objectArray");
            return;
        }
        ObjectElement objectElement = (ObjectElement) objectArray.get(0);
        Long reqId = objectElement.findField("reqId").map(ModuleUtils::getNumber).map(Number::longValue).or(() -> Optional.of(-1L))
                .map(n -> getReqId(configurationTool, executionContextTool, n)).orElse(null);
        int fileId = Optional.ofNullable(ModuleUtils.getNumber(messages.poll())).map(Number::intValue).orElse(0);
        int maxSize = Optional.ofNullable(ModuleUtils.getNumber(messages.poll())).map(Number::intValue).orElse(Integer.MAX_VALUE);
        int size = 1024 * 1024;
        if (reqId == null) {
            executionContextTool.addError("need reqId");
            return;
        }
        Response response = mapResponse.get(reqId);
        if (response == null) {
            executionContextTool.addError("response for reqId not exist");
            return;
        }
        if (response.getVirtualServerInfo().getMaxPostSize() != null)
            maxSize = Math.min(maxSize, response.getVirtualServerInfo().getMaxPostSize());

        byte[] bytes = null;
        Optional<ObjectArray> multipart = objectElement.findField("multipart").map(ModuleUtils::getObjectArray);
        if (multipart.isPresent()) {
            if (fileId < 0)
                fileId = Math.max(Math.min(multipart.get().size() - fileId, multipart.get().size() - 1), 0);
            int fileIdTmp = fileId;
            bytes = multipart
                    .filter(arr -> arr.size() > fileIdTmp && ModuleUtils.isArrayContainObjectElements(arr))
                    .map(arr -> (ObjectElement) arr.get(fileIdTmp))
                    .flatMap(o -> o.findField("data"))
                    .map(ModuleUtils::getBytes).orElse(null);
        } else {
            fileId = 0;
            bytes = objectElement.findField("data").map(ModuleUtils::getBytes).orElse(null);
        }
        if (bytes == null) {
            RequestInputStream requestInputStream = response.getRequestInputStreamMap().get(fileId);
            if (requestInputStream != null && requestInputStream.getSize() > 0) {
                int count = 0;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    do {
                        bytes = requestInputStream.getInputStream().readNBytes(size);
                        if (bytes.length > 0) {
                            count += bytes.length;
                            baos.write(bytes);
                        }
                    } while (bytes.length > 0 && count < maxSize);
                    bytes = baos.toByteArray();
                }
            }
        }
        if (bytes != null && bytes.length > 0)
            ModuleUtils.executeAndGetArrayElements(executionContextTool, 0, List.of(bytes)).ifPresent(executionContextTool::addMessage);
    }

    private void getFilePart(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws IOException {
        if (tomcat == null) {
            executionContextTool.addError("server not started");
            return;
        }
        Optional<LinkedList<IMessage>> messagesOpt = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0));
        if (messagesOpt.isEmpty()) {
            executionContextTool.addError("need 3 messages");
            return;
        }
        LinkedList<IMessage> messages = messagesOpt.get();
        Long reqId = Optional.ofNullable(ModuleUtils.getNumber(messages.poll())).map(Number::longValue).or(() -> Optional.of(-1L))
                .map(n -> getReqId(configurationTool, executionContextTool, n)).orElse(null);
        Integer fileId = Optional.ofNullable(ModuleUtils.getNumber(messages.poll())).map(Number::intValue).orElse(0);
        Integer size = Optional.ofNullable(ModuleUtils.getNumber(messages.poll())).map(Number::intValue).orElse(1024 * 1024);
        if (reqId == null) {
            executionContextTool.addError("need reqId");
            return;
        }
        Response response = mapResponse.get(reqId);
        if (response == null) {
            executionContextTool.addError("response for reqId not exist");
            return;
        }
        RequestInputStream requestInputStream = response.getRequestInputStreamMap().get(fileId);
        if (requestInputStream == null) {
            executionContextTool.addError("requestInputStream not exist");
            return;
        }
        byte[] bytes = requestInputStream.getInputStream().readNBytes(size);
        if (bytes.length > 0)
            executionContextTool.addMessage(bytes);
    }

    private void start(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) {
        if (tomcat != null) {
            executionContextTool.addError("server already exist");
            return;
        }
        startServer(configurationTool, executionContextTool);
        while (tomcat != null) {
            synchronized (this) {
                try {
                    this.wait(500);
                } catch (Exception e) {
                    // stopServer();
                    // e.printStackTrace();
                    // externalExecutionContextTool.addMessage("force stop server");
                    // break;
                }
            }
            if (executionContextTool.isNeedStop()) {
                stopServer(configurationTool);
                executionContextTool.addMessage("force stop server");
                break;
            }
                /*
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    stopServer();
                    // e.printStackTrace();
                    externalExecutionContextTool.addMessage("force stop server");
                    break;
                }
                */
        }
    }

    private void stop(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) {
        if (tomcat != null) {
            stopServer(configurationTool);
            executionContextTool.addMessage("stop server");
            synchronized (this) {
                this.notifyAll();
            }
        } else {
            executionContextTool.addError("server already stopped");
        }
    }

    private Long getReqId(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, Long reqId) {
        if (reqId == null || reqId == -1L) {
            reqId = configurationTool.getInfo("threadId").map(ModuleUtils::getNumber).map(n -> threadReqMap.get(n.longValue())).orElse(-1L);
            if (reqId == -1L) {
                executionContextTool.addError("need reqId");
                return null;
            }
        }
        return reqId;
    }

    private void fastResponse(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) {
        if (tomcat == null) {
            executionContextTool.addError("server not started");
            return;
        }
        LinkedList<IMessage> messages = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0)).orElse(new LinkedList<>());
        String fieldErrorCode = Optional.ofNullable(ModuleUtils.getString(messages.poll())).orElse("errorCode");
        String fieldErrorText = Optional.ofNullable(ModuleUtils.getString(messages.poll())).orElse("errorText");
        String fieldData = Optional.ofNullable(ModuleUtils.getString(messages.poll())).orElse("data");

        messages = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(1)).orElse(new LinkedList<>());
        Long reqId = Optional.ofNullable(ModuleUtils.getNumber(messages.poll())).map(Number::longValue).or(() -> Optional.of(-1L))
                .map(n -> getReqId(configurationTool, executionContextTool, n)).orElse(null);
        if (reqId == null) {
            executionContextTool.addError("need reqId");
            return;
        }
        String mimeType = Optional.ofNullable(ModuleUtils.getString(messages.poll())).orElse("application/json");
        IAction a = ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getCommands(2)).orElse(null);

        int errorCode = 0;
        String errorText = null;
        ObjectField data = null;
        byte[] content = null;
        String path = null;

        boolean errors = false;
        // if (a == null) {
        //     executionContextTool.addError("no data");
        //     return;
        // }

        LinkedList<IMessage> lst = new LinkedList<>(ModuleUtils.getErrors(a));
        errors = !lst.isEmpty();
        if (errors) {
            IMessage m = lst.poll();
            errorCode = -1;
            errorText = "";
            if (ModuleUtils.isObjectArray(m)) {
                data = new ObjectField(fieldData, ModuleUtils.getObjectArray(m));
                ObjectArray objectArray = ModuleUtils.getObjectArray(data);
                if (ModuleUtils.isArrayContainObjectElements(objectArray) && objectArray.size() == 1) {
                    ObjectElement objectElementV = (ObjectElement) objectArray.get(0);
                    errorText = objectElementV.findField(fieldErrorText).map(ModuleUtils::toString).orElse("");
                    errorCode = objectElementV.findField(fieldErrorCode).map(ModuleUtils::toNumber).map(Number::intValue).orElse(-1);
                }
            } else {
                errorText = ModuleUtils.toString(m);
                if (!lst.isEmpty()) {
                    IMessage message = lst.poll();
                    if (ModuleUtils.isNumber(message))
                        errorCode = ModuleUtils.getNumber(message).intValue();
                }
            }
        } else {
            lst = new LinkedList<>(ModuleUtils.getData(a));
            if (!lst.isEmpty()) {
                IMessage m = lst.poll();
                if (ModuleUtils.isObjectArray(m)) {
                    data = new ObjectField(fieldData, ModuleUtils.getObjectArray(m));
                    ObjectArray objectArray = ModuleUtils.getObjectArray(data);
                    if (ModuleUtils.isArrayContainObjectElements(objectArray) && objectArray.size() == 1) {
                        ObjectElement objectElementV = (ObjectElement) objectArray.get(0);
                        if (objectElementV.getFields().size() >= 2 && objectElementV.getFields().size() <= 3) {
                            Integer errorCodeV = objectElementV.findField(fieldErrorCode).map(ModuleUtils::toNumber).map(Number::intValue).orElse(null);
                            String errorTextV = objectElementV.findField(fieldErrorText).map(ModuleUtils::toString).orElse(null);
                            ObjectField dataV = objectElementV.findField(fieldData).orElse(null);
                            if (errorCodeV != null &&
                                    (errorTextV != null || errorCodeV == 0) &&
                                    (dataV != null || errorCodeV != 0)) {
                                errorCode = errorCodeV;
                                errorText = errorTextV;
                                data = dataV;
                            }
                        }
                    }
                } else if (ModuleUtils.isBytes(m)) {
                    content = ModuleUtils.getBytes(m);
                    mimeType = "application/octet-stream";
                    try (ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) content); InputStream is = new BufferedInputStream(bais)) {
                        String mimeTypeNew = URLConnection.guessContentTypeFromStream(is);
                        if (mimeTypeNew != null)
                            mimeType = mimeTypeNew;
                    } catch (Exception e) {
                        configurationTool.loggerWarn("error while get mime type fore bytes" + ModuleUtils.getErrorMessageOrClassName(e));
                    }
                } else if (lst.isEmpty() && ModuleUtils.isString(m)) {
                    path = ModuleUtils.getString(m);
                    mimeType = URLConnection.guessContentTypeFromName(path);
                    if (mimeType == null) {
                        mimeType = mimetypesFileTypeMap.getContentType(path);
                        if (mimeType == null)
                            mimeType = "text/plain";
                    }
                } else {
                    data = new ObjectField(fieldData, new ObjectArray(
                            lst.stream()
                                    .filter(m2 -> !ModuleUtils.isObjectArray(m2))
                                    .collect(Collectors.toList()),
                            ObjectType.VALUE_ANY
                    ));
                }
            } else {
                errorCode = -1;
                errorText = "no data";
            }
        }

        int resultCode = errors ? 500 : 200;
        String headerContentType = "Content-Type=" + mimeType;
        if (content == null && path == null) {
            ObjectElement objectElement = new ObjectElement(new ObjectField(fieldErrorCode, errorCode));
            if (errorText != null)
                objectElement.getFields().add(new ObjectField(fieldErrorText, errorText));
            if (data != null)
                objectElement.getFields().add(data);
            Optional<byte[]> optResult = ModuleUtils.executeAndGetMessages(executionContextTool, 0, List.of(new ObjectArray(objectElement)))
                    .map(l -> {
                        IMessage m = l.get(0);
                        byte[] result = ModuleUtils.getBytes(m);
                        if (result == null)
                            result = ModuleUtils.toString(m).getBytes();
                        return result;
                    });
            if (optResult.isPresent()) {
                content = optResult.get();
            } else {
                resultCode = 500;
                headerContentType = null;
                content = "error convert response".getBytes();
            }
        }
        List<String> headers = executionContextTool.countSource() > 3 ?
                ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getCommands(3))
                        .map(IAction::getMessages)
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(ModuleUtils::isString)
                        .map(ModuleUtils::getString)
                        .flatMap(s -> Arrays.stream(s.split("\n")))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList()) :
                new ArrayList<>();
        if (headerContentType != null)
            headers.add(headerContentType);
        fastResponse(configurationTool, executionContextTool, reqId, resultCode, headers, content, path, mimeType, 1);
    }

    private void fastResponse(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, long reqId, int resultCode, List<String> headers, byte[] content, String path, String mimeType, int startEcId) {
        ResponseObj responseObj = new ResponseObj(reqId, resultCode, headers, content, path, executionContextTool, startEcId);
        try {
            Response response = mapResponse.get(reqId);
            HttpServletResponse resp = response != null ? response.getHttpServletResponse() : null;
            if (resp != null) {
                response.setResponseObj(responseObj);
                try {
                    resultCode = handleResponse(resp, responseObj, response.getVirtualServerInfo());
                } catch (ClientAbortException e) {
                    configurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
                } catch (IOException e) {
                    configurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
                    // configurationTool.loggerTrace(ModuleUtils.getStackTraceAsString(e));
                } catch (Exception e) {
                    configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                }
            }
            configurationTool.loggerTrace(String.format("reqId=%d, code=%d, content-type=%s, path=%s", reqId, resultCode, mimeType, path));
            executionContextTool.addMessage(resultCode);
            headers.forEach(executionContextTool::addMessage);
            if (path != null) {
                executionContextTool.addMessage(path);
            } else {
                executionContextTool.addMessage(content);
            }
        } finally {
            responseObj.setWork(false);
        }
    }

    private void fileFastResponse(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) {
        if (tomcat == null) {
            executionContextTool.addError("server not started");
            return;
        }
        Long reqId = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0)).map(l -> ModuleUtils.getNumber(l.poll()))
                .map(Number::longValue).map(n -> getReqId(configurationTool, executionContextTool, n)).orElse(null);
        if (reqId == null)
            return;
        String path = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(1)).map(l -> ModuleUtils.getString(l.poll())).orElse(null);

        String mimeType = "application/json";
        byte[] content = null;
        List<String> headers = new ArrayList<>();
        if (path != null) {
            mimeType = URLConnection.guessContentTypeFromName(path);
            if (mimeType == null) {
                mimeType = mimetypesFileTypeMap.getContentType(path);
                if (mimeType == null)
                    mimeType = "text/plain";
            }
            if (executionContextTool.countSource() > 2) {
                headers.addAll(ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getCommands(2))
                        .map(IAction::getMessages)
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(ModuleUtils::isString)
                        .map(ModuleUtils::getString)
                        .flatMap(s -> Arrays.stream(s.split("\n")))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList()));
            }
            headers.add("Content-Type=" + mimeType);
        } else {
            content = "File not found".getBytes();
        }
        int resultCode = path != null ? 200 : 500;
        fastResponse(configurationTool, executionContextTool, reqId, resultCode, headers, content, path, mimeType, 0);
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        // requestCounter = null;
        if (newRequests != null) {
            newRequests.clear();
            newRequests = null;
        }
        if (requestMap != null) {
            requestMap.clear();
            requestMap = null;
        }
        stopServer(configurationTool);
        virtualServerInfoMap = null;
        threadReqMap = null;
        mapResponse = null;
        reqIdGenerator = null;
    }

    private void addServlet(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, List<VirtualServerInfo> virtualServerInfos, Method createDefaultRealm) throws InvocationTargetException, IllegalAccessException {
        if (virtualServerInfos == null || virtualServerInfos.isEmpty())
            return;
        Connector connector = null;
        VirtualServerInfo virtualServerInfo = virtualServerInfos.get(0);
        Protocol protocolLocal = protocol == Protocol.VIRTUAL ? Protocol.valueOf(virtualServerInfo.getUrl().getProtocol().toUpperCase()) : protocol;
        switch (protocolLocal) {
            case HTTP: {
                connector = new Connector();
                break;
            }
            case HTTPS: {
                connector = new Connector();
                connector.setSecure(true);
                connector.setScheme("https");
                connector.setAttribute("keyPass", virtualServerInfo.getKeyPass());
                connector.setAttribute("keyAlias", virtualServerInfo.getKeyAlias());
                connector.setAttribute("keystorePass", virtualServerInfo.getKeyStorePass());
                connector.setAttribute("keystoreFile", virtualServerInfo.getKeyStore().getAbsolutePath());
                connector.setAttribute("clientAuth", "false");
                connector.setAttribute("sslProtocol", "TLS");
                connector.setAttribute("SSLEnabled", true);
                break;
            }
            default:
                throw new ModuleException("wrong protocol");
        }
        connector.setPort(virtualServerInfo.getUrl().getPort());
        AbstractProtocol abstractProtocol = (AbstractProtocol) connector.getProtocolHandler();
        abstractProtocol.setMaxThreads(virtualServerInfo.getCountThreads());
        abstractProtocol.setAcceptCount(virtualServerInfo.getBacklog());
        if (virtualServerInfo.getAddress() != null)
            abstractProtocol.setAddress(virtualServerInfo.getAddress());
        abstractProtocol.setConnectionTimeout(virtualServerInfo.getRequestTimeout());

        // tomcat.getService().addConnector(connector);
        // tomcat.setBaseDir("");
        // tomcat.setConnector(connector);

        Service service = new StandardService();
        service.setName(virtualServerInfo.getUrl().getProtocol() + "_" + virtualServerInfo.getUrl().getHost() + "_" + virtualServerInfo.getUrl().getPort());
        service.addConnector(connector);
        tomcat.getServer().addService(service);

        Engine engine = new StandardEngine();
        // engine.setBackgroundProcessorDelay(-1);
        engine.setName(virtualServerInfo.getUrl().getHost());
        engine.setDefaultHost(virtualServerInfo.getUrl().getHost());
        engine.setRealm((Realm) createDefaultRealm.invoke(tomcat));
        service.setContainer(engine);
        tomcat.getHost().setAutoDeploy(false);

        engine.addChild(virtualServerInfo.getHost());

        Context rootCtx = tomcat.addContext(virtualServerInfo.getHost(), "", null);
        rootCtx.setSessionTimeout(virtualServerInfo.getSessionTimeout());
        rootCtx.setBackgroundProcessorDelay(10);
        String servletName = "servlet-" + virtualServerInfo.getUrlHeader();

        int countManagedExecutionContexts = externalExecutionContextTool.getFlowControlTool().countManagedExecutionContexts();
        HttpServlet servlet = new HttpServlet() {
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                ResponseObj responseObj = null;
                long reqId = 0;
                long startTime = System.currentTimeMillis();
                try {
                    // req.getSession().setMaxInactiveInterval(requestTimeout);
                    // System.out.println(req.getSession().getMaxInactiveInterval());
                    VirtualServerInfo virtualServerInfoCur = virtualServerInfos.size() > 1 ?
                            virtualServerInfos.stream()
                                    .filter(s -> s.getHost().getName().equals(req.getServerName()))
                                    .findAny()
                                    .orElse(null) :
                            virtualServerInfo;
                    List<Integer> idsForExecute = null;
                    if (virtualServerInfoCur != null) {
                        if (virtualServerInfoCur.getPatterns() != null && !virtualServerInfoCur.getPatterns().isEmpty()) {
                            String s = req.getRequestURI();
                            idsForExecute = virtualServerInfoCur.getPatterns().stream()
                                    .filter(e -> e.getValue().matcher(s).find())
                                    .filter(e -> e.getKey() < countManagedExecutionContexts)
                                    .map(e -> List.of(e.getKey()))
                                    .findFirst()
                                    .orElse(null);
                        } else {
                            // for compatibility
                            idsForExecute = Stream.iterate(0, n -> n + 1)
                                    .limit(countManagedExecutionContexts)
                                    .collect(Collectors.toList());
                        }
                    }
                    if (idsForExecute == null) {
                        responseObj = new ResponseObj(null, 404, null, "Page not found".getBytes(), null, null, 1);
                        handleResponse(resp, responseObj, virtualServerInfo);
                        return;
                    }
                    String requestOrigin = getRequestOrigin(req);
                    if (virtualServerInfo.getCorsAccessList() != null && !virtualServerInfo.getCorsAccessList().isEmpty() && requestOrigin != null) {
                        // externalConfigurationTool.loggerTrace("check cors, origin: " + requestOrigin + ", cors list size: " + virtualServerInfo.getCorsAccessList().size());
                        if (virtualServerInfo.getCorsAccessList().stream().anyMatch(p -> p.matcher(requestOrigin).find())) {
                            resp.addHeader("Access-Control-Allow-Origin", requestOrigin);
                        } else {
                            responseObj = new ResponseObj(null, 403, null, "Forbidden".getBytes(), null, null, 1);
                            handleResponse(resp, responseObj, virtualServerInfo);
                            return;
                        }
                    }
                    int idForGetResponse = idsForExecute.get(idsForExecute.size() - 1);

                    Map<Integer, RequestInputStream> requestInputStreamMap = new HashMap<>();
                    Map.Entry<Long, List<Object>> requestEntry = createRequest(req, virtualServerInfo.getRequestType(), virtualServerInfo.getMaxFileSizeFull(), requestInputStreamMap);
                    reqId = requestEntry.getKey();
                    // externalConfigurationTool.loggerTrace("New request " + reqId + " " + req.getRequestURI());
                    Response responseMain = new Response(reqId, req, resp, virtualServerInfo, requestInputStreamMap);
                    mapResponse.put(reqId, responseMain);
                    long threadId = externalExecutionContextTool.getFlowControlTool().executeParallel(
                            CommandType.EXECUTE,
                            idsForExecute,
                            requestEntry.getValue(),
                            null,
                            virtualServerInfo.getRequestTimeout());
                    boolean mapFastResponseArrived = false;
                    threadReqMap.put(threadId, requestEntry.getKey());
                    try {
                        do {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ignore) {
                            }
                        } while (externalExecutionContextTool.getFlowControlTool().isThreadActive(threadId) &&
                                !externalExecutionContextTool.isNeedStop() &&
                                (virtualServerInfo.getRequestTimeout() <= 0 || virtualServerInfo.getRequestTimeout() > System.currentTimeMillis() - startTime) &&
                                (mapResponse.containsKey(reqId) && responseMain.getResponseObj() == null));
                        if (responseMain.getResponseObj() != null) {
                            mapFastResponseArrived = true;
                            responseObj = responseMain.getResponseObj();
                            if (responseObj != null && !externalExecutionContextTool.isNeedStop() &&
                                    (virtualServerInfo.getRequestTimeout() <= 0 || virtualServerInfo.getRequestTimeout() > System.currentTimeMillis() - startTime))
                                responseObj.waitWork();
                        } else {
                            List<IMessage> response = externalExecutionContextTool.getFlowControlTool().getMessagesFromExecuted(threadId, idForGetResponse).stream()
                                    .flatMap(a -> a.getMessages().stream()/*.map(IValue::getValue)*/)
                                    .collect(Collectors.toList());
                            if (response.size() < 2) {
                                responseObj = new ResponseObj(null, 500, null, null, null, null, 1);
                            } else {
                                Number codeObject = ModuleUtils.getNumber(response.get(0));
                                int code = codeObject != null ? codeObject.intValue() : 500;
                                IMessage responseBodyObject = response.get(response.size() - 1);
                                byte[] responseBody = ModuleUtils.isString(responseBodyObject) ?
                                        (ModuleUtils.getString(responseBodyObject)).getBytes() :
                                        (ModuleUtils.isBytes(responseBodyObject) ?
                                                ModuleUtils.getBytes(responseBodyObject) :
                                                ModuleUtils.toString(responseBodyObject).getBytes());
                                List<String> headers = new ArrayList<>(response.size());
                                for (int i = 1; i < response.size() - 1; i++)
                                    headers.add(ModuleUtils.toString(response.get(i)));
                                responseObj = new ResponseObj(null, code, headers, responseBody, null, null, 1);
                            }
                        }
                    } finally {
                        threadReqMap.remove(threadId);
                        if (!mapFastResponseArrived) {
                            // if (externalExecutionContextTool.getFlowControlTool().isThreadActive(threadId))
                            //     externalConfigurationTool.loggerWarn(String.format("Thread %d steel work. Time left=%d, getRequestTimeout=%d, mapResponse contaik key=%s, getResponseObj is null=%s", threadId, System.currentTimeMillis() - startTime, virtualServerInfo.getRequestTimeout(), mapResponse.containsKey(reqId), responseMain.getResponseObj() == null));
                            externalExecutionContextTool.getFlowControlTool().releaseThread(threadId);
                        } else {
                            externalExecutionContextTool.getFlowControlTool().releaseThreadCache(threadId);
                        }
                    }
                } catch (ClientAbortException e) {
                    externalConfigurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
                } catch (IOException e) {
                    externalConfigurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
                    // externalConfigurationTool.loggerTrace(ModuleUtils.getStackTraceAsString(e));
                } catch (Exception e) {
                    externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                    // externalExecutionContextTool.addError(e.getLocalizedMessage());
                } finally {
                    Response response = mapResponse.remove(reqId);
                    if (response != null)
                        response.getRequestInputStreamMap().forEach((k, v) -> v.close());
                    if (response != null && (responseObj == null || !responseObj.isFastResponse())) {
                        try {
                            handleResponse(resp, responseObj, virtualServerInfo);
                        } catch (ClientAbortException e) {
                            externalConfigurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
                        } catch (IOException e) {
                            externalConfigurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
                            // externalConfigurationTool.loggerTrace(ModuleUtils.getStackTraceAsString(e));
                        } catch (Exception e) {
                            externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                        }
                    }
                    // externalConfigurationTool.loggerTrace("End request " + reqId);
                }
            }
        };

        Tomcat.addServlet(rootCtx, servletName, servlet);

        connector.setMaxPostSize(virtualServerInfo.getMaxPostSize());
        rootCtx.setAllowCasualMultipartParsing(virtualServerInfo.getAllowMultipartParsing());

        rootCtx.addServletMappingDecoded("/*", servletName);
    }

    private void startServer(ConfigurationTool configurationTool, ExecutionContextTool externalExecutionContextTool) {
        /*
        Thread thread = Thread.currentThread();
        // ClassLoader contextClassLoader = null;//new URLClassLoader(new URL[0], thread.getContextClassLoader());
        thread.setContextClassLoader(new URLClassLoader(new URL[0], thread.getContextClassLoader()));
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        */
        try {
            tomcat = new Tomcat();
            Method createDefaultRealm = Tomcat.class.getDeclaredMethod("createDefaultRealm");
            createDefaultRealm.setAccessible(true);

            Map<Integer, List<VirtualServerInfo>> serversList = new HashMap<>();
            List<VirtualServerInfo> servers = new ArrayList<>(virtualServerInfoMap.values());
            for (VirtualServerInfo virtualServerInfo : servers)
                serversList.computeIfAbsent(virtualServerInfo.getUrl().getPort(), k -> new ArrayList<>()).add(virtualServerInfo);
            serversList.forEach((k, v) -> {
                try {
                    addServlet(configurationTool, externalExecutionContextTool, v, createDefaultRealm);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Field field = WebappClassLoaderBase.class.getDeclaredField("JVM_THREAD_GROUP_NAMES");
            field.setAccessible(true); // Suppress Java language access checking
            List<String> strings = (List<String>) field.get(null);
            strings.add("SMC_ASYNC_MAIN");
            strings.add("SMC_ASYNC_THREADS");
            strings.add("SMC_ASYNC_SC");
            field.setAccessible(false);

            tomcat.start();

            // ((WebappClassLoaderBase) (rootCtx.getLoader()).getClassLoader()).setClearReferencesThreadLocals(false);

        } catch (Exception e) {
            stopServer(configurationTool);
            throw new ModuleException("error", e);
        } finally {
            // thread.setContextClassLoader(contextClassLoader);
        }
    }

    private void stopServer(ConfigurationTool configurationTool) {
        if (tomcat != null) {
            try {
                tomcat.stop();
            } catch (Exception e) {
                // e.printStackTrace();
                configurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
            }
            try {
                tomcat.destroy();
            } catch (Exception e) {
                // e.printStackTrace();
                configurationTool.loggerWarn(ModuleUtils.getErrorMessageOrClassName(e));
            }
            tomcat = null;
        }
    }

    private Map.Entry<Long, List<Object>> createRequest(HttpServletRequest req, RequestType requestType, Integer maxFileSizeFull, Map<Integer, RequestInputStream> requestInputStreamMap) throws IOException, ServletException {
        List<Object> request = new LinkedList<>();

        List<Map.Entry<String, String>> parameters = new LinkedList<>();
        req.getParameterMap().forEach((key, value) -> {
            if (value == null)
                return;
            for (String aValue : value)
                parameters.add(Map.entry(key, aValue));
        });

        Map<String, String> mainHeaders = new HashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            mainHeaders.put(headerName, req.getHeader(headerName));
        }

        long reqId = reqIdGenerator.incrementAndGet();
        String clientIpAddress = getClientIpAddress(req);
        if (requestType == RequestType.LIST) {
            request.add(req.getMethod());
            request.add(req.getRequestURI());
            request.add(clientIpAddress);
            request.add(req.getSession().getId());

            request.add(parameters.size());
            request.addAll(parameters.stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toList()));

            request.add(mainHeaders.size());
            mainHeaders.forEach((k, v) -> request.add(k + "=" + v));

            if (ServletFileUpload.isMultipartContent(req)) {
                ArrayList<Part> parts = new ArrayList<>(req.getParts());
                request.add(parts.size());
                for (int i = 0; i < parts.size(); i++) {
                    Part part = parts.get(i);
                    Map<String, String> headers = new HashMap<>();
                    for (String headerName : part.getHeaderNames())
                        headers.put(headerName, part.getHeader(headerName));
                    RequestInputStream requestInputStream = new RequestInputStream(part);
                    long size = requestInputStream.getSize();
                    if (size > 0 && (maxFileSizeFull == -1 || maxFileSizeFull > size)) {
                        byte[] bytes = IOUtils.toByteArray(requestInputStream.getInputStream());
                        requestInputStream.close();
                        if (bytes != null && bytes.length > 0) {
                            request.add(headers.size());
                            headers.forEach((k, v) -> request.add(k + "=" + v));
                            request.add(bytes);
                        }
                    } else {
                        request.add(headers.size());
                        headers.forEach((k, v) -> request.add(k + "=" + v));
                        request.add(size);
                    }
                    if (requestInputStream.getSize() > 0)
                        requestInputStreamMap.put(i, requestInputStream);
                }
            } else {
                RequestInputStream requestInputStream = new RequestInputStream(req);
                long size = requestInputStream.getSize();
                if (size > 0 && (maxFileSizeFull == -1 || maxFileSizeFull > size)) {
                    byte[] bytes = IOUtils.toByteArray(requestInputStream.getInputStream());
                    requestInputStream.close();
                    if (bytes != null && bytes.length > 0)
                        request.add(bytes);
                } else {
                    request.add(size);
                }
                if (requestInputStream.getSize() > 0)
                    requestInputStreamMap.put(0, requestInputStream);
            }
        } else {
            ObjectElement objectElement = new ObjectElement(
                    new ObjectField("method", req.getMethod())
                    , new ObjectField("uri", req.getRequestURI())
                    , new ObjectField("remoteAddr", clientIpAddress)
                    , new ObjectField("sessionId", req.getSession().getId())
                    , new ObjectField("reqId", reqId)
            );
            if (!parameters.isEmpty())
                objectElement.getFields().add(new ObjectField("params",
                        new ObjectElement(parameters.stream()
                                .map(e -> new ObjectField(e.getKey(), e.getValue()))
                                .collect(Collectors.toList()))));
            if (!mainHeaders.isEmpty())
                objectElement.getFields().add(new ObjectField("headers",
                        new ObjectElement(mainHeaders.entrySet().stream()
                                .map(e -> new ObjectField(e.getKey(), e.getValue()))
                                .collect(Collectors.toList()))));
            byte[] bytes = null;
            Long size = null;
            if (ServletFileUpload.isMultipartContent(req)) {
                List<ObjectElement> partElements = new ArrayList<>(req.getParts().size());
                ArrayList<Part> parts = new ArrayList<>(req.getParts());
                for (int i = 0; i < parts.size(); i++) {
                    Part part = parts.get(i);
                    Map<String, String> headers = new HashMap<>();
                    for (String headerName : part.getHeaderNames())
                        headers.put(headerName, part.getHeader(headerName));
                    RequestInputStream requestInputStream = new RequestInputStream(part);
                    size = requestInputStream.getSize();
                    bytes = null;
                    ObjectElement objectElementPart = new ObjectElement();
                    objectElementPart.getFields().add(new ObjectField("name", requestInputStream.getName()));
                    objectElementPart.getFields().add(new ObjectField("contentType", requestInputStream.getContentType()));
                    if (!headers.isEmpty()) {
                        objectElementPart.getFields().add(new ObjectField("headers",
                                new ObjectElement(headers.entrySet().stream()
                                        .map(e -> new ObjectField(e.getKey(), e.getValue()))
                                        .collect(Collectors.toList()))));
                    }
                    objectElementPart.getFields().add(new ObjectField("size", size));
                    if (size > 0 && (maxFileSizeFull == -1 || maxFileSizeFull > size)) {
                        bytes = IOUtils.toByteArray(requestInputStream.getInputStream());
                        requestInputStream.close();
                        if (bytes != null && bytes.length > 0)
                            objectElementPart.getFields().add(new ObjectField("data", bytes));
                    }
                    partElements.add(objectElementPart);
                    if (requestInputStream.getSize() > 0)
                        requestInputStreamMap.put(i, requestInputStream);
                    // if (bytes != null && bytes.length > 0 && (requestInputStream.getContentType() == null || !requestInputStream.getContentType().toLowerCase().contains("charset"))) {
                    //     byte[] bytesTmp = bytes;
                    //     parameters.stream().filter(e -> e.getKey().equals(requestInputStream.getName())).findFirst()
                    //             .ifPresent(e -> parameters.set(parameters.indexOf(e), Map.entry(e.getKey(), new String(bytesTmp, StandardCharsets.UTF_8))));
                    // }
                }
                if (!partElements.isEmpty())
                    objectElement.getFields().add(new ObjectField("multipart", new ObjectArray((List) partElements, ObjectType.OBJECT_ELEMENT)));
            } else {
                RequestInputStream requestInputStream = new RequestInputStream(req);
                size = requestInputStream.getSize();
                if (size > 0 && (maxFileSizeFull == -1 || maxFileSizeFull > size)) {
                    bytes = IOUtils.toByteArray(req.getInputStream());
                    requestInputStream.close();
                }
                if (requestInputStream.getSize() > 0)
                    requestInputStreamMap.put(0, requestInputStream);
            }
            if (bytes != null && bytes.length > 0)
                objectElement.getFields().add(new ObjectField("data", bytes));
            if (size != null)
                objectElement.getFields().add(new ObjectField("size", size));
            request.add(new ObjectArray(objectElement));
        }

        return Map.entry(reqId, request);
    }

    private int handleResponse(HttpServletResponse resp, ResponseObj responseObj, VirtualServerInfo virtualServerInfo) throws IOException {
        if (responseObj == null) {
            resp.setStatus(500);
            return 500;
        }
        int code = responseObj.getResultCode();
        responseObj.getHeaders().forEach(headerText -> {
            String[] split = headerText.split("=", 2);
            // h.getResponseHeaders().add(split[0].trim(), split[1].trim());
            if (split.length > 1)
                resp.setHeader(split[0].trim(), split[1].trim());
        });
        if (virtualServerInfo.getHeaders() != null) {
            virtualServerInfo.getHeaders().forEach(headerText -> {
                String[] split = headerText.split("=", 2);
                // h.getResponseHeaders().add(split[0].trim(), split[1].trim());
                if (split.length > 1)
                    resp.setHeader(split[0].trim(), split[1].trim());
            });
        }
        Integer fileResponsePieceSize = virtualServerInfo.getFileResponsePieceSize();
        if (responseObj.getPath() != null) {
            byte[] bytes = responseObj.getBytes(0, fileResponsePieceSize);
            Long size;
            if (bytes != null && bytes.length == fileResponsePieceSize) {
                size = responseObj.getSize();
                if (size != null) {
                    resp.setContentLengthLong(size);
                } else {
                    size = Long.MAX_VALUE;
                }
                ServletOutputStream outputStream = resp.getOutputStream();
                outputStream.write(bytes);
                for (long position = fileResponsePieceSize; position < size; position += fileResponsePieceSize) {
                    bytes = responseObj.getBytes(position, fileResponsePieceSize);
                    if (bytes == null)
                        break;
                    outputStream.write(bytes);
                    if (bytes.length != fileResponsePieceSize)
                        break;
                }
                outputStream.flush();
            } else if (bytes != null) {
                writeBytesResponse(resp, bytes);
            } else {
                code = 404;
            }
        } else {
            writeBytesResponse(resp, responseObj.getContent());
        }
        resp.setStatus(code);
        return code;
    }

    private void writeBytesResponse(HttpServletResponse resp, byte[] responseBody) throws IOException {
        if (responseBody == null || responseBody.length == 0)
            return;
        resp.setContentLength(responseBody.length);
        ServletOutputStream outputStream = resp.getOutputStream();
        outputStream.write(responseBody);
        outputStream.flush();
    }

    public String getClientIpAddress(HttpServletRequest req) {
        String ipAddress = req.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress))
            ipAddress = req.getRemoteAddr();
        if (ipAddress != null && ipAddress.contains(","))
            ipAddress = ipAddress.split(",")[0].trim();
        return ipAddress;
    }

    public String getRequestOrigin(HttpServletRequest req) {
        return req.getHeader("Origin");
    }

    public enum Protocol {
        HTTP,
        HTTPS,
        VIRTUAL
    }

    public enum RequestType {
        LIST,
        OBJECT
    }

    private enum Type {
        START, STOP, FAST_RESPONSE, FILE_FAST_RESPONSE, GET_FILE_PART, GET_PART_AS_OBJECT
    }

}
