package com.xqcoder.easypan.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionShareDto {
    private String shareId;
    private String shareUserId;
    private Date expireTime;
    private String fileId;

}
