package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerAssignRequest;
import com.ruoyi.system.domain.CloudContainerProxyRequest;

/**
 * 云机容器资源池与用户端云机操作服务。
 */
public interface ICloudContainerService
{
    /** 查询容器明细列表。 */
    List<CloudContainer> selectCloudContainerList(CloudContainer container);

    /** 查询按主机实例位聚合后的容器资源池列表。 */
    List<CloudContainer> selectCloudContainerSlotList(CloudContainer container);

    /** 查询容器详情。 */
    CloudContainer selectCloudContainerById(Long containerId);

    /** 查询当前用户名下拥有的云机列表。 */
    List<CloudContainer> selectMyCloudContainerList(Long userId, CloudContainer container);

    /** 查询当前用户云机统计信息。 */
    Map<String, Object> selectMyCloudContainerSummary(Long userId);

    /** 获取当前用户指定云机的 WebRTC 控制地址。 */
    Map<String, Object> selectMyContainerWebrtcInfo(Long userId, Long containerId);

    /** 按主机ID和实例位批量分配云机给指定用户。 */
    int assignContainerByHostIndex(CloudContainerAssignRequest request, String operator);

    /** 取消单个容器分配。 */
    int unassignContainer(Long containerId);

    /** 按主机ID和实例位批量取消分配。 */
    int unassignContainerByHostIndex(CloudContainerAssignRequest request);

    /** 用户端绑定业务账号到自己的云机。 */
    int bindMyContainerAccount(Long userId, Long containerId, String boundAccountNo);

    /** 用户端准备云机控制环境并打开 QQ。 */
    Map<String, Object> openMyContainerQq(Long userId, Long containerId);

    /** 用户端上传二维码并通过虚拟摄像头进入 QQ 扫码流程。 */
    Map<String, Object> scanMyContainerQq(Long userId, Long containerId, MultipartFile file, String filePath) throws Exception;

    /** 用户端设置或关闭自己云机的 S5 代理。 */
    CloudContainer setMyContainerS5Proxy(Long userId, Long containerId, CloudContainerProxyRequest request);

    /** 用户端刷新并查询自己云机的 S5 代理状态。 */
    CloudContainer getMyContainerS5ProxyStatus(Long userId, Long containerId);

    /** 主端启动容器。 */
    void startContainer(Long containerId);

    /** 用户端启动自己的云机。 */
    void startMyContainer(Long userId, Long containerId);

    /** 主端停止容器。 */
    void stopContainer(Long containerId);

    /** 用户端停止自己的云机。 */
    void stopMyContainer(Long userId, Long containerId);

    /** 主端重启容器。 */
    void restartContainer(Long containerId);

    /** 用户端重启自己的云机。 */
    void restartMyContainer(Long userId, Long containerId);

    /** 释放单个容器资源，清理调度和账号绑定。 */
    int releaseContainer(Long containerId);
}
