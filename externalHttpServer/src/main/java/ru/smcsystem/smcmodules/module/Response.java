package ru.smcsystem.smcmodules.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class Response {
    private final long id;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final VirtualServerInfo virtualServerInfo;
    private final Map<Integer, RequestInputStream> requestInputStreamMap;
    private volatile ResponseObj responseObj;

    public Response(long id, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, VirtualServerInfo virtualServerInfo, Map<Integer, RequestInputStream> requestInputStreamMap) {
        this.id = id;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.virtualServerInfo = virtualServerInfo;
        this.requestInputStreamMap = requestInputStreamMap;
        this.responseObj = null;
    }

    public long getId() {
        return id;
    }

    public ResponseObj getResponseObj() {
        return responseObj;
    }

    public void setResponseObj(ResponseObj responseObj) {
        this.responseObj = responseObj;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    public VirtualServerInfo getVirtualServerInfo() {
        return virtualServerInfo;
    }

    public Map<Integer, RequestInputStream> getRequestInputStreamMap() {
        return requestInputStreamMap;
    }
}
