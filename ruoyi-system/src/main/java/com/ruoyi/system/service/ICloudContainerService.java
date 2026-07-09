package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerAssignRequest;

public interface ICloudContainerService
{
    List<CloudContainer> selectCloudContainerList(CloudContainer container);

    List<CloudContainer> selectCloudContainerSlotList(CloudContainer container);

    CloudContainer selectCloudContainerById(Long containerId);

    List<CloudContainer> selectMyCloudContainerList(Long userId, CloudContainer container);

    Map<String, Object> selectMyCloudContainerSummary(Long userId);

    Map<String, Object> selectMyContainerWebrtcInfo(Long userId, Long containerId);

    int assignContainerByHostIndex(CloudContainerAssignRequest request, String operator);

    int unassignContainer(Long containerId);

    int unassignContainerByHostIndex(CloudContainerAssignRequest request);

    int bindMyContainerAccount(Long userId, Long containerId, String boundAccountNo);

    Map<String, Object> openMyContainerQq(Long userId, Long containerId);

    Map<String, Object> scanMyContainerQq(Long userId, Long containerId, MultipartFile file, String filePath) throws Exception;

    void startContainer(Long containerId);

    void startMyContainer(Long userId, Long containerId);

    void stopContainer(Long containerId);

    void stopMyContainer(Long userId, Long containerId);

    void restartContainer(Long containerId);

    void restartMyContainer(Long userId, Long containerId);

    int releaseContainer(Long containerId);
}
