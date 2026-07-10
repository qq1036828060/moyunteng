package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CloudSdkCallLog;

/**
 * SDK 调用日志查询服务。
 */
public interface ICloudSdkCallLogService
{
    /** 查询 SDK/Android API 调用日志列表。 */
    List<CloudSdkCallLog> selectCloudSdkCallLogList(CloudSdkCallLog log);
}
