package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CloudHost;

/**
 * 云机主机数据访问接口。
 */
public interface CloudHostMapper
{
    /** 查询主机列表。 */
    List<CloudHost> selectCloudHostList(CloudHost host);

    /** 按ID查询主机。 */
    CloudHost selectCloudHostById(Long hostId);

    /** 新增主机。 */
    int insertCloudHost(CloudHost host);

    /** 更新主机。 */
    int updateCloudHost(CloudHost host);

    /** 批量逻辑删除主机。 */
    int deleteCloudHostByIds(Long[] hostIds);
}
