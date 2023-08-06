package com.xqcoder.easypan.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionWebUserDto {
    private String nickName;
    private String userId;
    private Boolean isAdmin;
    private String avatar;
}
