//package com.dat.backend_v2_1.util;
//
//import com.dat.backend_v2_1.dto.RestResponse;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.core.MethodParameter;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.ServerHttpRequest;
//import org.springframework.http.server.ServerHttpResponse;
//import org.springframework.http.server.ServletServerHttpResponse;
//import org.springframework.lang.NonNull;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
//
//@ControllerAdvice
//public class FormatRestResponse implements ResponseBodyAdvice<Object> {
//
//    @Override
//    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class converterType) {
//        return true;
//    }
//
//    @Override
//    public Object beforeBodyWrite(
//            Object body,
//            @NonNull MethodParameter returnType,
//            @NonNull MediaType selectedContentType,
//            @NonNull Class selectedConverterType,
//            @NonNull ServerHttpRequest request,
//            @NonNull ServerHttpResponse response) {
//
//        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
//        int status = servletResponse.getStatus();
//
//        RestResponse<Object> res = new RestResponse<>();
//        res.setStatusCode(status);
//
//        if (body instanceof String) {
//            return body;
//        }
//
//        if (status >= 400) {
//            return body;
//        }
//
//        res.setData(body);
//        res.setMessage("CALL API SUCCESS");
//        return res;
//    }
//}
