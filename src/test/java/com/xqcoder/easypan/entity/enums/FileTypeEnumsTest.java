package com.xqcoder.easypan.entity.enums;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FileTypeEnumsTest {
    @Test
    public void testGetFileTypeBySuffix() {
        System.out.println(getFileTypeBySuffix(".txt"));
        assertEquals(getFileTypeBySuffix(".txt"), FileTypeEnums.TXT);
    }
    public static FileTypeEnums getFileTypeBySuffix(String suffix) {
        for (FileTypeEnums item : FileTypeEnums.values()) {
            System.out.println(item);
            if (ArrayUtils.contains(item.getSuffixs(), suffix)) {
                return item;
            }
        }
        return FileTypeEnums.OTHERS;
    }
}
