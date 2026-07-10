package com.ruoyi.web.controller.cloud;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerAssignRequest;
import com.ruoyi.system.service.ICloudContainerService;

/**
 * 主端容器资源池管理接口。
 */
@RestController
@RequestMapping("/cloud/container")
public class CloudContainerController extends BaseController
{
    @Autowired
    private ICloudContainerService cloudContainerService;

    /** 分页查询按实例位聚合后的容器资源池。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:list')")
    @GetMapping("/list")
    public TableDataInfo list(CloudContainer container)
    {
        startPage();
        List<CloudContainer> list = cloudContainerService.selectCloudContainerSlotList(container);
        return getDataTable(list);
    }

    /** 查询容器详情。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:query')")
    @GetMapping("/{containerId}")
    public AjaxResult getInfo(@PathVariable Long containerId)
    {
        return success(cloudContainerService.selectCloudContainerById(containerId));
    }

    /** 按主机实例位批量分配云机给用户。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:assign')")
    @Log(title = "云机容器分配", businessType = BusinessType.UPDATE)
    @PostMapping("/assign")
    public AjaxResult assign(@RequestBody CloudContainerAssignRequest request)
    {
        return toAjax(cloudContainerService.assignContainerByHostIndex(request, getUsername()));
    }

    /** 取消单个容器分配。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:assign')")
    @Log(title = "云机容器取消分配", businessType = BusinessType.UPDATE)
    @PostMapping("/{containerId}/unassign")
    public AjaxResult unassign(@PathVariable Long containerId)
    {
        return toAjax(cloudContainerService.unassignContainer(containerId));
    }

    /** 按主机实例位取消分配。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:assign')")
    @Log(title = "云机实例位取消分配", businessType = BusinessType.UPDATE)
    @PostMapping("/unassign-slot")
    public AjaxResult unassignSlot(@RequestBody CloudContainerAssignRequest request)
    {
        return toAjax(cloudContainerService.unassignContainerByHostIndex(request));
    }

    /** 主端启动容器。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:start')")
    @Log(title = "云机容器", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/start")
    public AjaxResult start(@PathVariable Long containerId)
    {
        cloudContainerService.startContainer(containerId);
        return success();
    }

    /** 主端停止容器。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:stop')")
    @Log(title = "云机容器", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/stop")
    public AjaxResult stop(@PathVariable Long containerId)
    {
        cloudContainerService.stopContainer(containerId);
        return success();
    }

    /** 主端重启容器。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:restart')")
    @Log(title = "云机容器", businessType = BusinessType.OTHER)
    @PostMapping("/{containerId}/restart")
    public AjaxResult restart(@PathVariable Long containerId)
    {
        cloudContainerService.restartContainer(containerId);
        return success();
    }

    /** 释放容器调度、分配和账号绑定资源。 */
    @PreAuthorize("@ss.hasPermi('cloud:container:release')")
    @Log(title = "云机容器", businessType = BusinessType.UPDATE)
    @PostMapping("/{containerId}/release")
    public AjaxResult release(@PathVariable Long containerId)
    {
        return toAjax(cloudContainerService.releaseContainer(containerId));
    }
}
