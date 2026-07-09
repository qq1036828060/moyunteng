alter table cloud_sdk_call_log
  modify column request_body longtext comment '请求摘要',
  modify column response_body longtext comment '响应摘要';
