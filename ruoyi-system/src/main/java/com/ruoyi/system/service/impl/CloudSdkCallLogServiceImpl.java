package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.CloudSdkCallLog;
import com.ruoyi.system.mapper.CloudSdkCallLogMapper;
import com.ruoyi.system.service.ICloudSdkCallLogService;

@Service
public class CloudSdkCallLogServiceImpl implements ICloudSdkCallLogService
{
    @Autowired
    private CloudSdkCallLogMapper cloudSdkCallLogMapper;

    @Override
    public List<CloudSdkCallLog> selectCloudSdkCallLogList(CloudSdkCallLog log)
    {
        return cloudSdkCallLogMapper.selectCloudSdkCallLogList(log);
    }
}

