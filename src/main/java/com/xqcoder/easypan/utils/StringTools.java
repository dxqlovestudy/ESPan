package com.xqcoder.easypan.utils;


import com.xqcoder.easypan.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {

    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    public static boolean isEmpty(String str) {

        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }


    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        fileName = fileName.substring(0, index);
        return fileName;
    }

    /*
     * @description: 重命名文件
     * @param fileName 文件名
     * @return java.lang.String 不带后缀的文件名 + 随机字符串 + 后缀
     * @author: HuaXian
     * @date: 2023/12/22 11:02
     */
    public static String rename(String fileName) {
        // 获取不带后缀的文件名
        String fileNameReal = getFileNameNoSuffix(fileName);
        // 获取文件后缀
        String suffix = getFileSuffix(fileName);
        // 不带后缀的文件名 + 随机字符串 + 后缀
        return fileNameReal + "_" + getRandomString(Constants.LENGTH_5) + suffix;
    }

    public static final String getRandomString(Integer count) {
        /**
         * @description: 生成包含字符和数字的随机字符串
         * @param count
         * @return java.lang.String
         * @author: HuaXian
         * @date: 2023/8/6 16:30
         */
        return RandomStringUtils.random(count, true, true);

    }

    public static final String getRandomNumber(Integer count) {
        /**
         * @description: 生成随机数字
         * @param count
         * @return java.lang.String
         * @author: HuaXian
         * @date: 2023/8/6 16:23
         */
        return RandomStringUtils.random(count, false, true);
    }


    public static String escapeTitle(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        return content;
    }


    public static String escapeHtml(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace(" ", "&nbsp;");
        content = content.replace("\n", "<br>");
        return content;
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }
}
