package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CloudSdkCallLog;

public interface CloudSdkCallLogMapper
{
    List<CloudSdkCallLog> selectCloudSdkCallLogList(CloudSdkCallLog log);

    int insertCloudSdkCallLog(CloudSdkCallLog log);
}

