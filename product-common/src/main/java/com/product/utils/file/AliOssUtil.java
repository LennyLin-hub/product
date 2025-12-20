package com.product.utils.file;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.product.utils.DateUtils;
import com.product.utils.StringUtils;
import com.product.utils.uuid.Seq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

/**
 * @Auther: chuan
 * @Date: 2025/11/26 - 11 - 26 - 22:15
 * @Description: alioss工具类
 * @version: 1.0
 */
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes 文件字节数组
     * @param objectName 文件在OSS中的存储名称
     * @return 文件访问URL
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //构建文件访问路径
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }

    /**
     * 头像上传：沿用原本本地上传的校验与命名规则，将文件直接上传到 OSS。
     *
     * @param file MultipartFile
     * @return 可访问的 OSS URL
     */
    public String uploadAvatar(MultipartFile file) throws Exception {
        // 复用原有校验规则，保证行为与本地上传一致
        FileUploadUtils.assertAllowed(file, MimeTypeUtils.IMAGE_EXTENSION);
        // 生成与本地一致的日期路径+序列文件名
        String objectName = "avatar:" + extractFilename(file);
        log.info("文件名:{}", objectName);
        return upload(file.getBytes(), objectName);
    }

    /**
     * 生成唯一的文件名
     * 格式：日期路径/原文件名_序列号.扩展名
     * 例如：2024/12/20/avatar_1634567890123.jpg
     *
     * @param file 上传的文件对象
     * @return 生成的唯一文件名
     */
    public static final String extractFilename(MultipartFile file)
    {
        return StringUtils.format("{}_{}_{}.{}", DateUtils.dateToFilePath(),
                FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), FileUploadUtils.getExtension(file));
    }
}
