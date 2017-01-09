package org.apache.rocketmq.console.exception;

/**
 * Created by tangjie
 * 2016/11/21
 * styletang.me@gmail.com
 */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 9213584003139969215L;
    private int code;

    public ServiceException( int code,String message) {
        super(message);
        this.code = code;
    }
    public int getCode() {
        return code;
    }
}
