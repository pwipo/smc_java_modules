package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Http implements Module {

    private CloseableHttpClient client;

    private enum Method {
        GET,
        POST
    }

    private Charset charset;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String cookiesStr = (String) configurationTool.getSetting("cookies").orElseThrow(() -> new ModuleException("cookies setting")).getValue();
        Boolean userAuth = Boolean.valueOf((String) configurationTool.getSetting("userAuth").orElseThrow(() -> new ModuleException("userAuth setting")).getValue());
        String username = (String) configurationTool.getSetting("username").orElseThrow(() -> new ModuleException("username setting")).getValue();
        String password = (String) configurationTool.getSetting("password").orElseThrow(() -> new ModuleException("password setting")).getValue();
        String strCharset = (String) configurationTool.getSetting("charset").orElseThrow(() -> new ModuleException("charset setting")).getValue();

        charset = !strCharset.isBlank() ? Charset.forName(strCharset) : null;

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try {
            httpClientBuilder
                    .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                    SSLContexts.custom()
                                            .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                            .build()
                                    , NoopHostnameVerifier.INSTANCE
                            )
                    );
        } catch (Exception e) {
            throw new ModuleException("error", e);
        }

        if (StringUtils.isNoneBlank(cookiesStr)) {
            CookieStore cookieStore = new BasicCookieStore();
            Arrays.stream(cookiesStr.split("::"))
                    .map(s -> s.split("="))
                    .filter(arr -> arr.length > 1)
                    .map(arr -> new BasicClientCookie(arr[0], arr[1]))
                    .peek(c -> c.setPath("/"))
                    .forEach(cookieStore::addCookie);
            // BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", getSessionId());
            //cookie.setDomain("your domain");
            // cookie.setPath("/");
            // cookieStore.addCookie(cookie);
            // client.setCookieStore(cookieStore);
            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }

        if (userAuth) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY, //new AuthScope("httpbin.org", 80)
                    new UsernamePasswordCredentials(username, password));
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

        httpClientBuilder.setDefaultHeaders(List.of(new BasicHeader(HttpHeaders.USER_AGENT, "Apache http client")));
        client = httpClientBuilder.build();

    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        try {
            switch (executionContextTool.getType()) {
                case "default":
                    if (executionContextTool.countSource() == 0)
                        break;
                    Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .map(IAction::getMessages)
                            .filter(messages -> messages.size() >= 2)
                            .map(LinkedList::new)
                            .forEach(messages -> {
                                try {
                                    Method method = Method.values()[(getNumber(messages.poll()).intValue())];
                                    String address = getString(messages.poll());

                                    HttpRequestBase request = null;
                                    switch (method) {
                                        case GET:
                                            request = new HttpGet(address);
                                            break;
                                        case POST:
                                            request = new HttpPost(address);
                                            break;
                                    }

                                    if (!messages.isEmpty()) {
                                        int countHeaders = getNumber(messages.poll()).intValue();
                                        for (int i = 0; i < countHeaders; i++) {
                                            String[] split = ModuleUtils.getString(messages.poll()).split("=");
                                            if (split.length > 1)
                                                request.addHeader(split[0].trim(), split[1].trim());
                                        }
                                    }

                                    if (Method.POST.equals(method) && !messages.isEmpty())
                                        ((HttpPost) request).setEntity(getPost(messages, true));

                                    // System.out.println(request.getRequestLine());
                                    proceessRequest(executionContextTool, request);
                                } catch (Exception e) {
                                    executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                                    configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                                }
                            });
                    break;
                case "get": {
                    Optional<IAction> lastActionWithData = ModuleUtils.getLastActionWithData(executionContextTool.getMessages(0));
                    if (lastActionWithData.isEmpty())
                        break;
                    LinkedList<IMessage> messages = new LinkedList<>(lastActionWithData.get().getMessages());
                    String address = ModuleUtils.getString(messages.poll());
                    HttpRequestBase request = new HttpGet(address);
                    addHeaders(request, messages);
                    proceessRequest(executionContextTool, request);
                    break;
                }
                case "post": {
                    Optional<IAction> lastActionWithData = ModuleUtils.getLastActionWithData(executionContextTool.getMessages(0));
                    if (lastActionWithData.isEmpty())
                        break;
                    LinkedList<IMessage> messages = new LinkedList<>(lastActionWithData.get().getMessages());
                    String address = ModuleUtils.getString(messages.poll());
                    HttpRequestBase request = new HttpPost(address);
                    addHeaders(request, messages);
                    Optional<IAction> lastActionWithDataPost = ModuleUtils.getLastActionWithData(executionContextTool.getMessages(1));
                    if (lastActionWithDataPost.isPresent())
                        ((HttpPost) request).setEntity(getPost(new LinkedList<>(lastActionWithDataPost.get().getMessages()), false));
                    proceessRequest(executionContextTool, request);
                    break;
                }
            }
        } catch (Exception e) {
            executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
            configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
        }
    }

    private HttpEntity getPost(LinkedList<IMessage> messages, boolean useCounter) throws UnsupportedEncodingException {
        HttpEntity reqEntity = null;
        if (messages.isEmpty())
            return reqEntity;
        Map<String, Object> params = new HashMap<>();
        boolean useMultipart = false;
        if (messages.size() == 1) {
            IMessage value = messages.poll();
            if (ValueType.BYTES.equals(value.getType())) {
                reqEntity = new ByteArrayEntity((byte[]) value.getValue(), ContentType.DEFAULT_BINARY);
            } else {
                reqEntity = new StringEntity(value.getValue().toString());
            }
        } else {
            if (useCounter) {
                int countParams = getNumber(messages.poll()).intValue();
                for (int i = 0; i + 1 < countParams; i = i + 2) {
                    String name = ModuleUtils.toString(messages.poll());
                    IMessage value = messages.poll();
                    if (!useMultipart)
                        useMultipart = value != null && ValueType.BYTES.equals(value.getType());
                    if (value != null)
                        params.put(name, value.getValue());
                }
            } else {
                while (!messages.isEmpty()) {
                    String name = ModuleUtils.toString(messages.poll());
                    IMessage value = messages.poll();
                    if (!useMultipart)
                        useMultipart = value != null && ValueType.BYTES.equals(value.getType());
                    if (name != null && value != null)
                        params.put(name, value.getValue());
                }
            }
            if (useMultipart) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                params.forEach((k, v) -> {
                    if (v instanceof byte[]) {
                        builder.addPart(k, new ByteArrayBody((byte[]) v, k));
                    } else {
                        try {
                            builder.addPart(k, new StringBody(v.toString()));
                        } catch (UnsupportedEncodingException e) {
                            throw new ModuleException("wrong params", e);
                        }
                    }
                });
                reqEntity = builder.build();
            } else {
                List<NameValuePair> nvps = params.entrySet().stream()
                        .map(e -> new BasicNameValuePair(e.getKey(), e.getValue().toString()))
                        .collect(Collectors.toList());
                try {
                    reqEntity = new UrlEncodedFormEntity(nvps);
                } catch (UnsupportedEncodingException e) {
                    throw new ModuleException("wrong params", e);
                }
            }
        }
        return reqEntity;
    }

    private void addHeaders(HttpRequestBase request, LinkedList<IMessage> messages) {
        while (!messages.isEmpty()) {
            String header = ModuleUtils.getString(messages.poll());
            if (header != null) {
                String[] split = header.split("=");
                if (split.length > 1)
                    request.addHeader(split[0].trim(), split[1].trim());
            }
        }
    }

    private void proceessRequest(ExecutionContextTool executionContextTool, HttpRequestBase request) {
        try (CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            executionContextTool.addMessage(status);
            Object result = null;
            // if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                executionContextTool.addMessage(response.getAllHeaders().length);
                Arrays.stream(response.getAllHeaders()).forEach(h -> executionContextTool.addMessage(h.getName() + "=" + h.getValue()));

                if (ContentType.getOrDefault(entity).getMimeType().startsWith("text")) {
                    result = charset != null ? EntityUtils.toString(entity, charset) : EntityUtils.toString(entity);
                } else {
                    result = EntityUtils.toByteArray(entity);
                }
                EntityUtils.consume(entity);
            }
            // }
            if (result != null)
                executionContextTool.addMessage(List.of(result));
        } catch (Exception e) {
            throw new ModuleException("execute exception", e);
        } finally {
            request.releaseConnection();
        }
    }

    private Number getNumber(IMessage message) {
        if (ValueType.BYTE.equals(message.getType())
                || ValueType.FLOAT.equals(message.getType())
                || ValueType.INTEGER.equals(message.getType())
                || ValueType.LONG.equals(message.getType())
                || ValueType.FLOAT.equals(message.getType())
                || ValueType.DOUBLE.equals(message.getType())
                || ValueType.BIG_INTEGER.equals(message.getType())
                || ValueType.BIG_DECIMAL.equals(message.getType())
        )
            return (Number) message.getValue();
        throw new ModuleException("wrong type, need number");
    }

    private String getString(IMessage message) {
        if (ValueType.STRING.equals(message.getType()))
            return (String) message.getValue();
        throw new ModuleException("wrong type, need string");
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        charset = null;
    }

}
