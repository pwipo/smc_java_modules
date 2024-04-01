package ru.smcsystem.smcmodules.module;

import javax.servlet.http.HttpServletResponse;

public class Response {
    private final long id;
    private final HttpServletResponse httpServletResponse;
    private final VirtualServerInfo virtualServerInfo;
    private volatile ResponseObj responseObj;

    public Response(long id, HttpServletResponse httpServletResponse, VirtualServerInfo virtualServerInfo) {
        this.id = id;
        this.httpServletResponse = httpServletResponse;
        this.virtualServerInfo = virtualServerInfo;
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

    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    public VirtualServerInfo getVirtualServerInfo() {
        return virtualServerInfo;
    }
}
