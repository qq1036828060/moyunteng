package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CloudSdkCallLog;

/**
 * SDK 调用日志数据访问接口。
 */
public interface CloudSdkCallLogMapper
{
    /** 查询 SDK 调用日志列表。 */
    List<CloudSdkCallLog> selectCloudSdkCallLogList(CloudSdkCallLog log);

    /** 写入一条 SDK 调用日志。 */
    int insertCloudSdkCallLog(CloudSdkCallLog log);
}
