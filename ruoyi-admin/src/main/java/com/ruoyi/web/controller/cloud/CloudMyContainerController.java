package com.ruoyi.web.controller.cloud;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerProxyRequest;
import com.ruoyi.system.service.ICloudContainerService;

@RestController
@RequestMapping("/cloud/my/container")
public class CloudMyContainerController extends BaseController
{
    @Autowired
    private ICloudContainerService cloudContainerService;

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:list')")
    @GetMapping("/list")
    public TableDataInfo list(CloudContainer container)
    {
        startPage();
        List<CloudContainer> list = cloudContainerService.selectMyCloudContainerList(getUserId(), container);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:list')")
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(cloudContainerService.selectMyCloudContainerSummary(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @GetMapping("/{containerId}/webrtc")
    public AjaxResult webrtc(@PathVariable Long containerId)
    {
        return success(cloudContainerService.selectMyContainerWebrtcInfo(getUserId(), containerId));
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "我的云机启动", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/start")
    public AjaxResult start(@PathVariable Long containerId)
    {
        cloudContainerService.startMyContainer(getUserId(), containerId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "我的云机停止", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/stop")
    public AjaxResult stop(@PathVariable Long containerId)
    {
        cloudContainerService.stopMyContainer(getUserId(), containerId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "我的云机重启", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/restart")
    public AjaxResult restart(@PathVariable Long containerId)
    {
        cloudContainerService.restartMyContainer(getUserId(), containerId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:bind')")
    @Log(title = "我的云机绑定账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{containerId}/account")
    public AjaxResult bindAccount(@PathVariable Long containerId, @RequestBody Map<String, String> body)
    {
        return toAjax(cloudContainerService.bindMyContainerAccount(getUserId(), containerId, body.get("boundAccountNo")));
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "S5 Proxy", businessType = BusinessType.UPDATE)
    @PutMapping("/{containerId}/proxy/s5")
    public AjaxResult setS5Proxy(@PathVariable Long containerId, @RequestBody CloudContainerProxyRequest request)
    {
        return success(cloudContainerService.setMyContainerS5Proxy(getUserId(), containerId, request));
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @GetMapping("/{containerId}/proxy/s5")
    public AjaxResult getS5Proxy(@PathVariable Long containerId)
    {
        return success(cloudContainerService.getMyContainerS5ProxyStatus(getUserId(), containerId));
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "Open QQ", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/qq/open")
    public AjaxResult openQq(@PathVariable Long containerId)
    {
        return success(cloudContainerService.openMyContainerQq(getUserId(), containerId));
    }

    @PreAuthorize("@ss.hasPermi('cloud:mycontainer:operate')")
    @Log(title = "QQ Scan", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/qq/scan")
    public AjaxResult scanQq(@PathVariable Long containerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filePath", required = false) String filePath) throws Exception
    {
        return success(cloudContainerService.scanMyContainerQq(getUserId(), containerId, file, filePath));
    }
}
