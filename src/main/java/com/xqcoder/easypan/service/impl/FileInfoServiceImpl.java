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
            // 使用StringTools工具类获取文件后缀，传入fileName，返回文件后缀,带“.”
            String fileSuffix = StringTools.getFileSuffix(fileName);
            // 真实文件名为：用户Id + 文件Id + 文件后缀
            String realFileName = currentUserFolderName + fileSuffix;
            // 文件类型，根据文件后缀获取文件类型，fileTypeEnum，返回文件类型
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            // 自动重命名 fileName = 文件名 + 随机字符串 + 后缀
            fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
            // 创建一个FileInfo对象，用于存储文件信息
            FileInfo fileInfo = new FileInfo();
            // 将文件信息存入fileInfo中
            fileInfo.setFileId(fileId);
            // 将用户Id存入fileInfo中
            fileInfo.setUserId(webUserDto.getUserId());
            // 将文件MD5存入fileInfo中，MD5作用：1.防止重复上传，2.断点续传 3.文件完整性校验，防止文件损坏，4.文件秒传
            fileInfo.setFileMd5(fileMd5);
            // 将文件名存入fileInfo中
            fileInfo.setFileName(fileName);
            // 将文件路径存入fileInfo中，文件路径为：月份 + / + 真实文件名，只是路径，不是文件
            fileInfo.setFilePath(month + "/" + realFileName);
            // 将文件父ID存入fileInfo中
            fileInfo.setFilePid(filePid);
            // 将创建时间存入fileInfo中
            fileInfo.setCreateTime(curDate);
            // 将最后更新时间存入fileInfo中
            fileInfo.setLastUpdateTime(curDate);
            // 将文件类型存入fileInfo中，fileTypeEnum.getCategory()返回的是文件类型枚举中的文件类型(FileCategoryEnums)，FileCategoryEnums.getCategory()返回的1(视频)，2(音频)，3(图片)，4(文档)，5(其他)
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            // 将文件类型存入fileInfo中，fileTypeEnum.getType()返回的是具体文件格式，1(视频)，2(音频)，3(图片)，4(pdf)，5(word)，6(excel)，7(txt)，8(代码)，9(压缩包)，10(其他)
            fileInfo.setFileType(fileTypeEnum.getType());
            // 将上传状态存入fileInfo中，FileStatusEnums.TRANSFER.getStatus()返回1（上传中），2（使用中），3（转码失败）
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            // 将文件属性存入fileInfo中，FileFolderTypeEnums.FILE.getType()返回1（文件），2（目录）
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            // 将删除标记存入fileInfo中，FileDelFlagEnums.USING.getFlag()返回0（删除），1（使用中），2（回收站）
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            // 将文件信息插入数据库
            this.fileInfoMapper.insert(fileInfo);

            // 获取本次上传的文件片大小
            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            // 更新用户空间使用的大小,传入Session中的信息和本次上传的文件片大小
            updateUserSpace(webUserDto, totalSize);

            // 将文件状态设置为upload_finish(上传完成)，UploadStatusEnums.UPLOAD_FINISH.getCode()返回2（上传完成）
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

    /**
     * @description: TODO
     * @param fileId
     * @param webUserDto
     * @return void
     * @author: HuaXian
     * @date: 2023/12/27 17:31
     */
    @Override
    // @Async注解表明是异步执行，创建一个新的线程去执行下面方法
    @Async
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        // 转码标志，true表示转码成功，false表示转码失败
        Boolean transferSuccess = true;
        // 目标文件路径
        String targetFilePath = null;
        // 文件封面
        String cover = null;
        // 文件类型
        FileTypeEnums fileTypeEnum = null;
        // 根据文件Id和用户Id查询文件信息
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());
        try {
            // 如果文件信息为空或者文件状态不是上传中，直接返回
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            // 临时目录,配置文件中的projectFolder + / + /temp/  TODO 这里文件位置是不是有问题，多了/
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            // 定义当前用户文件夹名，当前用户文件夹名为：用户Id + 文件Id
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            // 获取当前用户文件夹
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if (!fileFolder.exists()) {
                /**
                 * mkdirs()和mkdir()的主要区别：主要区别是该目录的父目录是否存在。
                 * mkdir() 如果该目录的父目录不存在，该方法将失败，不会递归创建缺失的父目录
                 * mkdirs() 如果该目录的父目录不存在，会递归创建缺失的父目录
                 */
                fileFolder.mkdirs();
            }
            // 获取文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            // 将date转换为字符串，格式为：yyyyMM，传入month中
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            // 目标目录,配置文件中的projectFolder + /  + /file/ TODO 这里文件位置是不是有问题，多了/
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            // 目标文件夹,目标文件夹为：配置文件中的projectFolder + /  + /file/ + / + 月份
            File targetFolder = new File(targetFolderName + "/" + month);
            // 如果目标文件夹不存在，创建目标文件夹
            if (! targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            // 真实文件名,真实文件名为：用户Id + 文件Id + 文件后缀
            String realFileName = currentUserFolderName + fileSuffix;
            // 真实文件路径,真实文件路径为：目标文件夹(targetFolder) + / + 真实文件名
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            // 合并文件,将临时目录中的文件合并成一个文件，合并后的文件路径为：目标文件夹(targetFolder) + / + 真实文件名
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            // 通过getFileTypeBySuffix方法获取文件类型，传入文件后缀，返回文件类型
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            // 如果文件类型是视频，调用cutFile4Video方法，传入文件Id和目标文件路径
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

    /**
     * @description: 对视频进行切片保存
     * @param fileId 文件Id
     * @param videoFilePath 视频文件路径
     * @return void
     * @author: HuaXian
     * @date: 2023/12/28 18:32
     */
    private void cutFile4Video(String fileId, String videoFilePath) {
        // 创建同名切片目录，切片目录为：视频文件路径（videoFilePath）去掉后缀
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        // 如果切片目录不存在，创建切片目录
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        // 定义一个CMD_TRANSFER_2TS，用于将视频文件转换成.ts文件，%s是占位符，第一个%s是视频文件路径，第二个%s是.ts文件路径
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        // 定义一个CMD_CUT_TS，用于将.ts文件切片，%s是占位符，第一个%s是.ts文件路径，第二个%s是索引文件路径，第三个%s是切片目录路径，第四个%s是文件Id
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        // 定义一个tsPath路径，tsPath为：切片目录路径（tsFolder.getPath()） + / + index.ts
        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        // 生成.ts,将视频文件转换成.ts文件，传入视频文件路径和.ts文件路径
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        // 执行cmd命令，ProcessUtils.executeCommand(cmd, false)返回的是执行结果，true表示执行成功，false表示执行失败
        ProcessUtils.executeCommand(cmd, false);
        // 生成索引文件.m3u8和切片.ts,将.ts文件切片，传入.ts文件路径，索引文件路径，切片目录路径，文件Id
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        // 执行cmd命令，ProcessUtils.executeCommand(cmd, false)返回的是执行结果，true表示执行成功，false表示执行失败
        ProcessUtils.executeCommand(cmd, false);
        // 删除index.ts
        new File(tsPath).delete();
    }

    /**
     * @description: TODO
     * @param dirPath 当前用户目录路径
     * @param toFilePath 合成后的文件路径
     * @param fileName 文件名
     * @param delSource 是否删除源文件
     * @return void
     * @author: HuaXian
     * @date: 2023/12/27 17:58
     */
    private void union(String dirPath, String toFilePath, String fileName, boolean delSource) {
        // 获取当前用户的文件/目录，如果当前用户目录不存在，抛出异常“目录不存在”
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        // 获取当前用户目录下的所有文件，存入fileList中
        File[] fileList = dir.listFiles();
        // 根据toFilePath创建一个文件对象，命名为targetFile TODO 这里为什么不判断是否存在，不存在就创建
        File targetFile = new File(toFilePath);
        // 创建一个RandomAccessFile对象，用于随机（任意）写入文件
        RandomAccessFile writeFile = null;
        try {
            // 懒加载，使用的时候才创建，创建一个RandomAccessFile对象，可以读写（rw）
            writeFile = new RandomAccessFile(targetFile, "rw");
            // 创建一个10M的字节数组
            byte[] b = new byte[1024 * 10];
            // 遍历fileList，将每个文件读取到b中，然后写入targetFile中
            for (int i = 0; i < fileList.length; i++) {
                // 创建一个int类型的变量，用于存储读取的字节数
                int len = -1;
                // 创建读块文件的对象,路径为：当前用户目录路径（dirPath） + /（File.separator会根据不同系统进行动态变化） + i
                File chunkFile = new File(dirPath + File.separator + i);
                // 创建一个RandomAccessFile对象，用于随机（任意）读取文件
                RandomAccessFile readFile = null;
                try {
                    // 懒加载，使用的时候才创建，创建一个RandomAccessFile对象，只读（r）
                    readFile = new RandomAccessFile(chunkFile, "r");
                    /**
                     * 读写文件操作，具体逻辑如下：
                     * 1.readFile.read(b)：将读取的文件存入b中，返回读取的字节数传入len中，如果读取的字节数为-1，说明读取完毕，跳出循环，每次while循环读取一个文件，且会覆盖上一次读取的文件
                     * 2.writeFile.write(b, 0, len)：将b中的内容写入targetFile中，从0开始写，写入len个字节
                     */
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    // 如果读写文件失败，抛出异常
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    // 关闭流
                    readFile.close();
                }
            }
        } catch (Exception e) {
            // 如果合并文件失败，抛出异常
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            try {
                // 关闭流
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                // 如果关闭流失败，抛出异常
                logger.error("关闭流失败", e);
            }
            // 删除源文件,如果delSource为true，删除源文件
            if (delSource) {
                // 判断当前用户目录是否存在，如果存在，删除当前用户目录
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        // 如果删除失败，抛出异常
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
