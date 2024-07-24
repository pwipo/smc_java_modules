package ru.smcsystem.smcmodules.module;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class ServerTest {

    @Test
    public void process() {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(ValueType.INTEGER, 8080),
                "requestTimeout", new Value(ValueType.INTEGER, 1000),
                "countThreads", new Value(ValueType.INTEGER, 10),
                "backlog", new Value(ValueType.INTEGER, 0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value(ValueType.STRING, "HTTP"),
                "availablePaths", new Value(ValueType.STRING, " "),
                "keyStoreFileName", new Value(ValueType.STRING, " "),
                "keyStorePass", new Value(ValueType.STRING, " "),
                "keyPass", new Value(ValueType.STRING, " "),
                "keyAlias", new Value(ValueType.STRING, " ")
        ));
        settings.put("bindAddress", new Value(ValueType.STRING, " "));
        settings.put("sessionTimeout", new Value(ValueType.INTEGER, 30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("virtualServerSettings", new Value(ValueType.OBJECT_ARRAY, new ObjectArray()));
        settings.put("requestType", new Value("LIST"));
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        "",
                        settings,
                        null,
                        null
                ),
                new Server()
        );


        process.start();

        List<IMessage> allMessages = new ArrayList<>();

        new Thread(() -> {
            try {
                System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/hello"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        long currentTimeMillis = System.currentTimeMillis();
        List<IMessage> messages = null;
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            messages = process.execute(executionContextTool);
        } while (executionContextTool.getOutput().size() == 0);
        allMessages.addAll(messages);
        System.out.println(System.currentTimeMillis() - currentTimeMillis);

        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        Long requestId = (Long) executionContextTool.getOutput().get(0).getValue();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, requestId)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 200)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTES, "hi world".getBytes()))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        allMessages.addAll(process.execute(executionContextTool));

        process.update();

        new Thread(() -> {
            try {
                System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/hello?kf=45"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        currentTimeMillis = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            messages = process.execute(executionContextTool);
        } while (executionContextTool.getOutput().size() == 0);
        allMessages.addAll(messages);
        System.out.println(System.currentTimeMillis() - currentTimeMillis);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        requestId = (Long) executionContextTool.getOutput().get(0).getValue();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, requestId)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 200)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTES, "hi world2".getBytes()))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        allMessages.addAll(process.execute(executionContextTool));

        allMessages.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                List.of(
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1))),
                                ActionType.EXECUTE),
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2))),
                                ActionType.EXECUTE)
                ));
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process2() throws InterruptedException {
        process2Prv();
        process2Prv();
    }

    private void process2Prv() throws InterruptedException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(ValueType.INTEGER, 8080),
                "requestTimeout", new Value(ValueType.INTEGER, 20000),
                "countThreads", new Value(ValueType.INTEGER, 10),
                "backlog", new Value(ValueType.INTEGER, 0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value(ValueType.STRING, "HTTP"),
                "availablePaths", new Value(ValueType.STRING, ".*"),
                "keyStoreFileName", new Value(ValueType.STRING, " "),
                "keyStorePass", new Value(ValueType.STRING, " "),
                "keyPass", new Value(ValueType.STRING, " "),
                "keyAlias", new Value(ValueType.STRING, " ")
        ));
        settings.put("bindAddress", new Value(ValueType.STRING, " "));
        settings.put("sessionTimeout", new Value(ValueType.INTEGER, 30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("virtualServerSettings", new Value(ValueType.OBJECT_ARRAY, new ObjectArray()));
        settings.put("requestType", new Value("OBJECT"));

        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\WebServer\\old\\keys"
                ),
                new Server()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(200))
                                        , new Message(MessageType.DATA, new Date(), new Value("hi".getBytes()))
                                ),
                                ActionType.EXECUTE)
                ));
        // process.execute(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().clear();

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
        });
        thread.start();

        try {
            Thread.sleep(100);
            System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/hello"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool2);

        thread.join();

        process.stop();
    }

    @Test
    public void process3() throws InterruptedException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(ValueType.INTEGER, 8080),
                "requestTimeout", new Value(ValueType.INTEGER, 20000),
                "countThreads", new Value(ValueType.INTEGER, 10),
                "backlog", new Value(ValueType.INTEGER, 0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value(ValueType.STRING, "HTTPS"),
                "availablePaths", new Value(ValueType.STRING, ".*"),
                "keyStoreFileName", new Value(ValueType.STRING, "lig.keystore"),
                "keyStorePass", new Value(ValueType.STRING, "simulator"),
                "keyPass", new Value(ValueType.STRING, "simulator"),
                "keyAlias", new Value(ValueType.STRING, "self_signed")
        ));
        settings.put("bindAddress", new Value(ValueType.STRING, " "));
        settings.put("sessionTimeout", new Value(ValueType.INTEGER, 30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("virtualServerSettings", new Value(ValueType.OBJECT_ARRAY, new ObjectArray()));
        settings.put("requestType", new Value("OBJECT"));
        settings.put("fileResponsePieceSize", new Value(1048576));
        settings.put("headers", new Value(new ObjectArray(List.of("Access-Control-Allow-Origin=*", "Access-Control-Allow-Methods=POST, GET, PUT, DELETE, OPTIONS", "Access-Control-Allow-Headers=Content-Type"), ObjectType.STRING)));

        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\WebServer\\old\\keys"
                ),
                new Server()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                null,
                List.of(
                        list -> {
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(200))
                                            , new Message(MessageType.DATA, new Date(), new Value("hi".getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        }
                )
        );

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
        });
        thread.start();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                SSLContexts.custom()
                                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                        .build()
                                , NoopHostnameVerifier.INSTANCE
                        )
                );
        // .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);

        for (int i = 0; i < 1; i++) {
            try {
                // System.out.println("response " + i);
                System.out.println("Response: " + sendingGetRequest(httpClientBuilder, "https://localhost:8080/hello"));
                // Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool2);

        thread.join();

        process.stop();
    }

    private static String sendingGetRequest(HttpClientBuilder builder, String url) throws IOException, InterruptedException {
        String token = null;
        try (CloseableHttpClient client = builder.build()) {
            HttpGet request = new HttpGet(url);
            request.addHeader(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT);

            try (CloseableHttpResponse response = client.execute(request)) {
                Header[] allHeaders = response.getAllHeaders();
                System.out.println("headers: " + Arrays.stream(allHeaders).map(h -> h.getName() + "=" + h.getValue()).collect(Collectors.joining(", ")));
                HttpEntity entity = response.getEntity();
                // token = IOUtils.toString(entity.getContent());
                token = EntityUtils.toString(entity);
                // Thread.sleep(10000);
            }
            request.releaseConnection();
        }
        return token;
    }


    @Test
    public void process4() throws InterruptedException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(8080),
                "requestTimeout", new Value(20000),
                "countThreads", new Value(10),
                "backlog", new Value(0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value("HTTP"),
                "availablePaths", new Value(".*"),
                "keyStoreFileName", new Value(" "),
                "keyStorePass", new Value(" "),
                "keyPass", new Value(" "),
                "keyAlias", new Value(" ")
        ));
        settings.put("bindAddress", new Value(ValueType.STRING, " "));
        settings.put("sessionTimeout", new Value(ValueType.INTEGER, 30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("virtualServerSettings", new Value(ValueType.OBJECT_ARRAY, new ObjectArray()));
        settings.put("requestType", new Value("OBJECT"));
        settings.put("fileResponsePieceSize", new Value(1048576));
        settings.put("headers", new Value(new ObjectArray(List.of("Access-Control-Allow-Origin=*", "Access-Control-Allow-Methods=POST, GET, PUT, DELETE, OPTIONS", "Access-Control-Allow-Headers=Content-Type"), ObjectType.STRING)));

        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\WebServer\\old\\keys"
                ),
                new Server()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(200))
                                        , new Message(MessageType.DATA, new Date(), new Value("hi".getBytes()))
                                ),
                                ActionType.EXECUTE)
                ));
        // process.execute(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().clear();

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
        });
        thread.start();

        try {
            Thread.sleep(100);
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            try (CloseableHttpClient client = httpClientBuilder.build()) {
                HttpPost request = new HttpPost("http://localhost:8080/hello");
                request.addHeader(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT);

                request.setEntity(
                        MultipartEntityBuilder.create()
                                .setContentType(ContentType.MULTIPART_FORM_DATA)
                                .setCharset(Charset.forName("UTF-8"))
                                .addTextBody("uuid", "uuid")
                                .addTextBody("provider", "provider")
                                .addTextBody("author", "author")
                                .addTextBody("contacts", "contacts")
                                .addTextBody("homepage", "homepage")
                                .addTextBody("version", "version")
                                .addTextBody("api_version", "api_version")
                                .addTextBody("locale", "locale")
                                .addTextBody("comment", "comment")
                                .addBinaryBody("data", new byte[]{1, 2, 3, 4, 5})
                                .build());

                try (CloseableHttpResponse response = client.execute(request)) {
                    HttpEntity entity = response.getEntity();
                    String content = EntityUtils.toString(entity);
                    System.out.println("Response: " + content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool2);

        thread.join();

        process.stop();
    }

    @Test
    public void process5() throws InterruptedException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(8080),
                "requestTimeout", new Value(20000),
                "countThreads", new Value(10),
                "backlog", new Value(0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value("VIRTUAL"),
                "availablePaths", new Value(".*::https://localhost:8081/test::https://service.smcsystem.ru:8082/store"),
                "keyStoreFileName", new Value(""),
                "keyStorePass", new Value(""),
                "keyPass", new Value(""),
                "keyAlias", new Value("")
        ));
        settings.put("bindAddress", new Value(""));
        settings.put("sessionTimeout", new Value(30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("requestType", new Value("OBJECT"));
        settings.put("virtualServerSettings", new Value(new ObjectArray(
                new ObjectElement(
                        new ObjectField("protocol", "HTTP")
                        , new ObjectField("hostname", "localhost")
                        , new ObjectField("port", 8080)
                        , new ObjectField("keyStoreFileName", "")
                        , new ObjectField("keyStorePass", "")
                        , new ObjectField("keyAlias", "")
                        , new ObjectField("keyPass", "")
                        , new ObjectField("bindAddress", "")
                        , new ObjectField("requestTimeout", 20000)
                        , new ObjectField("countThreads", 10)
                        , new ObjectField("backlog", 0)
                        , new ObjectField("sessionTimeout", 30)
                        , new ObjectField("maxPostSize", 10485760)
                        , new ObjectField("allowMultipartParsing", "false")
                ),
                new ObjectElement(
                        new ObjectField("protocol", "HTTPS")
                        , new ObjectField("hostname", "localhost")
                        , new ObjectField("port", 8081)
                        , new ObjectField("keyStoreFileName", "lig.keystore")
                        , new ObjectField("keyStorePass", "simulator")
                        , new ObjectField("keyPass", "simulator")
                        , new ObjectField("keyAlias", "self_signed")
                        , new ObjectField("bindAddress", "")
                ),
                new ObjectElement(
                        new ObjectField("protocol", "HTTPS")
                        , new ObjectField("hostname", "service.smcsystem.ru")
                        , new ObjectField("port", 8082)
                        , new ObjectField("keyStoreFileName", "lig2.keystore")
                        , new ObjectField("keyStorePass", "simulator")
                        , new ObjectField("keyPass", "simulator")
                        , new ObjectField("keyAlias", "self_signed")
                        , new ObjectField("bindAddress", "")
                )
        )));
        settings.put("fileResponsePieceSize", new Value(1048576));
        settings.put("headers", new Value(new ObjectArray(List.of("Access-Control-Allow-Origin=*", "Access-Control-Allow-Methods=POST, GET, PUT, DELETE, OPTIONS", "Access-Control-Allow-Headers=Content-Type"), ObjectType.STRING)));

        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\WebServer\\old\\keys"
                ),
                new Server()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                null,
                List.of(
                        list -> {
                            System.out.println("func1");
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(200))
                                            , new Message(MessageType.DATA, new Date(), new Value("hi".getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        },
                        list -> {
                            System.out.println("func2");
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(200))
                                            , new Message(MessageType.DATA, new Date(), new Value("test".getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        },
                        list -> {
                            System.out.println("func3");
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(200))
                                            , new Message(MessageType.DATA, new Date(), new Value("store".getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        }
                )
        );

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
        });
        thread.start();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                SSLContexts.custom()
                                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                        .build()
                                , NoopHostnameVerifier.INSTANCE
                        )
                );
        // .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);

        Thread.sleep(1000);

        System.out.println("Response: " + sendingGetRequest(httpClientBuilder, "http://localhost:8080/hello"));
        System.out.println("Response: " + sendingGetRequest(httpClientBuilder, "https://localhost:8081/test"));
        // System.out.println("Response: " + sendingGetRequest(httpClientBuilder, "https://service.smcsystem.ru:8082/store"));

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool2);

        thread.join();

        process.stop();
    }

    @Test
    public void testFastResp() throws InterruptedException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(8080),
                "requestTimeout", new Value(20000),
                "countThreads", new Value(10),
                "backlog", new Value(0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value("HTTP"),
                "availablePaths", new Value(".*::https://localhost:8081/test::https://service.smcsystem.ru:8082/store"),
                "keyStoreFileName", new Value(""),
                "keyStorePass", new Value(""),
                "keyPass", new Value(""),
                "keyAlias", new Value("")
        ));
        settings.put("bindAddress", new Value(""));
        settings.put("sessionTimeout", new Value(30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("requestType", new Value("OBJECT"));
        settings.put("virtualServerSettings", new Value(new ObjectArray()));
        settings.put("fileResponsePieceSize", new Value(1048576));
        settings.put("headers", new Value(new ObjectArray(List.of("Access-Control-Allow-Origin=*", "Access-Control-Allow-Methods=POST, GET, PUT, DELETE, OPTIONS", "Access-Control-Allow-Headers=Content-Type"), ObjectType.STRING)));

        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\WebServer\\old\\keys"
                ),
                new Server()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                null,
                List.of(
                        (lst) -> {
                            ObjectElement objectElement = (ObjectElement) ((ObjectArray) lst.get(0)).get(0);
                            String uri = objectElement.findField("uri").map(ModuleUtils::toString).orElse("");
                            Long reqId = objectElement.findField("reqId").map(ModuleUtils::getNumber).map(Number::longValue).orElse(-1L);
                            if (uri.equals("/hello")) {
                                sendFastResp(process, reqId,
                                        List.of(new Message(new Value(new ObjectArray(new ObjectElement(new ObjectField("result", "success")))))));
                            } else if (uri.equals("/file")) {
                                sendFastResp(process, reqId,
                                        List.of(new Message(new Value("file.txt"))));
                            } else if (uri.equals("/data")) {
                                sendFastResp(process, reqId,
                                        List.of(new Message(new Value(new ObjectArray(new ObjectElement(
                                                new ObjectField("errorCode", 0),
                                                new ObjectField("data", new ObjectElement(new ObjectField("result", "success1")))
                                        ))))));
                            } else {
                                sendFastResp(process, reqId, List.of(
                                        // new Message(MessageType.DATA, new Date(), new Value("success")),
                                        new Message(MessageType.ERROR, new Date(), new Value("error")),
                                        new Message(MessageType.ERROR, new Date(), new Value(10))
                                ));
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(200))
                                            , new Message(MessageType.DATA, new Date(), new Value("hi".getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        }

                ),
                "default", "start");

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        });
        thread.start();

        try {
            Thread.sleep(1000);
            System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/hello"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
            System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/file"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
            System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/hello2"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
            System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/data"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null, null, null, null, "default", "stop");
        process.execute(executionContextTool2);

        thread.join();

        process.stop();
    }

    private void sendFastResp(Process process, Long reqId, List<IMessage> messages) {
        ExecutionContextToolImpl executionContextToolFastResp = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("errorCode")),
                                                new Message(MessageType.DATA, new Date(), new Value("errorText")),
                                                new Message(MessageType.DATA, new Date(), new Value("data"))
                                        ),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(reqId))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        messages,
                                        ActionType.EXECUTE)
                        )),
                null,
                null,
                List.of(
                        list -> {
                            System.out.println("func1");
                            System.out.println(list);
                            ObjectElement objectElement = (ObjectElement) ((ObjectArray) list.get(0)).get(0);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(String.format("{\"errorCode\":%d,\"errorText\":\"%s\",\"data\":\"%s\"}",
                                                    objectElement.findField("errorCode").map(ModuleUtils::toNumber).map(Number::intValue).orElse(500),
                                                    objectElement.findField("errorText").map(ModuleUtils::toString).orElse(""),
                                                    objectElement.findField("data").map(ModuleUtils::toString).orElse(""))))
                                    ),
                                    ActionType.EXECUTE);
                        },
                        list -> {
                            System.out.println("func2");
                            System.out.println(list);
                            String path = (String) list.get(0);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(String.format("generated file for path=%s", path).getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        }
                ),
                "default", "fast_response"
        );
        process.execute(executionContextToolFastResp);
        executionContextToolFastResp.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolFastResp.getOutput().clear();
    }

    @Test
    public void testBytesWithHeaders() throws InterruptedException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "port", new Value(8080),
                "requestTimeout", new Value(20000),
                "countThreads", new Value(10),
                "backlog", new Value(0),
                // "mode", new Value(ValueType.STRING, "PASSIVE"),
                "protocol", new Value("HTTP"),
                "availablePaths", new Value(".*::https://localhost:8081/test::https://service.smcsystem.ru:8082/store"),
                "keyStoreFileName", new Value(""),
                "keyStorePass", new Value(""),
                "keyPass", new Value(""),
                "keyAlias", new Value("")
        ));
        settings.put("bindAddress", new Value(""));
        settings.put("sessionTimeout", new Value(30));
        settings.put("maxPostSize", new Value(10485760));
        settings.put("allowMultipartParsing", new Value("true"));
        settings.put("requestType", new Value("OBJECT"));
        settings.put("virtualServerSettings", new Value(new ObjectArray()));
        settings.put("fileResponsePieceSize", new Value(1048576));
        settings.put("headers", new Value(new ObjectArray(List.of(), ObjectType.STRING)));

        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\WebServer\\old\\keys"
                ),
                new Server()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                null,
                List.of(
                        (lst) -> {
                            System.out.println("func1");
                            System.out.println(lst);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(200))
                                            , new Message(MessageType.DATA, new Date(), new Value("Content-Type=application/json".getBytes()))
                                            , new Message(MessageType.DATA, new Date(), new Value("Access-Control-Allow-Origin=*".getBytes()))
                                            , new Message(MessageType.DATA, new Date(), new Value("Access-Control-Allow-Methods=POST, GET, PUT, DELETE, OPTIONS".getBytes()))
                                            , new Message(MessageType.DATA, new Date(), new Value("Access-Control-Allow-Headers=Content-Type".getBytes()))
                                            , new Message(MessageType.DATA, new Date(), new Value("Content-Disposition=attachment;filename=data.zip".getBytes()))
                                            , new Message(MessageType.DATA, new Date(), new Value("hi".getBytes()))
                                    ),
                                    ActionType.EXECUTE);
                        }

                ),
                "default", "start");

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        });
        thread.start();

        try {
            Thread.sleep(1000);
            System.out.println("Response: " + sendingGetRequest(HttpClientBuilder.create(), "http://localhost:8080/hello"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null, null, null, null, "default", "stop");
        process.execute(executionContextTool2);

        thread.join();

        process.stop();
    }

}