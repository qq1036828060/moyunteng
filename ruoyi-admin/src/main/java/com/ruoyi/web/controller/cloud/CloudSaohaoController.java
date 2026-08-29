package com.ruoyi.web.controller.cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.CloudBankCodeRequest;
import com.ruoyi.system.domain.CloudBankPayRequest;
import com.ruoyi.system.service.ICloudSaohaoService;

/**
 * 扫号分支用户端支付操作接口。
 */
@RestController
@RequestMapping("/cloud/saohao/pay")
public class CloudSaohaoController extends BaseController
{
    @Autowired
    private ICloudSaohaoService cloudSaohaoService;

    /**
     * 上传二维码并使用当前用户分配的云机执行支付宝扫码付款流程。
     * 请求参数包含支付密码，因此操作日志不保存请求参数。
     */
    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "支付宝扫码付款", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @PostMapping("/scan")
    public AjaxResult scan(@RequestParam("file") MultipartFile file,
            @RequestParam("payPassword") String payPassword,
            @RequestParam(value = "containerId", required = false) Long containerId,
            @RequestParam(value = "filePath", required = false) String filePath)
    {
        return success(cloudSaohaoService.scanAndPay(getUserId(), containerId, file, filePath, payPassword));
    }

    /** 下发银行卡支付命令，操作到短信验证码等待页面。 */
    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "银行卡支付", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @PostMapping("/bank")
    public AjaxResult bank(@RequestBody(required = false) CloudBankPayRequest request)
    {
        return success(cloudSaohaoService.startBankPayment(getUserId(), request));
    }

    /** 接收银行卡短信验证码并确认支付。 */
    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "银行卡支付验证码", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @PostMapping("/bankcode")
    public AjaxResult bankCode(@RequestBody CloudBankCodeRequest request)
    {
        return success(cloudSaohaoService.submitBankCode(getUserId(), request));
    }
}
