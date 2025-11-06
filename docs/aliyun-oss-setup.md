# 阿里云OSS配置指南

本项目支持使用阿里云对象存储OSS来存储用户上传的头像和媒体文件。

## 配置步骤

### 1. 创建阿里云OSS Bucket

1. 登录[阿里云控制台](https://oss.console.aliyun.com/)
2. 创建一个新的Bucket
3. 记录Bucket名称和地域节点（Endpoint）

### 2. 获取访问密钥

1. 在阿里云控制台中创建AccessKey
2. 记录AccessKey ID和AccessKey Secret

### 3. 配置环境变量

在项目根目录的`.env`文件中添加以下配置：

```bash
# 阿里云OSS配置
ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_ACCESS_KEY_ID=your_access_key_id
ALIYUN_OSS_ACCESS_KEY_SECRET=your_access_key_secret
ALIYUN_OSS_BUCKET_NAME=your_bucket_name
ALIYUN_OSS_DOMAIN=https://your_custom_domain.com  # 可选，自定义域名
```

### 4. 配置说明

- `ALIYUN_OSS_ENDPOINT`: OSS地域节点，例如：`https://oss-cn-hangzhou.aliyuncs.com`
- `ALIYUN_OSS_ACCESS_KEY_ID`: 阿里云AccessKey ID
- `ALIYUN_OSS_ACCESS_KEY_SECRET`: 阿里云AccessKey Secret
- `ALIYUN_OSS_BUCKET_NAME`: OSS存储桶名称
- `ALIYUN_OSS_DOMAIN`: 自定义域名（可选），如果不配置将使用OSS默认域名

### 5. 回退机制

如果未配置阿里云OSS相关环境变量，系统将自动回退到本地文件存储。

## 功能特性

- 自动文件类型检测和验证
- 支持图片和视频文件上传
- 文件大小限制（最大50MB）
- 唯一文件名生成（时间戳+随机字符串）
- 错误处理和日志记录

## 安全注意事项

1. 请妥善保管AccessKey信息，不要提交到代码仓库
2. 建议为OSS配置适当的访问权限策略
3. 如果使用自定义域名，请确保SSL证书配置正确

## 故障排除

1. 检查环境变量是否正确配置
2. 确认OSS Bucket是否存在且有写入权限
3. 检查网络连接和防火墙设置
4. 查看应用日志中的详细错误信息