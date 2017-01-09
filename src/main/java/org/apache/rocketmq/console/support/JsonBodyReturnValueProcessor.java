package org.apache.rocketmq.console.support;

import org.apache.rocketmq.console.support.annotation.JsonBody;
import org.apache.rocketmq.console.util.JsonUtil;
import org.springframework.core.MethodParameter;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by tangjie
 * 2016/11/21
 * styletang.me@gmail.com
 */
public class JsonBodyReturnValueProcessor implements HandlerMethodReturnValueHandler {

    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.getMethodAnnotation(JsonBody.class) != null;
    }

    @SuppressWarnings("unchecked")
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
                                  ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
            throws IOException, HttpMediaTypeNotAcceptableException {
        mavContainer.setRequestHandled(true);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        JsonResult jsonResult = new JsonResult(returnValue);
        response.setContentType("application/json; charset=UTF-8");
        JsonUtil.writeValue(response.getWriter(), jsonResult);
    }

}
