package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.cloud.MoyuntengSdkClient;
import com.ruoyi.system.cloud.MytAndroidContainer;
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
        CloudHost host = requireHost(container.getHostId());
        moyuntengSdkClient.startAndroid(host, container.getContainerName());
        refreshContainerFromSdk(host, container);
    }

    @Override
    public void stopContainer(Long containerId)
    {
        CloudContainer container = requireContainer(containerId);
        CloudHost host = requireHost(container.getHostId());
        moyuntengSdkClient.stopAndroid(host, container.getContainerName());
        refreshContainerFromSdk(host, container);
    }

    @Override
    public void restartContainer(Long containerId)
    {
        CloudContainer container = requireContainer(containerId);
        CloudHost host = requireHost(container.getHostId());
        moyuntengSdkClient.restartAndroid(host, container.getContainerName());
        refreshContainerFromSdk(host, container);
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
            throw new ServiceException("Cloud container does not exist");
        }
        return container;
    }

    private CloudHost requireHost(Long hostId)
    {
        CloudHost host = cloudHostMapper.selectCloudHostById(hostId);
        if (host == null)
        {
            throw new ServiceException("Cloud host does not exist");
        }
        return host;
    }

    private void refreshContainerFromSdk(CloudHost host, CloudContainer container)
    {
        sleepAfterCommand();
        List<MytAndroidContainer> containers = moyuntengSdkClient.listAndroid(host);
        for (MytAndroidContainer item : containers)
        {
            if (container.getProviderContainerId().equals(item.getId())
                    || container.getContainerName().equals(item.getName()))
            {
                CloudContainer update = new CloudContainer();
                update.setContainerId(container.getContainerId());
                update.setContainerName(item.getName());
                update.setInstanceId(item.getId());
                update.setIndexNum(item.getIndexNum());
                update.setContainerStatus(item.getStatus());
                update.setAndroidType(item.getAndroidType());
                update.setContainerIp(item.getIp());
                update.setNetworkName(item.getNetworkName());
                update.setImage(item.getImage());
                update.setWidth(item.getWidth());
                update.setHeight(item.getHeight());
                update.setDpi(item.getDpi());
                update.setWebrtcTcpPort(item.getWebrtcTcpPort());
                update.setWebrtcUdpPort(item.getWebrtcUdpPort());
                update.setAdbPort(item.getAdbPort());
                update.setRawJson(item.getRawJson());
                cloudContainerMapper.updateCloudContainer(update);
                return;
            }
        }
    }

    private void sleepAfterCommand()
    {
        try
        {
            Thread.sleep(1200L);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

