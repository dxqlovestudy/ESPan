package com.xqcoder.easypan.entity.query;

import com.xqcoder.easypan.entity.enums.PageSize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplePage {
	private int pageNo;
	private int countTotal;
	private int pageSize;
	private int pageTotal;
	private int start;
	private int end;

	public SimplePage(Integer pageNo, int countTotal, int pageSize) {
		if (null == pageNo) {
			pageNo = 0;
		}
		this.pageNo = pageNo;
		this.countTotal = countTotal;
		this.pageSize = pageSize;
		action();
	}

	public SimplePage(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void action() {
		if (this.pageSize <= 0) {
			this.pageSize = PageSize.SIZE20.getSize();
		}
		if (this.countTotal > 0) {
			this.pageTotal = this.countTotal % this.pageSize == 0 ? this.countTotal / this.pageSize
					: this.countTotal / this.pageSize + 1;
		} else {
			pageTotal = 1;
		}

		if (pageNo <= 1) {
			pageNo = 1;
		}
		if (pageNo > pageTotal) {
			pageNo = pageTotal;
		}
		this.start = (pageNo - 1) * pageSize;
		this.end = this.pageSize;
	}
}
