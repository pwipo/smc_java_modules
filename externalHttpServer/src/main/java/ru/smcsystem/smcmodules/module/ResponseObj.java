package ru.smcsystem.smcmodules.module;

import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.ArrayList;
import java.util.List;

public class ResponseObj {
    private final Long reqId;
    private final int resultCode;
    private final List<String> headers;
    private final byte[] content;
    private final String path;
    private final ExecutionContextTool executionContextTool;
    private final int startEcId;
    private volatile boolean work;

    public ResponseObj(Long reqId, int resultCode, List<String> headers, byte[] content, String path, ExecutionContextTool executionContextTool, int startEcId) {
        this.reqId = reqId;
        this.resultCode = resultCode;
        this.headers = headers != null ? headers : new ArrayList<>();
        this.content = content;
        this.path = path;
        this.executionContextTool = executionContextTool;
        this.startEcId = startEcId;
        work = true;
    }

    public Long getReqId() {
        return reqId;
    }

    public int getResultCode() {
        return resultCode;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public byte[] getContent() {
        return content;
    }

    public String getPath() {
        return path;
    }

    public byte[] getBytes(long startPosition, int size) {
        return executionContextTool != null && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > startEcId ?
                ModuleUtils.executeParallelAndGetMessages(executionContextTool, startEcId, List.of(path, startPosition, size))
                        .map(lst -> ModuleUtils.getBytes(lst.get(0)))
                        .orElse(null) :
                null;
    }

    public Long getSize() {
        return executionContextTool != null && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > startEcId + 1 ?
                ModuleUtils.executeParallelAndGetMessages(executionContextTool, startEcId + 1, List.of(path))
                        .map(lst -> ModuleUtils.getNumber(lst.get(0))).map(Number::longValue).orElse(null) :
                null;
    }

    public void waitWork() {
        if (!work)
            return;
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException ignore) {
            }
        }
    }

    public void setWork(boolean work) {
        this.work = work;
        if (!work) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public boolean isFastResponse() {
        return executionContextTool != null;
    }

}
