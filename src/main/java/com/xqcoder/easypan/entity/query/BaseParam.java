package com.xqcoder.easypan.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseParam {
	private SimplePage simplePage;
	private Integer pageNo;
	private Integer pageSize;
	private String orderBy;
}
