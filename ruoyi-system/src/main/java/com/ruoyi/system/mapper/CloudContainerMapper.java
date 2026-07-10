package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CloudContainer;

/**
 * 云机容器数据访问接口。
 */
public interface CloudContainerMapper
{
    /** 查询容器明细列表。 */
    List<CloudContainer> selectCloudContainerList(CloudContainer container);

    /** 查询按主机实例位聚合后的容器列表。 */
    List<CloudContainer> selectCloudContainerSlotList(CloudContainer container);

    /** 按容器ID查询容器。 */
    CloudContainer selectCloudContainerById(Long containerId);

    /** 按魔云腾容器ID查询容器。 */
    CloudContainer selectCloudContainerByProviderId(CloudContainer container);

    /** 查询同一主机同一实例位下的所有容器。 */
    List<CloudContainer> selectCloudContainerByHostIndex(CloudContainer container);

    /** 新增容器。 */
    int insertCloudContainer(CloudContainer container);

    /** 更新容器。 */
    int updateCloudContainer(CloudContainer container);

    /** 按魔云腾容器ID插入或更新容器，同步时使用。 */
    int upsertCloudContainer(CloudContainer container);

    /** 将本地存在但本次魔云腾列表中已缺失的容器标记为删除。 */
    int markDeletedByMissingProviderIds(@Param("hostId") Long hostId, @Param("providerContainerIds") List<String> providerContainerIds);

    /** 释放容器资源和绑定信息。 */
    int releaseCloudContainer(Long containerId);

    /** 分配单个容器给用户。 */
    int assignCloudContainer(CloudContainer container);

    /** 按主机实例位分配该实例位下所有容器给用户。 */
    int assignCloudContainerSlot(CloudContainer container);

    /** 绑定业务账号到容器。 */
    int bindCloudContainerAccount(CloudContainer container);

    /** 取消单个容器分配。 */
    int unassignCloudContainer(Long containerId);

    /** 按主机实例位取消该实例位下所有容器分配。 */
    int unassignCloudContainerSlot(CloudContainer container);
}
