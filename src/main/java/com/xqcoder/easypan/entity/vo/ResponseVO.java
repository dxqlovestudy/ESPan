package com.xqcoder.easypan.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseVO<T> {
    private String status;
    private Integer code;
    private String info;
    private T data;
}
