package ru.smcsystem.smcmodules.module;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Realm;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.AbstractProtocol;
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

    public enum Protocol {
        HTTP,
        HTTPS,
        VIRTUAL
    }

    public enum RequestType {
        LIST,
        OBJECT
    }

    // private AtomicLong requestCounter;

    private Map<Long, Exchanger<List<Object>>> requestMap;

    private LinkedList<List<Object>> newRequests;

    private volatile Tomcat tomcat;
    private Map<String, VirtualServerInfo> virtualServerInfoMap;
    private Protocol protocol;
    private RequestType requestType;
    private AtomicLong reqIdGenerator;
    private Map<Long, Response> mapResponse;

    private enum Type {
        START, STOP, FAST_RESPONSE, FILE_FAST_RESPONSE
    }

    private Integer fileResponsePieceSize;
    private MimetypesFileTypeMap mimetypesFileTypeMap;

    @Override
    public void start(ConfigurationTool externalConfigurationTool) throws ModuleException {
        protocol = Protocol.valueOf((String) externalConfigurationTool.getSetting("protocol").orElseThrow(() -> new ModuleException("protocol setting")).getValue());
        Integer port = (Integer) externalConfigurationTool.getSetting("port").orElseThrow(() -> new ModuleException("port setting")).getValue();
        Integer requestTimeout = (Integer) externalConfigurationTool.getSetting("requestTimeout").orElseThrow(() -> new ModuleException("requestTimeout setting")).getValue();
        Integer countThreads = (Integer) externalConfigurationTool.getSetting("countThreads").orElseThrow(() -> new ModuleException("countThreads setting")).getValue();
        Integer backlog = (Integer) externalConfigurationTool.getSetting("backlog").orElseThrow(() -> new ModuleException("backlog setting")).getValue();
        String availablePaths = (String) externalConfigurationTool.getSetting("availablePaths").orElseThrow(() -> new ModuleException("availablePaths setting")).getValue();
        String keyStoreFileName = (String) externalConfigurationTool.getSetting("keyStoreFileName").orElseThrow(() -> new ModuleException("keyStoreFileName setting")).getValue();
        String keyStorePass = (String) externalConfigurationTool.getSetting("keyStorePass").orElseThrow(() -> new ModuleException("keyStorePass setting")).getValue();
        String keyAlias = (String) externalConfigurationTool.getSetting("keyAlias").orElseThrow(() -> new ModuleException("keyAlias setting")).getValue();
        String keyPass = (String) externalConfigurationTool.getSetting("keyPass").orElseThrow(() -> new ModuleException("keyPass setting")).getValue();
        Integer sessionTimeout = (Integer) externalConfigurationTool.getSetting("sessionTimeout").orElseThrow(() -> new ModuleException("sessionTimeout setting")).getValue();
        Integer maxPostSize = (Integer) externalConfigurationTool.getSetting("maxPostSize").orElseThrow(() -> new ModuleException("maxPostSize setting")).getValue();
        Boolean allowMultipartParsing = Boolean.valueOf((String) externalConfigurationTool.getSetting("allowMultipartParsing").orElseThrow(() -> new ModuleException("allowMultipartParsing setting")).getValue());
        String strAddress = (String) externalConfigurationTool.getSetting("bindAddress").orElseThrow(() -> new ModuleException("bindAddress setting")).getValue();
        fileResponsePieceSize = (Integer) externalConfigurationTool.getSetting("fileResponsePieceSize").orElseThrow(() -> new ModuleException("fileResponsePieceSize setting")).getValue();
        ObjectArray headersArr = (ObjectArray) externalConfigurationTool.getSetting("headers").orElseThrow(() -> new ModuleException("headers setting")).getValue();
        List<String> availablePathsList = Arrays.stream(availablePaths.split("::"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        ObjectArray virtualServerSettings = (ObjectArray) externalConfigurationTool.getSetting("virtualServerSettings").orElseThrow(() -> new ModuleException("virtualServerSettings setting")).getValue();
        requestType = RequestType.valueOf((String) externalConfigurationTool.getSetting("requestType").orElseThrow(() -> new ModuleException("requestType setting")).getValue());
        reqIdGenerator = new AtomicLong();
        reqIdGenerator.compareAndSet(Long.MAX_VALUE, 0);
        mapResponse = new ConcurrentHashMap<>();

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
                URL url = null;
                try {
                    url = new URL(protocolVar.name().toLowerCase(), hostname, portVar, "");
                } catch (Exception e) {
                    throw new ModuleException("error", e);
                }
                String urlHeader = String.format("%s://%s:%d", url.getProtocol(), url.getHost(), url.getPort());
                Map<Integer, String> paths = new HashMap<>();
                for (int j = 0; j < availablePathsList.size(); j++) {
                    String path = availablePathsList.get(j);
                    if (path.isBlank())
                        continue;
                    if (i == 0 && (path.startsWith("/") || path.startsWith("^/") || !path.toUpperCase().startsWith("HTTP"))) {
                        paths.put(j, path);
                    } else if (path.toUpperCase().startsWith(urlHeader.toUpperCase())) {
                        paths.put(j, path.substring(urlHeader.length()));
                    }
                }
                VirtualServerInfo virtualServerInfo = buildVirtualInfo(externalConfigurationTool, urlHeader, url, keyStoreFileNameVar, keyStorePassVar,
                        keyAliasVar, keyPassVar, strAddressVar, paths, requestTimeoutVar, countThreadsVar, backlogVar, sessionTimeoutVar, maxPostSizeVar,
                        allowMultipartParsingVar, headersArrVar, requestTypeVar);
                virtualServerInfoMap.put(virtualServerInfo.getUrlHeader(), virtualServerInfo);
            }
        } else {
            String hostname = "localhost";
            try {
                Map<Integer, String> paths = new HashMap<>();
                Stream.iterate(0, n -> n + 1)
                        .limit(availablePathsList.size())
                        .forEach(id -> paths.put(id, availablePathsList.get(id)));
                VirtualServerInfo virtualServerInfo = buildVirtualInfo(externalConfigurationTool, hostname, new URL(protocol.name().toLowerCase(), hostname, port, ""),
                        keyStoreFileName, keyStorePass, keyAlias, keyPass, strAddress, paths, requestTimeout, countThreads, backlog, sessionTimeout,
                        maxPostSize, allowMultipartParsing, headersArr, requestType);
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
                                               String strAddress, Map<Integer, String> paths, Integer requestTimeout, Integer countThreads,
                                               Integer backlog, Integer sessionTimeout, Integer maxPostSize, Boolean allowMultipartParsing,
                                               ObjectArray headersArr, RequestType requestTypeVar) {
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
        Map<Integer, Pattern> patterns = null;
        if (paths != null && !paths.isEmpty()) {
            patterns = paths.entrySet().stream()
                    .filter(e -> StringUtils.isNotBlank(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Pattern.compile(e.getValue())));
        }
        List<String> headers = null;
        if (headersArr != null && headersArr.size() > 0 && headersArr.isSimple()) {
            headers = new ArrayList<>();
            for (int i = 0; i < headersArr.size(); i++)
                headers.add(headersArr.get(i).toString());
        }
        return new VirtualServerInfo(urlOrigin, url, keyStore, keyStorePass, keyAlias, keyPass, address, patterns,
                requestTimeout, countThreads, backlog, sessionTimeout, maxPostSize, allowMultipartParsing, headers, requestTypeVar);
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
                }
            }
        });
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
                stopServer();
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
            stopServer();
            executionContextTool.addMessage("stop server");
            synchronized (this) {
                this.notifyAll();
            }
        } else {
            executionContextTool.addError("server already stopped");
        }
    }

    private void fastResponse(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) {
        if (tomcat == null) {
            executionContextTool.addError("server not started");
            return;
        }
        Optional<LinkedList<IMessage>> names = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0));
        if (names.isEmpty() || names.get().size() < 3) {
            executionContextTool.addError("need 3 field names");
            return;
        }
        String fieldErrorCode = ModuleUtils.getString(names.get().get(0));
        String fieldErrorText = ModuleUtils.getString(names.get().get(1));
        String fieldData = ModuleUtils.getString(names.get().get(2));
        if (fieldErrorCode == null || fieldErrorText == null || fieldData == null) {
            executionContextTool.addError("wrong field names");
            return;
        }
        Long reqId = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(1)).map(l -> ModuleUtils.getNumber(l.poll())).map(Number::longValue).orElse(null);
        if (reqId == null) {
            executionContextTool.addError("need reqId");
            return;
        }
        IAction a = ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getCommands(2)).orElse(null);

        int errorCode = 0;
        String errorText = null;
        ObjectField data = null;
        String mimeType = "application/json";
        byte[] content = null;
        String path = null;

        boolean errors = false;
        // if (a == null) {
        //     executionContextTool.addError("no data");
        //     return;
        // }

        List<IMessage> lst = ModuleUtils.getErrors(a);
        errors = !lst.isEmpty();
        if (errors) {
            errorText = ModuleUtils.toString(lst.get(0));
            errorCode = -1;
            if (lst.size() > 1 && ModuleUtils.isNumber(lst.get(1)))
                errorCode = ModuleUtils.getNumber(lst.get(1)).intValue();
        } else {
            lst = ModuleUtils.getData(a);
            if (!lst.isEmpty()) {
                IMessage m = lst.get(0);
                if (ModuleUtils.isObjectArray(m)) {
                    data = new ObjectField(fieldData, ModuleUtils.getObjectArray(m));
                } else if (ModuleUtils.isBytes(m)) {
                    content = ModuleUtils.getBytes(m);
                    mimeType = "application/octet-stream";
                    try (ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) content); InputStream is = new BufferedInputStream(bais)) {
                        String mimeTypeNew = URLConnection.guessContentTypeFromStream(is);
                        if (mimeTypeNew != null)
                            mimeType = mimeTypeNew;
                    } catch (Exception e) {
                        configurationTool.loggerWarn("error while get mime type fore bytes" + e.getMessage());
                    }
                } else if (lst.size() == 1 && ModuleUtils.isString(m)) {
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
                    resultCode = handleResponse(resp, responseObj, response.getVirtualServerInfo().getHeaders());
                } catch (IOException e) {
                    configurationTool.loggerWarn(e.getMessage());
                    configurationTool.loggerTrace(ModuleUtils.getStackTraceAsString(e));
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
        Long reqId = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0)).map(l -> ModuleUtils.getNumber(l.poll())).map(Number::longValue).orElse(null);
        if (reqId == null) {
            executionContextTool.addError("need reqId");
            return;
        }
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
    public void stop(ConfigurationTool externalConfigurationTool) throws ModuleException {
        // requestCounter = null;
        newRequests.clear();
        newRequests = null;
        requestMap.clear();
        requestMap = null;
        stopServer();
        virtualServerInfoMap = null;
        mapResponse = null;
        reqIdGenerator = null;
    }

    private void addServlet(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, VirtualServerInfo virtualServerInfo, Method createDefaultRealm) throws InvocationTargetException, IllegalAccessException {
        Connector connector = null;
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
        engine.setName(virtualServerInfo.getUrl().getHost());
        engine.setDefaultHost(virtualServerInfo.getUrl().getHost());
        engine.setRealm((Realm) createDefaultRealm.invoke(tomcat));
        service.setContainer(engine);

        engine.addChild(virtualServerInfo.getHost());

        Context rootCtx = tomcat.addContext(virtualServerInfo.getHost(), "", null);
        rootCtx.setSessionTimeout(virtualServerInfo.getSessionTimeout());
        rootCtx.setBackgroundProcessorDelay(10);
        String servletName = "servlet-" + virtualServerInfo.getUrlHeader();

        int countManagedExecutionContexts = externalExecutionContextTool.getFlowControlTool().countManagedExecutionContexts();
        Set<Map.Entry<Integer, Pattern>> patternEntries = virtualServerInfo.getPatterns() != null ? virtualServerInfo.getPatterns().entrySet() : null;
        HttpServlet servlet = new HttpServlet() {
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                ResponseObj responseObj = null;
                long reqId = 0;
                long startTime = System.currentTimeMillis();
                try {
                    // req.getSession().setMaxInactiveInterval(requestTimeout);
                    // System.out.println(req.getSession().getMaxInactiveInterval());
                    List<Integer> idsForExecute = null;
                    if (patternEntries != null) {
                        String s = req.getRequestURI();
                        idsForExecute = patternEntries.stream()
                                .filter(e -> e.getValue().matcher(s).find())
                                .filter(e -> e.getKey() < countManagedExecutionContexts)
                                .map(e -> List.of(e.getKey()))
                                .findAny().orElse(null);
                    } else {
                        idsForExecute = Stream.iterate(0, n -> n + 1)
                                .limit(countManagedExecutionContexts)
                                .collect(Collectors.toList());
                    }
                    if (idsForExecute == null) {
                        responseObj = new ResponseObj(null, 404, null, "Page not found".getBytes(), null, null, 1);
                        return;
                    }
                    int idForGetResponse = idsForExecute.get(idsForExecute.size() - 1);

                    Map.Entry<Long, List<Object>> requestEntry = createRequest(req, virtualServerInfo.getRequestType());
                    reqId = requestEntry.getKey();
                    Response responseMain = new Response(reqId, resp, virtualServerInfo);
                    mapResponse.put(reqId, responseMain);
                    long threadId = externalExecutionContextTool.getFlowControlTool().executeParallel(
                            CommandType.EXECUTE,
                            idsForExecute,
                            requestEntry.getValue(),
                            null,
                            virtualServerInfo.getRequestTimeout());
                    boolean mapFastResponseArrived = false;
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
                        if (!mapFastResponseArrived) {
                            // if (externalExecutionContextTool.getFlowControlTool().isThreadActive(threadId))
                            //     externalConfigurationTool.loggerWarn(String.format("Thread %d steel work. Time left=%d, getRequestTimeout=%d, mapResponse contaik key=%s, getResponseObj is null=%s", threadId, System.currentTimeMillis() - startTime, virtualServerInfo.getRequestTimeout(), mapResponse.containsKey(reqId), responseMain.getResponseObj() == null));
                            externalExecutionContextTool.getFlowControlTool().releaseThread(threadId);
                        } else {
                            externalExecutionContextTool.getFlowControlTool().releaseThreadCache(threadId);
                        }
                    }
                } catch (Exception e) {
                    externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                    // externalExecutionContextTool.addError(e.getLocalizedMessage());
                } finally {
                    if (mapResponse.remove(reqId) != null && (responseObj == null || !responseObj.isFastResponse())) {
                        try {
                            handleResponse(resp, responseObj, virtualServerInfo.getHeaders());
                        } catch (IOException e) {
                            externalConfigurationTool.loggerWarn(e.getMessage());
                            externalConfigurationTool.loggerTrace(ModuleUtils.getStackTraceAsString(e));
                        } catch (Exception e) {
                            externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                        }
                    }
                    externalConfigurationTool.loggerTrace("End response " + reqId);
                }
            }
        };

        Tomcat.addServlet(rootCtx, servletName, servlet);

        connector.setMaxPostSize(virtualServerInfo.getMaxPostSize());
        rootCtx.setAllowCasualMultipartParsing(virtualServerInfo.getAllowMultipartParsing());

        rootCtx.addServletMappingDecoded("/*", servletName);
    }

    private void startServer(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool) {
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

            virtualServerInfoMap.values().forEach(v -> {
                try {
                    addServlet(externalConfigurationTool, externalExecutionContextTool, v, createDefaultRealm);
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
            stopServer();
            throw new ModuleException("error", e);
        } finally {
            // thread.setContextClassLoader(contextClassLoader);
        }
    }

    private void stopServer() {
        if (tomcat != null) {
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
            tomcat = null;
        }
    }

    private Map.Entry<Long, List<Object>> createRequest(HttpServletRequest req, RequestType requestType) throws IOException, ServletException {
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
        if (requestType == RequestType.LIST) {
            request.add(req.getMethod());
            request.add(req.getRequestURI());
            request.add(req.getRemoteAddr());
            request.add(req.getSession().getId());

            request.add(parameters.size());
            request.addAll(parameters.stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toList()));

            request.add(mainHeaders.size());
            mainHeaders.forEach((k, v) -> request.add(k + "=" + v));

            if (ServletFileUpload.isMultipartContent(req)) {
                request.add(req.getParts().size());
                for (Part part : req.getParts()) {
                    Map<String, String> headers = new HashMap<>();
                    for (String headerName : part.getHeaderNames())
                        headers.put(headerName, part.getHeader(headerName));
                    byte[] bytes = IOUtils.toByteArray(part.getInputStream());
                    if (bytes != null && bytes.length > 0) {
                        request.add(headers.size());
                        headers.forEach((k, v) -> request.add(k + "=" + v));
                        request.add(bytes);
                    }
                }
            } else {
                byte[] bytes = IOUtils.toByteArray(req.getInputStream());
                if (bytes != null && bytes.length > 0)
                    request.add(bytes);
            }
        } else {
            ObjectElement objectElement = new ObjectElement(
                    new ObjectField("method", req.getMethod())
                    , new ObjectField("uri", req.getRequestURI())
                    , new ObjectField("remoteAddr", req.getRemoteAddr())
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
            if (ServletFileUpload.isMultipartContent(req)) {
                List<ObjectElement> parts = new ArrayList<>(req.getParts().size());
                for (Part part : req.getParts()) {
                    Map<String, String> headers = new HashMap<>();
                    for (String headerName : part.getHeaderNames())
                        headers.put(headerName, part.getHeader(headerName));
                    bytes = IOUtils.toByteArray(part.getInputStream());
                    if (bytes != null && bytes.length > 0) {
                        ObjectElement objectElementPart = new ObjectElement();
                        objectElementPart.getFields().add(new ObjectField("name", part.getName()));
                        objectElementPart.getFields().add(new ObjectField("contentType", part.getContentType()));
                        if (!headers.isEmpty()) {
                            objectElementPart.getFields().add(new ObjectField("headers",
                                    new ObjectElement(headers.entrySet().stream()
                                            .map(e -> new ObjectField(e.getKey(), e.getValue()))
                                            .collect(Collectors.toList()))));
                        }
                        objectElementPart.getFields().add(new ObjectField("data", bytes));
                        parts.add(objectElementPart);
                    }
                }
                if (!parts.isEmpty())
                    objectElement.getFields().add(new ObjectField("multipart", new ObjectArray((List) parts, ObjectType.OBJECT_ELEMENT)));
            } else {
                bytes = IOUtils.toByteArray(req.getInputStream());
            }
            if (bytes != null && bytes.length > 0)
                objectElement.getFields().add(new ObjectField("data", bytes));
            request.add(new ObjectArray(objectElement));
        }

        return Map.entry(reqId, request);
    }

    private int handleResponse(HttpServletResponse resp, ResponseObj responseObj, List<String> headers) throws IOException {
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
        if (headers != null) {
            headers.forEach(headerText -> {
                String[] split = headerText.split("=", 2);
                // h.getResponseHeaders().add(split[0].trim(), split[1].trim());
                if (split.length > 1)
                    resp.setHeader(split[0].trim(), split[1].trim());
            });
        }
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
                try {
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

}
