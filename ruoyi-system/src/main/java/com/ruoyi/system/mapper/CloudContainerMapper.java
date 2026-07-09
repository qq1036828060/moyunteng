package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CloudContainer;

public interface CloudContainerMapper
{
    List<CloudContainer> selectCloudContainerList(CloudContainer container);

    List<CloudContainer> selectCloudContainerSlotList(CloudContainer container);

    CloudContainer selectCloudContainerById(Long containerId);

    CloudContainer selectCloudContainerByProviderId(CloudContainer container);

    List<CloudContainer> selectCloudContainerByHostIndex(CloudContainer container);

    int insertCloudContainer(CloudContainer container);

    int updateCloudContainer(CloudContainer container);

    int upsertCloudContainer(CloudContainer container);

    int releaseCloudContainer(Long containerId);

    int assignCloudContainer(CloudContainer container);

    int assignCloudContainerSlot(CloudContainer container);

    int bindCloudContainerAccount(CloudContainer container);

    int unassignCloudContainer(Long containerId);

    int unassignCloudContainerSlot(CloudContainer container);
}
