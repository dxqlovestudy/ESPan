package com.xqcoder.easypan.controller;

import com.xqcoder.easypan.service.FileInfoService;

import javax.annotation.Resource;

public class CommonFileController extends ABaseController{
    @Resource
    protected FileInfoService fileInfoService;
}
