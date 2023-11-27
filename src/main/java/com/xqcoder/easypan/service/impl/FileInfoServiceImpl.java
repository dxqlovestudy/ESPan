package com.xqcoder.easypan.service.impl;

import com.xqcoder.easypan.entity.enums.PageSize;
import com.xqcoder.easypan.entity.query.FileInfoQuery;
import com.xqcoder.easypan.entity.query.SimplePage;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import com.xqcoder.easypan.mappers.FileInfoMapper;
import com.xqcoder.easypan.service.FileInfoService;
import org.apache.tomcat.jni.FileInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {
    @Resource
    private FileInfoMapper fileInfoMapper;
    @Override
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(param);
        PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    @Override
    public int findCountByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectCount(param);
    }

    @Override
    public List<FileInfo> findListByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectList(param);
    }
}
