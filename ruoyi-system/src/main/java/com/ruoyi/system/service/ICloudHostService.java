package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.CloudHost;

/**
 * 云机主机管理服务。
 */
public interface ICloudHostService
{
    /** 查询主机列表。 */
    List<CloudHost> selectCloudHostList(CloudHost host);

    /** 查询主机详情。 */
    CloudHost selectCloudHostById(Long hostId);

    /** 新增主机配置。 */
    int insertCloudHost(CloudHost host);

    /** 修改主机配置。 */
    int updateCloudHost(CloudHost host);

    /** 批量删除主机。 */
    int deleteCloudHostByIds(Long[] hostIds);

    /** 测试主机 SDK 连接，并回写版本和在线状态。 */
    Map<String, Object> testConnection(Long hostId);

    /** 从魔云腾同步容器列表，新增/更新现有容器，并标记已移除容器。 */
    int syncContainers(Long hostId, String operator);
}
