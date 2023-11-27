package com.xqcoder.easypan.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter
@Setter
public class FileInfoVO {
    /**
     * 文件ID
     */
    private String fileId;
    /**
     * 父级ID
     */
    private String filePid;
    /**
     * 文件大小
     */
    private Long fileSize;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 封面
     */
    private String fileCover;
    /**
     * 恢复时间，TODO 目前还不清楚作用
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date recoveryTime;
    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;
    /**
     * 0:文件，1：目录
     */
    private Integer folderType;
    /**
     * 1:视频，2：音频，3：图片，4：文档，5：其他
     */
    private Integer fileCategory;
    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    private Integer fileType;
    /**
     * 0:转码中 1转码失败 2:转码成功
     */
    private Integer status;
}
