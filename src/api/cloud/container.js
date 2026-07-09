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

export function assignContainer(data) {
  return request({
    url: '/cloud/container/assign',
    method: 'post',
    data: data
  })
}

export function unassignContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId + '/unassign',
    method: 'post'
  })
}

export function unassignContainerSlot(data) {
  return request({
    url: '/cloud/container/unassign-slot',
    method: 'post',
    data: data
  })
}

export function releaseContainer(containerId) {
  return request({
    url: '/cloud/container/' + containerId + '/release',
    method: 'post'
  })
}
