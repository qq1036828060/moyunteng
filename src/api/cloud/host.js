import request from '@/utils/request'

export function listHost(query) {
  return request({
    url: '/cloud/host/list',
    method: 'get',
    params: query
  })
}

export function getHost(hostId) {
  return request({
    url: '/cloud/host/' + hostId,
    method: 'get'
  })
}

export function addHost(data) {
  return request({
    url: '/cloud/host',
    method: 'post',
    data
  })
}

export function updateHost(data) {
  return request({
    url: '/cloud/host',
    method: 'put',
    data
  })
}

export function delHost(hostIds) {
  return request({
    url: '/cloud/host/' + hostIds,
    method: 'delete'
  })
}

export function testHost(hostId) {
  return request({
    url: '/cloud/host/' + hostId + '/test',
    method: 'post'
  })
}

export function syncContainers(hostId) {
  return request({
    url: '/cloud/host/' + hostId + '/syncContainers',
    method: 'post'
  })
}

