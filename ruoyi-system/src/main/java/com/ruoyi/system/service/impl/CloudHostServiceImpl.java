package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.cloud.MoyuntengSdkClient;
import com.ruoyi.system.cloud.MytAndroidContainer;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudHost;
import com.ruoyi.system.mapper.CloudContainerMapper;
import com.ruoyi.system.mapper.CloudHostMapper;
import com.ruoyi.system.service.ICloudHostService;

@Service
public class CloudHostServiceImpl implements ICloudHostService
{
    @Autowired
    private CloudHostMapper cloudHostMapper;

    @Autowired
    private CloudContainerMapper cloudContainerMapper;

    @Autowired
    private MoyuntengSdkClient moyuntengSdkClient;

    @Override
    public List<CloudHost> selectCloudHostList(CloudHost host)
    {
        return cloudHostMapper.selectCloudHostList(host);
    }

    @Override
    public CloudHost selectCloudHostById(Long hostId)
    {
        return cloudHostMapper.selectCloudHostById(hostId);
    }

    @Override
    public int insertCloudHost(CloudHost host)
    {
        normalizeHost(host);
        if (StringUtils.isEmpty(host.getOnlineStatus()))
        {
            host.setOnlineStatus("UNKNOWN");
        }
        if (StringUtils.isEmpty(host.getEnabled()))
        {
            host.setEnabled("1");
        }
        if (host.getContainerCapacity() == null)
        {
            host.setContainerCapacity(0);
        }
        return cloudHostMapper.insertCloudHost(host);
    }

    @Override
    public int updateCloudHost(CloudHost host)
    {
        normalizeHost(host);
        return cloudHostMapper.updateCloudHost(host);
    }

    @Override
    public int deleteCloudHostByIds(Long[] hostIds)
    {
        return cloudHostMapper.deleteCloudHostByIds(hostIds);
    }

    @Override
    public Map<String, Object> testConnection(Long hostId)
    {
        CloudHost host = requireHost(hostId);
        Map<String, Object> info = moyuntengSdkClient.getInfo(host);
        host.setLatestVersion((Integer) info.get("latestVersion"));
        host.setCurrentVersion((Integer) info.get("currentVersion"));
        host.setOnlineStatus("ONLINE");
        host.setLastSyncTime(new Date());
        cloudHostMapper.updateCloudHost(host);
        return info;
    }

    @Override
    public int syncContainers(Long hostId, String operator)
    {
        CloudHost host = requireHost(hostId);
        List<MytAndroidContainer> containers = moyuntengSdkClient.listAndroid(host);
        Map<Integer, CloudContainer> slotAssignments = selectSlotAssignments(host.getHostId());
        List<String> providerContainerIds = new ArrayList<>();
        int count = 0;
        for (MytAndroidContainer item : containers)
        {
            if (StringUtils.isNotEmpty(item.getId()))
            {
                providerContainerIds.add(item.getId());
            }
            CloudContainer container = new CloudContainer();
            container.setHostId(host.getHostId());
            container.setProviderContainerId(item.getId());
            container.setContainerName(item.getName());
            container.setInstanceId(item.getId());
            container.setIndexNum(item.getIndexNum());
            container.setContainerStatus(item.getStatus());
            container.setScheduleStatus("IDLE");
            container.setAndroidType(item.getAndroidType());
            container.setContainerIp(item.getIp());
            container.setNetworkName(item.getNetworkName());
            container.setImage(item.getImage());
            container.setWidth(item.getWidth());
            container.setHeight(item.getHeight());
            container.setDpi(item.getDpi());
            container.setWebrtcTcpPort(item.getWebrtcTcpPort());
            container.setWebrtcUdpPort(item.getWebrtcUdpPort());
            container.setAdbPort(item.getAdbPort());
            container.setRawJson(item.getRawJson());
            container.setCreateBy(operator);
            inheritSlotAssignment(container, slotAssignments);
            cloudContainerMapper.upsertCloudContainer(container);
            count++;
        }
        cloudContainerMapper.markDeletedByMissingProviderIds(host.getHostId(), providerContainerIds);
        host.setOnlineStatus("ONLINE");
        host.setContainerCapacity(count);
        host.setLastSyncTime(new Date());
        host.setUpdateBy(operator);
        cloudHostMapper.updateCloudHost(host);
        return count;
    }

    private Map<Integer, CloudContainer> selectSlotAssignments(Long hostId)
    {
        CloudContainer query = new CloudContainer();
        query.setHostId(hostId);
        List<CloudContainer> localContainers = cloudContainerMapper.selectCloudContainerList(query);
        Map<Integer, CloudContainer> assignments = new HashMap<>();
        Set<Integer> conflictSlots = new HashSet<>();
        for (CloudContainer local : localContainers)
        {
            if (local.getIndexNum() == null || local.getAssignedUserId() == null || conflictSlots.contains(local.getIndexNum()))
            {
                continue;
            }
            CloudContainer current = assignments.get(local.getIndexNum());
            if (current == null)
            {
                assignments.put(local.getIndexNum(), local);
                continue;
            }
            if (!current.getAssignedUserId().equals(local.getAssignedUserId()))
            {
                assignments.remove(local.getIndexNum());
                conflictSlots.add(local.getIndexNum());
            }
        }
        return assignments;
    }

    private void inheritSlotAssignment(CloudContainer container, Map<Integer, CloudContainer> slotAssignments)
    {
        if (container.getIndexNum() == null)
        {
            return;
        }
        CloudContainer assignment = slotAssignments.get(container.getIndexNum());
        if (assignment == null)
        {
            return;
        }
        container.setAssignedUserId(assignment.getAssignedUserId());
        container.setAssignedUserName(assignment.getAssignedUserName());
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

    private void normalizeHost(CloudHost host)
    {
        if (host.getApiPort() == null)
        {
            host.setApiPort(8000);
        }
        if (StringUtils.isEmpty(host.getApiBaseUrl()) && StringUtils.isNotEmpty(host.getHostIp()))
        {
            host.setApiBaseUrl("http://" + host.getHostIp() + ":" + host.getApiPort());
        }
    }
}
