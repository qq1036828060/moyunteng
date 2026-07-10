package com.ruoyi.web.controller.cloud;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.CloudHost;
import com.ruoyi.system.service.ICloudHostService;

/**
 * 主端云机主机管理接口。
 */
@RestController
@RequestMapping("/cloud/host")
public class CloudHostController extends BaseController
{
    @Autowired
    private ICloudHostService cloudHostService;

    /** 分页查询主机列表。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:list')")
    @GetMapping("/list")
    public TableDataInfo list(CloudHost host)
    {
        startPage();
        List<CloudHost> list = cloudHostService.selectCloudHostList(host);
        return getDataTable(list);
    }

    /** 查询主机详情。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:query')")
    @GetMapping("/{hostId}")
    public AjaxResult getInfo(@PathVariable Long hostId)
    {
        return success(cloudHostService.selectCloudHostById(hostId));
    }

    /** 新增魔云腾主机配置。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:add')")
    @Log(title = "魔云腾主机", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CloudHost host)
    {
        host.setCreateBy(getUsername());
        return toAjax(cloudHostService.insertCloudHost(host));
    }

    /** 修改魔云腾主机配置。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:edit')")
    @Log(title = "魔云腾主机", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CloudHost host)
    {
        host.setUpdateBy(getUsername());
        return toAjax(cloudHostService.updateCloudHost(host));
    }

    /** 删除主机配置。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:remove')")
    @Log(title = "魔云腾主机", businessType = BusinessType.DELETE)
    @DeleteMapping("/{hostIds}")
    public AjaxResult remove(@PathVariable Long[] hostIds)
    {
        return toAjax(cloudHostService.deleteCloudHostByIds(hostIds));
    }

    /** 测试主机 SDK 连接。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:test')")
    @Log(title = "魔云腾主机", businessType = BusinessType.OTHER)
    @PostMapping("/{hostId}/test")
    public AjaxResult test(@PathVariable Long hostId)
    {
        return success(cloudHostService.testConnection(hostId));
    }

    /** 从魔云腾同步该主机下的容器列表。 */
    @PreAuthorize("@ss.hasPermi('cloud:host:sync')")
    @Log(title = "魔云腾主机", businessType = BusinessType.OTHER)
    @PostMapping("/{hostId}/syncContainers")
    public AjaxResult syncContainers(@PathVariable Long hostId)
    {
        return success(cloudHostService.syncContainers(hostId, getUsername()));
    }
}
