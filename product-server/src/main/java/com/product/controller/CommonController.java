package com.product.controller;

import com.product.common.config.ProductConfig;
import com.product.common.core.result.AjaxResult;
import com.product.common.utils.file.FileUploadUtils;
import com.product.common.utils.file.FileUtils;
import com.product.framework.config.ServerConfig;
import com.product.core.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Auther: chuan
 * @Date: 2025/12/24 - 12 - 24 - 00:31
 * @Description: com.product.controller
 * @version: 1.0
 */
@RestController
@RequestMapping("/common")
public class CommonController extends BaseController {
    @Autowired
    private ServerConfig serverConfig;

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = ProductConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
