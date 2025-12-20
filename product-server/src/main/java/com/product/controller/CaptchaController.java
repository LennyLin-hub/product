package com.product.controller;

import com.google.code.kaptcha.Producer;
import com.product.annotation.Anonymous;
import com.product.config.ProductConfig;
import com.product.constant.CacheConstants;
import com.product.constant.Constants;
import com.product.core.redis.RedisCache;
import com.product.core.result.AjaxResult;
import com.product.utils.sign.Base64;
import com.product.utils.uuid.IdUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 验证码操作处理
 *
 * @author fast
 */
@Slf4j
@RestController
public class CaptchaController
{
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ProductConfig productConfig;

    /**
     * 生成验证码
     */
    @Anonymous
    @GetMapping("/captchaImage")
    public AjaxResult getCode(HttpServletResponse response) throws IOException
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = true;

        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            return ajax;
        }

        // 用于唯一标识验证码
        String uuid = IdUtils.simpleUUID();

        // 缓存键
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        // 生成验证码
        String captchaType = productConfig.getCaptchaType();

        if ("math".equals(captchaType))
        {
            // 运算式验证码
            String capText = captchaProducerMath.createText();  // 生成运算式，类似2+3=@5
            capStr = capText.substring(0, capText.lastIndexOf("@"));    // 截取获得运算式2+3
            code = capText.substring(capText.lastIndexOf("@") + 1); // 截取获得结果5
            log.info("获取验证码: {}", capText);
            image = captchaProducerMath.createImage(capStr);    // 生成图片
        }
        else if ("char".equals(captchaType))
        {
            // 字符串验证码
            capStr = code = captchaProducer.createText();   // 随机字符序列
            image = captchaProducer.createImage(capStr);    // 生成图片
        }

        redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        log.info("当前缓存的键: {}", redisCache.keys(CacheConstants.CAPTCHA_CODE_KEY));
        log.info("当前缓存的验证码: {}", redisCache.getCacheObject(verifyKey).toString());
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "jpg", os);
        }
        catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        // 响应中放入唯一标识uuid
        ajax.put("uuid", uuid);
        //
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }
}
