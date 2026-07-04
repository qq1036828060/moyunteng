package com.ruoyi.web.controller.cloud;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.CloudSdkCallLog;
import com.ruoyi.system.service.ICloudSdkCallLogService;

@RestController
@RequestMapping("/cloud/sdk-log")
public class CloudSdkCallLogController extends BaseController
{
    @Autowired
    private ICloudSdkCallLogService cloudSdkCallLogService;

    @PreAuthorize("@ss.hasPermi('cloud:sdklog:list')")
    @GetMapping("/list")
    public TableDataInfo list(CloudSdkCallLog log)
    {
        startPage();
        List<CloudSdkCallLog> list = cloudSdkCallLogService.selectCloudSdkCallLogList(log);
        return getDataTable(list);
    }
}

