package com.ruoyi.system.service;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 扫号业务云机操作服务。
 */
public interface ICloudSaohaoService
{
    /**
     * 使用当前用户分配的云机完成支付宝扫码并输入支付密码。
     *
     * @param userId 当前登录用户ID
     * @param containerId 可选云机ID，用户只有一个实例位时可以不传
     * @param file 二维码图片
     * @param filePath 前端图片来源地址，仅用于结果记录
     * @param payPassword 六位数字支付密码
     * @return 指令下发结果
     */
    Map<String, Object> scanAndPay(Long userId, Long containerId, MultipartFile file,
            String filePath, String payPassword);
}
