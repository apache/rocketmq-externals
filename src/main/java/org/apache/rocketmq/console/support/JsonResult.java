package org.apache.rocketmq.console.support;

/**
 * Created by tangjie
 * 2016/11/21
 * styletang.me@gmail.com
 */
public class JsonResult<T>  {
    private int status;
    private T data;
    private String errMsg;

    public JsonResult(T data) {
        this.data = data;
    }

    public JsonResult(int status, String errMsg) {
        this.status = status;
        this.errMsg = errMsg;
    }

    public JsonResult(int status, T data, String errMsg) {
        this.status = status;
        this.data = data;
        this.errMsg = errMsg;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}
