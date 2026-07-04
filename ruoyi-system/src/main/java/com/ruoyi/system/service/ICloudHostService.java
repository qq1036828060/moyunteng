package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.CloudHost;

public interface ICloudHostService
{
    List<CloudHost> selectCloudHostList(CloudHost host);

    CloudHost selectCloudHostById(Long hostId);

    int insertCloudHost(CloudHost host);

    int updateCloudHost(CloudHost host);

    int deleteCloudHostByIds(Long[] hostIds);

    Map<String, Object> testConnection(Long hostId);

    int syncContainers(Long hostId, String operator);
}

