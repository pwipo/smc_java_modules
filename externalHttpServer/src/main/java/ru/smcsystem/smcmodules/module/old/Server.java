package ru.smcsystem.smcmodules.module.old;

import com.sun.net.httpserver.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server implements Module {

    private Integer port;
    private Integer countThreads;
    private Integer requestTimeout;
    private Integer backlog;

    private enum Protocol {
        HTTP,
        HTTPS
    }

    private Protocol protocol;

    private enum Mode {
        PASSIVE,
        ACTIVE
    }

    private Mode mode;

    private volatile HttpServer httpServer;

    private AtomicLong requestCounter;

    private Map<Long, Exchanger<List<Object>>> requestMap;

    private LinkedList<List<Object>> newRequests;

    private volatile String error;

    private List<Pattern> patterns;

    private InputStream licKeyIS;
    private String licKeyPass;

    @Override
    public void start(ConfigurationTool externalConfigurationTool) throws ModuleException {
        port = (Integer) externalConfigurationTool.getSetting("port").orElseThrow(() -> new ModuleException("port setting")).getValue();
        requestTimeout = (Integer) externalConfigurationTool.getSetting("requestTimeout").orElseThrow(() -> new ModuleException("requestTimeout setting")).getValue();
        countThreads = (Integer) externalConfigurationTool.getSetting("countThreads").orElseThrow(() -> new ModuleException("countThreads setting")).getValue();
        backlog = (Integer) externalConfigurationTool.getSetting("backlog").orElseThrow(() -> new ModuleException("backlog setting")).getValue();
        mode = Mode.valueOf((String) externalConfigurationTool.getSetting("mode").orElseThrow(() -> new ModuleException("mode setting")).getValue());
        protocol = Protocol.valueOf((String) externalConfigurationTool.getSetting("protocol").orElseThrow(() -> new ModuleException("protocol setting")).getValue());
        String availablePaths = (String) externalConfigurationTool.getSetting("availablePaths").orElseThrow(() -> new ModuleException("availablePaths setting")).getValue();
        licKeyPass = (String) externalConfigurationTool.getSetting("licKeyPass").orElseThrow(() -> new ModuleException("licKeyPass setting")).getValue();

        patterns = null;
        if (StringUtils.isNotBlank(availablePaths)) {
            patterns = Arrays.stream(availablePaths.split("::"))
                    .filter(StringUtils::isNoneBlank)
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
        }
        if (Protocol.HTTPS.equals(protocol)) {
            try {
                File[] files = new File(externalConfigurationTool.getWorkDirectory()).listFiles();
                licKeyIS = new FileInputStream(files[0]);
            } catch (Exception e) {
                throw new ModuleException("error", e);
            }
        }

        requestCounter = new AtomicLong();

        requestMap = new ConcurrentHashMap<>(countThreads * 10);

        newRequests = new LinkedList<>();

        httpServer = null;
        if (Mode.PASSIVE.equals(mode)) {
            startServer(port, backlog, countThreads, h -> {
                if (patterns != null) {
                    String s = h.getRequestURI().toString();
                    if (patterns.stream().noneMatch(pattern -> pattern.matcher(s).find())) {
                        handleResponse(h, List.of(404, ""));
                        return;
                    }
                }
                long requestId = requestCounter.incrementAndGet();
                Exchanger<List<Object>> exchanger = new Exchanger<>();
                requestMap.put(requestId, exchanger);
                newRequests.add(createRequest(h, requestId));
                try {
                    handleResponse(h, exchanger.exchange(Collections.EMPTY_LIST, requestTimeout, TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (error == null)
                        error = e.getMessage();
                } finally {
                    requestMap.remove(requestId);
                }
            });
        }
    }

    @Override
    public void update(ConfigurationTool externalConfigurationTool) throws ModuleException {
        stop(externalConfigurationTool);
        start(externalConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool) throws ModuleException {
        if (Mode.PASSIVE.equals(mode)) {
            if (error != null)
                throw new ModuleException(error);
            if (externalExecutionContextTool.countSource() > 0) {
                for (int i = 0; i < externalExecutionContextTool.countSource(); i++) {
                    externalExecutionContextTool.getMessages(i).forEach(a -> {
                        try {
                            List<IMessage> messages = a.getMessages();
                            int j = 0;
                            //response
                            Long requestId = getMessageAs(messages, "LONG", j);
                            Exchanger<List<Object>> listExchanger = requestMap.get(requestId);
                            if (listExchanger == null)
                                throw new ModuleException("wrong requestId " + requestId);
                            List<Object> response = new ArrayList<>();
                            response.add(getMessageAs(messages, "INTEGER", ++j));
                            for (int z = j + 1; z < messages.size() - 1; z++) {
                                response.add(getMessageAs(messages, "STRING", z));
                            }
                            response.add(getMessageAs(messages, "BYTES", messages.size() - 1));
                            listExchanger.exchange(response);
                        } catch (Exception e) {
                            throw new ModuleException("error ", e);
                        }
                    });
                }
            } else {
                List<Object> next = newRequests.poll();
                if (next != null) {
                    for (Object value : next) {
                        String type = "STRING";
                        if (value instanceof Long) {
                            type = "LONG";
                        } else if (value instanceof Integer) {
                            type = "INTEGER";
                        } else if (value instanceof byte[]) {
                            type = "BYTES";
                        }
                    }
                    externalExecutionContextTool.addMessage(next);
                }
            }
        } else {
            if (externalExecutionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0) {
                if (httpServer != null) {
                    stopServer();
                    externalExecutionContextTool.addMessage("stop server");
                    synchronized (this) {
                        this.notifyAll();
                    }
                } else {
                    externalExecutionContextTool.addError("server already stopped");
                }
            } else {
                if (httpServer != null) {
                    externalExecutionContextTool.addError("server already exist");
                    return;
                }

                startServer(port, backlog, countThreads, h -> {
                    List<Object> response = null;
                    try {
                        List<Integer> idsForExecute = null;
                        if (patterns != null) {
                            String s = h.getRequestURI().toString();
                            for (int i = 0; i < Math.min(patterns.size(), externalExecutionContextTool.getFlowControlTool().countManagedExecutionContexts()); i++) {
                                if (patterns.get(i).matcher(s).find()) {
                                    idsForExecute = List.of(i);
                                    break;
                                }
                            }
                        } else {
                            idsForExecute = Stream.iterate(0, n -> n + 1)
                                    .limit(externalExecutionContextTool.getFlowControlTool().countManagedExecutionContexts())
                                    .collect(Collectors.toList());
                        }
                        if (idsForExecute == null) {
                            handleResponse(h, List.of(404, ""));
                            return;
                        }
                        int idForGetResponse = idsForExecute.get(idsForExecute.size() - 1);

                        List<Object> request = createRequest(h, null);
                        idsForExecute.forEach(i ->
                                externalExecutionContextTool.getFlowControlTool().executeNow(
                                        CommandType.EXECUTE,
                                        i,
                                        request));
                        /*
                        long endTime = System.currentTimeMillis() + requestTimeout;
                        do {
                            try {
                                Thread.sleep(10);
                            } catch (Exception e) {
                            }
                        }
                        while ((endTime > System.currentTimeMillis()) && externalExecutionContextTool.isExecute(idForGetResponse));
                        */
                        response = externalExecutionContextTool.getFlowControlTool().getMessagesFromExecuted(idForGetResponse).stream()
                                .flatMap(a -> a.getMessages().stream().map(IValue::getValue))
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        // executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                        externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                    } finally {
                        handleResponse(h, response);
                    }
                });

                while (httpServer != null) {
                    try {
                        synchronized (this) {
                            // Thread.sleep(100);
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                    } catch (Exception e) {
                        stopServer();
                        // e.printStackTrace();
                        externalExecutionContextTool.addMessage("force stop server");
                        break;
                    }
                }
            }
        }

    }

    @Override
    public void stop(ConfigurationTool externalConfigurationTool) throws ModuleException {
        requestCounter = null;
        newRequests.clear();
        newRequests = null;
        requestMap.clear();
        requestMap = null;
        stopServer();
        error = null;
        patterns = null;
        licKeyIS = null;
    }

    private void stopServer() {
        if (httpServer != null) {
            try {
                httpServer.stop(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            httpServer = null;
        }
    }

    private <T> T getMessageAs(List<IMessage> messages, String type, int position) {
        if (messages.size() <= position)
            throw new ModuleException("wrong format: position " + position);
        IMessage m2 = messages.get(position);
        if (!m2.getType().name().equals(type))
            throw new ModuleException("wrong format: second message value type " + m2.getType());

        T value = (T) m2.getValue();
        return value;
    }

    private void startServer(int port, int backlog, int countThreads, HttpHandler handler) {
        // Provider.Service s;
        try {
            if (Protocol.HTTP.equals(protocol)) {
                httpServer = HttpServer.create(new InetSocketAddress(port), backlog);
            } else {
                HttpsServer server = HttpsServer.create(new InetSocketAddress(port), backlog);
                server.setHttpsConfigurator(createHttpsConfigurator(licKeyIS, licKeyPass));
                httpServer = server;
            }
        } catch (Exception e) {
            throw new ModuleException("server creation error", e);
        }

        //Create a new context for the given context and handler
        httpServer.createContext("/", handler);
        httpServer.setExecutor(Executors.newFixedThreadPool(countThreads));
        // httpServer.setExecutor(null);

        httpServer.start();
    }

    private HttpsConfigurator createHttpsConfigurator(InputStream licKey, String passwordStr) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        // SSLContext ctx = SSLContext.getDefault();//new SimpleSSLContext().get();
        /*
        SSLContext ctx = SSLContext.getInstance("SSLv3");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        //Load the JKS file (located, in this case, at D:\keystore.jks, with password 'test'
        //store.load(new FileInputStream("C:\\Users\\Eclipse-workspaces\\Test\\keystore.jks"), "changeit".toCharArray());
        store.load(new FileInputStream(keystoreFile), "changeit".toCharArray());
        //init the key store, along with the password 'changeit'
        kmf.init(store, "changeit".toCharArray());
        KeyManager[] keyManagers = new KeyManager[1];
        keyManagers = kmf.getKeyManagers();
        // Init the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // It will reference the same key store as the key managers
        tmf.init(store);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        ctx.init(keyManagers, trustManagers, new SecureRandom());
        */

        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Initialise the keystore
        char[] password = passwordStr.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        // FileInputStream fis = new FileInputStream("lig.keystore");
        ks.load(licKey, password);

        // Set up the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // Set up the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        // Set up the HTTPS context and parameters
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                try {
                    // Initialise the SSL context
                    // SSLContext c = SSLContext.getDefault();
                    SSLEngine engine = sslContext.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // Get the default parameters
                    SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    private List<Object> createRequest(HttpExchange h, Long requestId) throws IOException {
        List<Object> request = new LinkedList<>();
        if (requestId != null)
            request.add(requestId);
        request.add(h.getRequestMethod());

        String requestURI = h.getRequestURI().toString();
        int indexOfUriEnd = requestURI.indexOf("?");
        String uri = indexOfUriEnd > -1 ? requestURI.substring(0, indexOfUriEnd) : requestURI;
        // String params = indexOfUriEnd > -1 ? requestURI.substring(indexOfUriEnd + 1) : "";
        request.add(uri);
        request.add(h.getRemoteAddress().toString());

        List<Map<String, String>> lstMultipartHeaders = new LinkedList<>();
        List<byte[]> lstMultipartBody = new LinkedList<>();

        byte[] bytes = IOUtils.toByteArray(h.getRequestBody());
        try {
            Map<String, Object> parametersQuery = new HashMap<>();
            Map<String, Object> parameters = new HashMap<>();

            HttpParser.parseQuery(h.getRequestURI().getRawQuery(), parametersQuery);

            List<String> parametersQueryStr = new LinkedList<>();
            parametersQuery.forEach((key, value) -> {
                if (value instanceof String) {
                    parametersQueryStr.add(key + "=" + value);
                } else if (value instanceof List) {
                    ((List) value).forEach(v -> parametersQueryStr.add(key + "=" + v));
                }
            });
            request.add(parametersQueryStr.size());
            parametersQueryStr.forEach(request::add);

            if ("post".equalsIgnoreCase(h.getRequestMethod())) {
                HttpParser.parsePost(h.getRequestHeaders().getFirst("Content-Type"), bytes, parameters, lstMultipartHeaders, lstMultipartBody);
            }

            List<String> parametersStr = new LinkedList<>();
            parameters.forEach((key, value) -> {
                if (value instanceof String) {
                    parametersStr.add(key + "=" + value);
                } else if (value instanceof List) {
                    ((List) value).forEach(v -> parametersStr.add(key + "=" + v));
                }
            });
            request.add(parameters.size());
            parametersStr.forEach(request::add);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            request.add(h.getRequestHeaders().size());
            h.getRequestHeaders().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).forEach(request::add);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!lstMultipartHeaders.isEmpty() && !lstMultipartBody.isEmpty()) {
            int size = Math.min(lstMultipartHeaders.size(), lstMultipartBody.size());
            request.add(size);
            for (int i = 0; i < size; i++) {
                Map<String, String> stringStringMap = lstMultipartHeaders.get(i);
                request.add(stringStringMap.size());
                stringStringMap.forEach((k, v) -> request.add(k + "=" + v));
                request.add(lstMultipartBody.get(i));
            }
        }

        return request;
    }

    private void handleResponse(HttpExchange h, List<Object> response) throws IOException {
        if (response == null || response.size() < 2)
            response = List.of(500, "");

        Object codeObject = response.get(0);
        int code = codeObject instanceof Number ? ((Number) codeObject).intValue() : 500;

        Object responseBodyObject = response.get(response.size() - 1);
        byte[] responseBody = responseBodyObject instanceof byte[] ? (byte[]) responseBodyObject : (responseBodyObject instanceof String ? ((String) responseBodyObject).getBytes() : "".getBytes());

        for (int i = 1; i < response.size() - 1; i++) {
            String headerText = (String) response.get(i);
            String[] split = headerText.split("=");
            h.getResponseHeaders().add(split[0].trim(), split[1].trim());
        }

        //Set the response header status and length
        h.sendResponseHeaders(code, responseBody.length);

        //Write the response string
        OutputStream os = h.getResponseBody();
        os.write(responseBody);
        os.close();
    }

}
