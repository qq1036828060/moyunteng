package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CloudSdkCallLog;

public interface ICloudSdkCallLogService
{
    List<CloudSdkCallLog> selectCloudSdkCallLogList(CloudSdkCallLog log);
}

