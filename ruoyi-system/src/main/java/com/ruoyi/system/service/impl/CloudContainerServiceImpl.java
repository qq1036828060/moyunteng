package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.cloud.MoyuntengSdkClient;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudHost;
import com.ruoyi.system.mapper.CloudContainerMapper;
import com.ruoyi.system.mapper.CloudHostMapper;
import com.ruoyi.system.service.ICloudContainerService;

@Service
public class CloudContainerServiceImpl implements ICloudContainerService
{
    @Autowired
    private CloudContainerMapper cloudContainerMapper;

    @Autowired
    private CloudHostMapper cloudHostMapper;

    @Autowired
    private MoyuntengSdkClient moyuntengSdkClient;

    @Override
    public List<CloudContainer> selectCloudContainerList(CloudContainer container)
    {
        return cloudContainerMapper.selectCloudContainerList(container);
    }

    @Override
    public CloudContainer selectCloudContainerById(Long containerId)
    {
        return cloudContainerMapper.selectCloudContainerById(containerId);
    }

    @Override
    public void startContainer(Long containerId)
    {
        CloudContainer container = requireContainer(containerId);
        moyuntengSdkClient.startAndroid(requireHost(container.getHostId()), container.getContainerName());
    }

    @Override
    public void stopContainer(Long containerId)
    {
        CloudContainer container = requireContainer(containerId);
        moyuntengSdkClient.stopAndroid(requireHost(container.getHostId()), container.getContainerName());
    }

    @Override
    public void restartContainer(Long containerId)
    {
        CloudContainer container = requireContainer(containerId);
        moyuntengSdkClient.restartAndroid(requireHost(container.getHostId()), container.getContainerName());
    }

    @Override
    public int releaseContainer(Long containerId)
    {
        return cloudContainerMapper.releaseCloudContainer(containerId);
    }

    private CloudContainer requireContainer(Long containerId)
    {
        CloudContainer container = cloudContainerMapper.selectCloudContainerById(containerId);
        if (container == null)
        {
            throw new ServiceException("云机容器不存在");
        }
        return container;
    }

    private CloudHost requireHost(Long hostId)
    {
        CloudHost host = cloudHostMapper.selectCloudHostById(hostId);
        if (host == null)
        {
            throw new ServiceException("魔云腾主机不存在");
        }
        return host;
    }
}

