package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CloudContainer;

public interface ICloudContainerService
{
    List<CloudContainer> selectCloudContainerList(CloudContainer container);

    CloudContainer selectCloudContainerById(Long containerId);

    void startContainer(Long containerId);

    void stopContainer(Long containerId);

    void restartContainer(Long containerId);

    int releaseContainer(Long containerId);
}

