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
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server implements Module {

    public enum Protocol {
        HTTP,
        HTTPS,
        VIRTUAL
    }

    // private AtomicLong requestCounter;

    private Map<Long, Exchanger<List<Object>>> requestMap;

    private LinkedList<List<Object>> newRequests;

    private volatile Tomcat tomcat;
    private Map<String, VirtualServerInfo> virtualServerInfoMap;
    private Protocol protocol;

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
        List<String> availablePathsList = Arrays.stream(availablePaths.split("::"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        ObjectArray virtualServerSettings = (ObjectArray) externalConfigurationTool.getSetting("virtualServerSettings").orElseThrow(() -> new ModuleException("virtualServerSettings setting")).getValue();

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
                VirtualServerInfo virtualServerInfo = buildVirtualInfo(externalConfigurationTool, urlHeader, url, keyStoreFileNameVar, keyStorePassVar, keyAliasVar, keyPassVar, strAddressVar, paths, requestTimeoutVar, countThreadsVar, backlogVar, sessionTimeoutVar, maxPostSizeVar, allowMultipartParsingVar);
                virtualServerInfoMap.put(virtualServerInfo.getUrlHeader(), virtualServerInfo);
            }
        } else {
            String hostname = "localhost";
            try {
                Map<Integer, String> paths = new HashMap<>();
                Stream.iterate(0, n -> n + 1)
                        .limit(availablePathsList.size())
                        .forEach(id -> paths.put(id, availablePathsList.get(id)));
                VirtualServerInfo virtualServerInfo = buildVirtualInfo(externalConfigurationTool, hostname, new URL(protocol.name().toLowerCase(), hostname, port, ""), keyStoreFileName, keyStorePass, keyAlias, keyPass, strAddress, paths, requestTimeout, countThreads, backlog, sessionTimeout, maxPostSize, allowMultipartParsing);
                virtualServerInfoMap = Map.of(virtualServerInfo.getUrlHeader(), virtualServerInfo);
            } catch (Exception e) {
                throw new ModuleException("error", e);
            }
        }

        // requestCounter = new AtomicLong();
        requestMap = new ConcurrentHashMap<>(countThreads * 10);
        newRequests = new LinkedList<>();
    }

    private VirtualServerInfo buildVirtualInfo(ConfigurationTool externalConfigurationTool, String urlOrigin, URL url, String keyStoreFileName, String keyStorePass, String keyAlias, String keyPass, String strAddress, Map<Integer, String> paths, Integer requestTimeout, Integer countThreads, Integer backlog, Integer sessionTimeout, Integer maxPostSize, Boolean allowMultipartParsing) {
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
        return new VirtualServerInfo(urlOrigin, url, keyStore, keyStorePass, keyAlias, keyPass, address, patterns, requestTimeout, countThreads, backlog, sessionTimeout, maxPostSize, allowMultipartParsing);
    }

    @Override
    public void update(ConfigurationTool externalConfigurationTool) throws ModuleException {
        stop(externalConfigurationTool);
        start(externalConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool) throws ModuleException {
        int countManagedExecutionContexts = externalExecutionContextTool.getFlowControlTool().countManagedExecutionContexts();
        if (countManagedExecutionContexts == 0) {
            if (tomcat != null) {
                stopServer();
                externalExecutionContextTool.addMessage("stop server");
                synchronized (this) {
                    this.notifyAll();
                }
            } else {
                externalExecutionContextTool.addError("server already stopped");
            }
        } else {
            if (tomcat != null) {
                externalExecutionContextTool.addError("server already exist");
                return;
            }

            startServer(externalConfigurationTool, externalExecutionContextTool);

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
                if (externalExecutionContextTool.isNeedStop()) {
                    stopServer();
                    externalExecutionContextTool.addMessage("force stop server");
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
                List<Object> response = null;
                try {
                    // req.getSession().setMaxInactiveInterval(requestTimeout);
                    // System.out.println(req.getSession().getMaxInactiveInterval());

                    long startTime = System.currentTimeMillis();

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
                        handleResponse(resp, List.of(404, "Page not found"));
                        return;
                    }
                    int idForGetResponse = idsForExecute.get(idsForExecute.size() - 1);

                    List<Object> request = createRequest(req);
                    long threadId = externalExecutionContextTool.getFlowControlTool().executeParallel(
                            CommandType.EXECUTE,
                            idsForExecute,
                            request,
                            null,
                            virtualServerInfo.getRequestTimeout());
                    try {
                        do {
                            synchronized (this) {
                                try {
                                    this.wait(25);
                                } catch (InterruptedException e) {
                                }
                            }
                        } while (!externalExecutionContextTool.isNeedStop() &&
                                externalExecutionContextTool.getFlowControlTool().isThreadActive(threadId) &&
                                (virtualServerInfo.getRequestTimeout() <= 0 || virtualServerInfo.getRequestTimeout() > System.currentTimeMillis() - startTime));
                        response = externalExecutionContextTool.getFlowControlTool().getMessagesFromExecuted(threadId, idForGetResponse).stream()
                                .flatMap(a -> a.getMessages().stream().map(IValue::getValue))
                                .collect(Collectors.toList());
                    } finally {
                        externalExecutionContextTool.getFlowControlTool().releaseThread(threadId);
                    }
                } catch (Exception e) {
                    externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                    // externalExecutionContextTool.addError(e.getLocalizedMessage());
                } finally {
                    handleResponse(resp, response);
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

    private List<Object> createRequest(HttpServletRequest req) throws IOException, ServletException {
        List<Object> request = new LinkedList<>();
        request.add(req.getMethod());
        request.add(req.getRequestURI());
        request.add(req.getRemoteAddr());
        request.add(req.getSession().getId());

        List<String> parameters = new LinkedList<>();
        req.getParameterMap().forEach((key, value) -> {
            if (value == null)
                return;
            for (String aValue : value)
                parameters.add(key + "=" + aValue);
        });
        request.add(parameters.size());
        request.addAll(parameters);

        Map<String, String> mainHeaders = new HashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            mainHeaders.put(headerName, req.getHeader(headerName));
        }
        request.add(mainHeaders.size());
        mainHeaders.forEach((k, v) -> request.add(k + "=" + v));

        if (ServletFileUpload.isMultipartContent(req)) {
            request.add(req.getParts().size());
            for (Part part : req.getParts()) {

                Map<String, String> headers = new HashMap<>();
                for (String headerName : part.getHeaderNames()) {
                    headers.put(headerName, part.getHeader(headerName));
                }

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

        return request;
    }

    private void handleResponse(HttpServletResponse resp, List<Object> response) throws IOException {
        if (response == null || response.size() < 2)
            response = List.of(500, "");

        Object codeObject = response.get(0);
        int code = codeObject instanceof Number ? ((Number) codeObject).intValue() : 500;

        Object responseBodyObject = response.get(response.size() - 1);
        byte[] responseBody = responseBodyObject instanceof byte[] ? (byte[]) responseBodyObject : (responseBodyObject instanceof String ? ((String) responseBodyObject).getBytes() : "".getBytes());

        for (int i = 1; i < response.size() - 1; i++) {
            String headerText = (String) response.get(i);
            String[] split = headerText.split("=", 2);
            // h.getResponseHeaders().add(split[0].trim(), split[1].trim());
            if (split.length > 1)
                resp.setHeader(split[0].trim(), split[1].trim());
        }

        //Set the response header status and length
        resp.setStatus(code);

        resp.setContentLength(responseBody.length);

        ServletOutputStream outputStream = resp.getOutputStream();
        outputStream.write(responseBody);
        outputStream.flush();
    }

}
