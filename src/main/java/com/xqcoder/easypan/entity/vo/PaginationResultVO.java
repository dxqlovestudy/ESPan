package com.xqcoder.easypan.entity.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationResultVO<T> {
	private Integer totalCount;
	private Integer pageSize;
	private Integer pageNo;
	private Integer pageTotal;
	private List<T> list = new ArrayList<T>();

	public PaginationResultVO(Integer totalCount, Integer pageSize, Integer pageNo, List<T> list) {
		this.totalCount = totalCount;
		this.pageSize = pageSize;
		this.pageNo = pageNo;
		this.list = list;
	}

	public PaginationResultVO(List<T> list) {
		this.list = list;
	}
}
