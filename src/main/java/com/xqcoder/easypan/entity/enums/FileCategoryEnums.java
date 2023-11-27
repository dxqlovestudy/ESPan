package com.xqcoder.easypan.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileCategoryEnums {
    VIDEO(1, "video", "视频"),
    MUSIC(2, "music", "音频"),
    IMAGE(3, "image", "图片"),
    DOC(4, "doc", "文档"),
    OTHERS(5, "others", "其他");

    private Integer category;
    private String code;
    private String desc;


    public static FileCategoryEnums getByCode (String code) {
        for (FileCategoryEnums item :
                FileCategoryEnums.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
