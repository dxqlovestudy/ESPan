package com.xqcoder.easypan.utils;

import com.xqcoder.easypan.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);
    /**
     * @description: 执行FFmpeg命令
     * @param cmd 需要执行的命令
     * @param outprintLog 是否输出日志
     * @return java.lang.String
     * @author: HuaXian
     * @date: 2023/12/28 21:50
     */
    public static String executeCommand(String cmd, Boolean outprintLog) {
        // 判断要执行的命令是否为空,如果为空，报错并返回null
        if (StringTools.isEmpty(cmd)) {
            logger.error("--- 指令执行失败，因为要执行的FFmpeg指令为空！ ---");
            return null;
        }
        // 执行命令，通过Runtime.getRuntime()获取当前运行时环境，然后执行指令
        /**
         * 这里定义了Runtime runtime = Runtime.getRuntime();
         * 后面的process = Runtime.getRuntime().exec(cmd);为什么不直接使用runtime？
         */
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            // 执行ffmpeg指令
            // 取出输出流和错误流的信息
            // 注意：必须取出ffmpeg在执行命令过程中产生的输出信息，如果不取的话当输出流信息填满jvm存储输出留信息的缓冲区时，线程就回阻塞住
            PrintStream errorStream = new PrintStream(process.getErrorStream());
            PrintStream inputStream = new PrintStream(process.getInputStream());
            errorStream.start();
            inputStream.start();
            // 等待ffmpeg命令执行完
            process.waitFor();
            // 获取执行结果字符串
            String result = errorStream.stringBuffer.append(inputStream.stringBuffer + "\n").toString();
            // 输出执行的命令信息
            if (outprintLog) {
                logger.info("执行命令:{}，已执行完毕,执行结果:{}", cmd, result);
            } else {
                logger.info("执行命令:{}，已执行完毕", cmd);
            }
            return result;
        } catch (Exception e) {
            // logger.error("执行命令失败:{} ", e.getMessage());
            e.printStackTrace();
            throw new BusinessException("视频转换失败");
        } finally {
            if (null != process) {
                ProcessKiller ffmpegKiller = new ProcessKiller(process);
                runtime.addShutdownHook(ffmpegKiller);
            }
        }
    }

    /**
     * 用于取出ffmpeg线程执行过程中产生的各种输出和错误流的信息
     */
    static class PrintStream extends Thread {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuffer stringBuffer = new StringBuffer();

        /**
         * 构造方法
         * @param inputStream 输入流
         */
        public PrintStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * 重写run方法
         */
        @Override
        public void run() {
            try {
                // 如果输入流不为空，则开始读取
                if (null == inputStream) {
                    return;
                }
                /**
                 * InputStream inputStream是字节流对象，用于读取字节数据。
                 * InputStreamReader inputStreamReader是字符流通向字节流的桥梁，将字节流转换为字符流，允许按字符读取字节数据。
                 * BufferedReader bufferedReader是 Java 提供的缓冲字符输入流，它提供了一种更高效的方式来读取字符数据，通过缓冲可以减少对底层资源的访问次数，提高读取效率。
                 */
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                // 读取输入流中的内容
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }
            } catch (Exception e) {
                logger.error("读取输入流出错了！错误信息：" + e.getMessage());
            } finally {
                try {
                    if (null != bufferedReader) {
                        bufferedReader.close();
                    }
                    if (null != inputStream) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    logger.error("调用PrintStream读取输出流后，关闭流时出错！");
                }
            }
        }
    }

    /**
     * 在程序退出前结束已有的FFmpeg进程
     */
    private static class ProcessKiller extends Thread {
        private Process process;
        public ProcessKiller(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            this.process.destroy();
        }
    }
}
