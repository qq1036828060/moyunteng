package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CloudHost;

public interface CloudHostMapper
{
    List<CloudHost> selectCloudHostList(CloudHost host);

    CloudHost selectCloudHostById(Long hostId);

    int insertCloudHost(CloudHost host);

    int updateCloudHost(CloudHost host);

    int deleteCloudHostByIds(Long[] hostIds);
}

