import request from '@/utils/request'

export function listContainer(query) {
  return request({
    url: '/cloud/container/list',
    method: 'get',
    params: query
  })
}

export function getContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId,
    method: 'get'
  })
}

export function startContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId + '/start',
    method: 'post'
  })
}

export function stopContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId + '/stop',
    method: 'post'
  })
}

export function restartContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId + '/restart',
    method: 'post'
  })
}

export function releaseContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId + '/release',
    method: 'post'
  })
}

