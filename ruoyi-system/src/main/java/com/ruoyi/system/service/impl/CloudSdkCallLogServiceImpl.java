package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.CloudSdkCallLog;
import com.ruoyi.system.mapper.CloudSdkCallLogMapper;
import com.ruoyi.system.service.ICloudSdkCallLogService;

/**
 * SDK 调用日志查询服务实现。
 */
@Service
public class CloudSdkCallLogServiceImpl implements ICloudSdkCallLogService
{
    @Autowired
    private CloudSdkCallLogMapper cloudSdkCallLogMapper;

    /** 查询 SDK / Android API 调用日志列表。 */
    @Override
    public List<CloudSdkCallLog> selectCloudSdkCallLogList(CloudSdkCallLog log)
    {
        return cloudSdkCallLogMapper.selectCloudSdkCallLogList(log);
    }
}
