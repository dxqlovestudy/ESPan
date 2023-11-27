package com.xqcoder.easypan.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileDelFlagEnums {
    DEL(0, "删除"),
    RECYCLE (1, "回收站"),
    USING(2, "使用中");


    private Integer flag;
    private String desc;
}
