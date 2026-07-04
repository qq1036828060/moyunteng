package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CloudContainer;

public interface CloudContainerMapper
{
    List<CloudContainer> selectCloudContainerList(CloudContainer container);

    CloudContainer selectCloudContainerById(Long containerId);

    CloudContainer selectCloudContainerByProviderId(CloudContainer container);

    int insertCloudContainer(CloudContainer container);

    int updateCloudContainer(CloudContainer container);

    int upsertCloudContainer(CloudContainer container);

    int releaseCloudContainer(Long containerId);
}

