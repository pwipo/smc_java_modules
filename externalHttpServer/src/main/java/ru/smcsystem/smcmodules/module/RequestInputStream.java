package ru.smcsystem.smcmodules.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class RequestInputStream implements Closeable {
    private Part part;
    HttpServletRequest req;
    private InputStream inputStream;
    private Long size;

    public RequestInputStream(Part part) {
        this.part = part;
        req = null;
        inputStream = null;
        size = part.getSize();
    }

    public RequestInputStream(HttpServletRequest req) {
        this.req = req;
        part = null;
        inputStream = null;
        size = (long) req.getContentLength();
    }

    public synchronized InputStream getInputStream() throws IOException {
        if (inputStream == null)
            inputStream = part != null ? part.getInputStream() : req.getInputStream();
        return inputStream;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignore) {
            }
            inputStream = null;
        }
    }

    public Collection<String> getHeaderNames() {
        return part != null ? part.getHeaderNames() : Collections.list(req.getHeaderNames());
    }

    public String getName() {
        return part != null ? part.getName() : "file";
    }

    public String getContentType() {
        return part != null ? part.getContentType() : req.getContentType();
    }

}
