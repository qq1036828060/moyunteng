package com.ruoyi.web.controller.cloud;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.service.ICloudContainerService;

@RestController
@RequestMapping("/cloud/container")
public class CloudContainerController extends BaseController
{
    @Autowired
    private ICloudContainerService cloudContainerService;

    @PreAuthorize("@ss.hasPermi('cloud:container:list')")
    @GetMapping("/list")
    public TableDataInfo list(CloudContainer container)
    {
        startPage();
        List<CloudContainer> list = cloudContainerService.selectCloudContainerList(container);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cloud:container:query')")
    @GetMapping("/{containerId}")
    public AjaxResult getInfo(@PathVariable Long containerId)
    {
        return success(cloudContainerService.selectCloudContainerById(containerId));
    }

    @PreAuthorize("@ss.hasPermi('cloud:container:start')")
    @Log(title = "云机容器", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/start")
    public AjaxResult start(@PathVariable Long containerId)
    {
        cloudContainerService.startContainer(containerId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('cloud:container:stop')")
    @Log(title = "云机容器", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/stop")
    public AjaxResult stop(@PathVariable Long containerId)
    {
        cloudContainerService.stopContainer(containerId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('cloud:container:restart')")
    @Log(title = "云机容器", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/restart")
    public AjaxResult restart(@PathVariable Long containerId)
    {
        cloudContainerService.restartContainer(containerId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('cloud:container:release')")
    @Log(title = "云机容器", businessType = BusinessType.UPDATE)
    @PostMapping("/{containerId}/release")
    public AjaxResult release(@PathVariable Long containerId)
    {
        return toAjax(cloudContainerService.releaseContainer(containerId));
    }
}

