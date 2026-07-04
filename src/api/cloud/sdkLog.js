import request from '@/utils/request'

export function listSdkLog(query) {
  return request({
    url: '/cloud/sdk-log/list',
    method: 'get',
    params: query
  })
}

