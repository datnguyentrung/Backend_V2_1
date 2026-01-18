package com.dat.backend_v2_1.dto;

import lombok.Data;

@Data
public class RestResponse<T> {
    private int statusCode;
    private String error;

    private Object message;
    private T data;
}
