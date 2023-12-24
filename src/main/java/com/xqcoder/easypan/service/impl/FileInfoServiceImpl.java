package com.xqcoder.easypan.service.impl;

import com.xqcoder.easypan.component.RedisComponent;
import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.config.AppConfig;
import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.dto.UploadResultDto;
import com.xqcoder.easypan.entity.dto.UserSpaceDto;
import com.xqcoder.easypan.entity.enums.*;
import com.xqcoder.easypan.entity.po.FileInfo;
import com.xqcoder.easypan.entity.po.UserInfo;
import com.xqcoder.easypan.entity.query.FileInfoQuery;
import com.xqcoder.easypan.entity.query.SimplePage;
import com.xqcoder.easypan.entity.query.UserInfoQuery;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import com.xqcoder.easypan.exception.BusinessException;
import com.xqcoder.easypan.mappers.FileInfoMapper;
import com.xqcoder.easypan.mappers.UserInfoMapper;
import com.xqcoder.easypan.service.FileInfoService;
import com.xqcoder.easypan.service.UserInfoService;
import com.xqcoder.easypan.utils.DateUtil;
import com.xqcoder.easypan.utils.ProcessUtils;
import com.xqcoder.easypan.utils.ScaleFilter;
import com.xqcoder.easypan.utils.StringTools;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.ProcessIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;

@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {
    // 日志
    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);
    @Resource
    // 注入fileInfoMapper,用于操作数据库
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
    @Resource
    // 注入redisComponent,用于操作redis
    private RedisComponent redisComponent;
    @Resource
    // 注入userInfoMapper,用于操作数据库
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Resource
    // 注入appConfig,用于获取项目路径,通过@value注解获取application配置文件中的值
    private AppConfig appConfig;
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

    /**
     * @description: 上传文件功能，1.分片上传，前端调用的时候就已经将文件分片了，后端只需要将分片文件合并就可以了。
     * @param webUserDto 传入Session中的信息
     * @param fileId 文件id，如果是断点续传传入的FileId是一样的，如果不是断点续传，传入的FileId为空，后续方法里面会随机生成一个FileId
     * @param file 上传的文件，MultipartFile是spring的文件上传类
     * @param fileName 文件名
     * @param filePid 文件父id
     * @param fileMd5 文件md5，md5是文件的唯一标识,主要作用是用来判断文件是检验文件是否完整，防止文件上传出错。
     * @param chunkIndex 当前分片索引，用于分片上传，每片都需要调用该方法，上传当前片
     * @param chunks 分片总数 如果是断点续传，需要传入分片总数，否则不需要。
     * @return com.xqcoder.easypan.entity.dto.UploadResultDto 返回上传结果，包括文件id(fileId)和上传状态(status)，上传状态有：上传中，上传完成，秒传
     * @author: HuaXian
     * @date: 2023/12/20 14:39
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // @Transactional注解表示该方法需要事务管理，如果该方法执行失败，会回滚到执行该方法之前的状态
    public UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            // 创建一个UploadResultDto对象，用于返回上传结果
            UploadResultDto resultDto = new UploadResultDto();
            // 如果传入的fileId为空，随机生成一个fileId
            if (StringTools.isEmpty(fileId)) {
                // 生成一个10位的随机字符串,包含字符和数字的随机字符串
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            // 然后将fileId存入resultDto中
            resultDto.setFileId(fileId);
            // 创建一个Date对象，用于获取当前时间
            Date curDate = new Date();
            // 获取用户空间使用情况，从redis中获取，存储在UserSpaceDto对象中，传入的参数UserId从Session中获取
            UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
            // 如果是断点续传，判断文件是否存在，断点续传就不需要创建文件夹了，因为已经创建过了
            // TODO 断点续传这里没了解清楚是继续从上次断点续传还是重新传，
            //  1. 如果是断点续传的话，没找到续传的代码，因为没使用传入file的这个方法
            //  如果是重新上传，为什么这里的if里面直接返回了resultDto，
            //  2. 可能是分片上传，不确定是断点续传，断点续传是copilot自动生成的
            if (chunkIndex == 0) {
                // 创建一个FileInfoQuery对象，里面存储文件信息的查询条件
                FileInfoQuery infoQuery = new FileInfoQuery();
                // 将fileMD5存入infoQuery中
                infoQuery.setFileMd5(fileMd5);
                // 做一个分页查询，查询第一条数据，因为mapper里面的selectList方法是查询所有符合条件的数据，所以这里做一个分页查询，查询第一条数据
                infoQuery.setSimplePage(new SimplePage(0, 1));
                // 设置文件状态为正在使用，FileStatusEnums.USING.getStatus()返回的是2
                infoQuery.setStatus(FileStatusEnums.USING.getStatus());
                // 根据infoQuery查询数据库，返回一个FileInfo对象的集合
                List<FileInfo> dbFileList = this.fileInfoMapper.selectList(infoQuery);
                // 秒传
                // 如果集合不为空，说明以前上传过该文件，直接返回，实现秒传功能
                if (!dbFileList.isEmpty()) {
                    // 获取第一个文件信息
                    FileInfo dbFile = dbFileList.get(0);
                    // 判断文件状态,如果文件大小+用户使用空间大小大于用户总空间大小，抛出网盘空间不足异常
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    // 将文件Id（fileId）存入dbFile中
                    dbFile.setFileId(fileId);
                    // 将文件父Id（filePid）存入dbFile中
                    dbFile.setFilePid(filePid);
                    // 从session中获取用户Id，将用户Id存入dbFileo中
                    dbFile.setUserId(webUserDto.getUserId());
                    // 将fileMd5设置为null存入dbFile中
                    dbFile.setFileMd5(null);
                    // 将当前时间（creDate）存入dbFile中
                    dbFile.setCreateTime(curDate);
                    // 将最后更新时间（lastUpdateTime）存入dbFile中
                    dbFile.setLastUpdateTime(curDate);
                    // 将文件状态（status）设置为正在使用，FileStatusEnums.USING.getStatus()返回的是2
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    // 将文件删除标记（delFlag）设置为正在使用，FileDelFlagEnums.USING.getFlag()返回的是2
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    // 将文件类型（folderType）设置为文件，FileFolderTypeEnums.FILE.getType()返回的是1
                    dbFile.setFileMd5(fileMd5);
                    // 根据传入参数查询数据库，如果数据库中有重名文件，自动重命名，重命名规则：文件名 + 随机字符串 + 后缀
                    fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
                    // 将文件名（fileName）存入dbFile中
                    dbFile.setFileName(fileName);
                    // 将dbFile插入数据库
                    this.fileInfoMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    // 更新用户空间使用的大小
                    updateUserSpace(webUserDto, dbFile.getFileSize());
                    return resultDto;
                }
            }
            // 定义临时目录路径，临时目录的路径为：配置文件中的projectFolder + Constants.FILE_FOLDER_TEMP（/temp/）
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            // 当前用户目录名为：用户Id + 文件Id
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            // 创建临时目录，临时目录的路径为：配置文件中的projectFolder + Constants.FILE_FOLDER_TEMP（/temp/） + currentUserFolderName(用户Id + 文件Id)
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            // 如果临时目录不存在，创建临时目录
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdir();
            }
            // 判断磁盘空间，从redis中获取用户临时文件大小，保存在currentTempSize中
            Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            // 判断临时磁盘空间，如果文件大小 + 用户临时文件大小 + 用户使用空间大小 > 用户总空间大小，抛出网盘空间不足异常
            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }
            // 创建一个临时文件，临时文件的路径为：临时目录路径（tempFileFolder，包含用户id和文件id的临时） / 当前分片索引
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            // 将上传的文件写入临时文件(newFile)中
            file.transferTo(newFile);
            // 将用户临时文件大小存入redis中，key为：easypan:user:file:temp:userId+fileId，value为：currentTempSize + file.getSize()
            redisComponent.saveFileTempSize(webUserDto.getUserId(), fileId, file.getSize());
            // 不是最后一个分片，直接返回
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }
            // 如果能执行到这里证明是最后一个分片。最后一个分片上传完成，记录数据库，异步合成分片
            // 将date转换为字符串，格式为：yyyyMM，传入month中
            String month = DateUtil.format(curDate, DateTimePatternEnum.YYYYMM.getPattern());
            // 使用StringTools工具类获取文件后缀，传入fileName，返回文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileName);
            // 真实文件名为：用户Id + 文件Id + 文件后缀
            String realFileName = currentUserFolderName + fileSuffix;
            // 文件类型，根据文件后缀获取文件类型，fileTypeEnum，返回文件类型
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            // 自动重命名
            fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(webUserDto.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            updateUserSpace(webUserDto, totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            // 事务提交后调用异步方法
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    transferFile(fileInfo.getFileId(), webUserDto);
                }
            });
            return resultDto;
        } catch (BusinessException e) {
            uploadSuccess = false;
            logger.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            uploadSuccess = false;
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } finally {
            // 如果上传失败，清除临时目录
            if (tempFileFolder != null && !uploadSuccess) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时目录失败");
                }
            }
        }
    }

    @Override
    // @Async注解表明是异步执行，创建一个新的线程去执行下面方法
    @Async
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        // 文件封面
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());
        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            // 临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if (!fileFolder.exists()) {
                /**
                 * mkdirs()和mkdir()的主要区别：主要区别是该目录的父目录是否存在。
                 * mkdir() 如果该目录的父目录不存在，该方法将失败，不会递归创建缺失的父目录
                 * mkdirs() 如果该目录的父目录不存在，会递归创建缺失的父目录
                 */
                fileFolder.mkdirs();
            }
            // 文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            // 目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (! targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            // 真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            // 真实文件路径
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            // 合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            // 视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                // 视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件Id:{},userId:{}", fileId, webUserDto.getUserId(), e);
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webUserDto.getUserId(), updateInfo, FileStatusEnums.TRANSFER.getStatus());
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        // 创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        // 生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        // 生成索引文件.m3u8和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        // 删除index.ts
        new File(tsPath).delete();
    }

    private void union(String dirPath, String toFilePath, String fileName, boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                // 创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            try {
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                logger.error("关闭流失败", e);
            }
            if (delSource) {
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @description: 更新用户空间大小
     * @param webUserDto 传入Session中的信息
     * @param totalSize 传入文件大小
     * @return void
     * @author: HuaXian
     * @date: 2023/12/18 16:41
     */
    private void updateUserSpace(SessionWebUserDto webUserDto, Long totalSize) {
        // 在userInfo表中更新.传入的参数为：用户ID，用户使用空间大小，用户总空间大小.(如果参数为null则不更新改参数，逻辑写在了mapper中了。)
        Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(), totalSize, null);
        // 如果更新失败，抛出异常,count是更新的行数，如果更新失败，count为0
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        // 在redis中更新用户使用空间大小
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + totalSize);
        // 将更新后的用户使用空间大小存入redis中
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(), spaceDto);
    }

    /**
     * @description: 自动改名
     * @param filePid 文件父ID
     * @param userId 用户ID
     * @param fileName 文件名
     * @return java.lang.String 返回改名后的文件名，如果没有重名，返回原文件名，如果有重名，返回重命名后的文件名，重命名规则：
     * @author: HuaXian
     * @date: 2023/12/18 16:47
     */
    private String autoRename(String filePid, String userId, String fileName) {
        // 创建一个FileInfoQuery对象
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        // 设置查询条件，查询条件为：用户ID（userId），文件父ID（filePid），文件删除标记（2，使用中），文件名（fileName
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        // 从数据库中查询符合条件的文件数量
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        // 如果数量大于0，说明有重名文件，调用StringTools的rename方法，将文件名重命名
        if (count > 0) {
            // 返回 文件名 + 随机字符串 + 后缀
            return StringTools.rename(fileName);
        }
        return fileName;
    }
}
