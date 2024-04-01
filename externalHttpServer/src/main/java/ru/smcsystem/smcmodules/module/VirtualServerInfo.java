package ru.smcsystem.smcmodules.module;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardHost;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VirtualServerInfo {
    private final String urlHeader;
    private final URL url;
    private final File keyStore;
    private final String keyStorePass;
    private final String keyAlias;
    private final String keyPass;
    private final InetAddress address;
    private final Map<Integer, Pattern> patterns;
    private final Host host;
    private final Integer requestTimeout;
    private final Integer countThreads;
    private final Integer backlog;
    private final Integer sessionTimeout;
    private final Integer maxPostSize;
    private final Boolean allowMultipartParsing;
    private final List<String> headers;
    private Server.RequestType requestType;

    public VirtualServerInfo(String urlHeader, URL url, File keyStore, String keyStorePass, String keyAlias, String keyPass,
                             InetAddress address, Map<Integer, Pattern> patterns, Integer requestTimeout, Integer countThreads,
                             Integer backlog, Integer sessionTimeout, Integer maxPostSize, Boolean allowMultipartParsing,
                             List<String> headers, Server.RequestType requestType) {
        this.urlHeader = urlHeader;
        this.url = url;
        this.keyStore = keyStore;
        this.keyStorePass = keyStorePass;
        this.keyAlias = keyAlias;
        this.keyPass = keyPass;
        this.address = address;
        this.patterns = patterns;
        this.requestTimeout = requestTimeout;
        this.countThreads = countThreads;
        this.backlog = backlog;
        this.sessionTimeout = sessionTimeout;
        this.maxPostSize = maxPostSize;
        this.allowMultipartParsing = allowMultipartParsing;
        this.host = new StandardHost();
        host.setName(url.getHost());
        this.headers = headers;
        this.requestType = requestType;
    }

    public String getUrlHeader() {
        return urlHeader;
    }

    public URL getUrl() {
        return url;
    }

    public File getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePass() {
        return keyStorePass;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Map<Integer, Pattern> getPatterns() {
        return patterns;
    }

    public Host getHost() {
        return host;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public Integer getCountThreads() {
        return countThreads;
    }

    public Integer getBacklog() {
        return backlog;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public Integer getMaxPostSize() {
        return maxPostSize;
    }

    public Boolean getAllowMultipartParsing() {
        return allowMultipartParsing;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public Server.RequestType getRequestType() {
        return requestType;
    }
}
