import request from '@/utils/request'

export function listMyContainer(query) {
  return request({
    url: '/cloud/my/container/list',
    method: 'get',
    params: query
  })
}

export function getMyContainerSummary() {
  return request({
    url: '/cloud/my/container/summary',
    method: 'get'
  })
}

export function getMyContainerWebrtc(containerId) {
  return request({
    url: '/cloud/my/container/' + containerId + '/webrtc',
    method: 'get'
  })
}

export function startMyContainer(containerId) {
  return request({
    url: '/cloud/my/container/' + containerId + '/start',
    method: 'post'
  })
}

export function stopMyContainer(containerId) {
  return request({
    url: '/cloud/my/container/' + containerId + '/stop',
    method: 'post'
  })
}

export function restartMyContainer(containerId) {
  return request({
    url: '/cloud/my/container/' + containerId + '/restart',
    method: 'post'
  })
}

export function bindMyContainerAccount(containerId, boundAccountNo) {
  return request({
    url: '/cloud/my/container/' + containerId + '/account',
    method: 'put',
    data: { boundAccountNo }
  })
}

export function openMyContainerQq(containerId) {
  return request({
    url: '/cloud/my/container/' + containerId + '/qq/open',
    method: 'post'
  })
}

export function scanMyContainerQq(containerId, file, filePath) {
  const rawFile = file.raw || file
  const data = new FormData()
  data.append('file', rawFile, rawFile.name)
  if (filePath) {
    data.append('filePath', filePath)
  }
  return request({
    url: '/cloud/my/container/' + containerId + '/qq/scan',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    transformRequest: [(formData) => formData],
    data
  })
}
