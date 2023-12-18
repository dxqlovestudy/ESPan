package com.xqcoder.easypan.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileFolderTypeEnums {
    FILE(0, "文件"),
    FOLDER(1, "目录");
    private Integer type;
    private String desc;
}
