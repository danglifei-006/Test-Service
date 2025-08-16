# 实时欺诈检测系统

一个基于AWS和Kubernetes的实时金融交易欺诈检测系统，能够快速、准确地检测和处理欺诈交易。

## 架构概览

![架构图](https://picsum.photos/id/0/800/400)

系统主要组件：
- **欺诈检测服务**：基于Spring Boot的Java应用，实现欺诈检测逻辑
- **AWS SQS**：接收和缓冲交易消息
- **AWS DynamoDB**：存储交易和欺诈检测结果
- **AWS SNS**：发送欺诈检测警报
- **AWS CloudWatch**：日志和监控
- **Kubernetes (EKS)**：容器编排平台，提供高可用性和弹性

## 功能特点

1. **实时欺诈检测**：
   - 基于规则的检测机制
   - 支持多种欺诈规则（金额阈值、可疑账户、高危地区等）
   - 低延迟处理交易

2. **高可用性**：
   - 部署在Kubernetes集群
   - 自动扩缩容
   - 健康检查和自动恢复

3. **可扩展性**：
   - 模块化设计，易于添加新的欺诈检测规则
   - 水平扩展能力应对交易峰值

## 前置条件

- AWS账户
- AWS CLI已配置
- kubectl已安装
- Docker已安装
- Maven 3.6+ 和 Java 11+

## 部署步骤

### 1. 准备AWS资源
# 创建SQS队列
aws sqs create-queue --queue-name transaction-queue

# 创建DynamoDB表
aws dynamodb create-table \
  --table-name fraud-transactions \
  --key-schema AttributeName=transactionId,KeyType=HASH \
  --attribute-definitions AttributeName=transactionId,AttributeType=S \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

# 创建SNS主题
aws sns create-topic --name fraud-alerts
### 2. 构建应用和Docker镜像
# 构建应用
mvn clean package -DskipTests

# 构建Docker镜像
docker build -t fraud-detection-system:1.0.0 .

# 推送镜像到ECR
登录ECR 获取临时登录命令
docker tag fraud-detection-system:1.0.0  846697434276.dkr.ecr.ap-southeast-1.amazonaws.com/dlf:latest
docker push 846697434276.dkr.ecr.ap-southeast-1.amazonaws.com/dlf:latest
### 3. 部署到Kubernetes (EKS)
# 创建命名空间
kubectl create namespace fraud-system

# 创建配置映射
kubectl create configmap fraud-config \
  --namespace fraud-system \
  --from-literal=aws.region=ap-southeast-1 \
  --from-literal=aws.sqs.queue.url=https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo  \
  --from-literal=aws.dynamodb.table.name=fraud-transactions
  --from-literal=aws.sns.topic.arn=arn:aws:sns:ap-southeast-1:846697434276:fraud-alerts

# 部署应用
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
## 测试

### 单元测试
mvn test
### 集成测试
mvn verify
### 发送测试交易

可以使用AWS CLI向SQS队列发送测试交易：
aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12345",
    "accountId": "ACCT-123",
    "amount": 15000.0,
    "location": "HighRiskCountry1",
    "timestamp": "2023-07-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-1"
  # 新增：指定消息组ID
## 监控

- 查看应用日志：`kubectl logs -f <pod-name> -n fraud-system`
- 在AWS CloudWatch中查看集中式日志
- 查看Kubernetes指标：`kubectl top pods -n fraud-system`

## 扩展指南

### 添加新的欺诈检测规则

1. 在`FraudDetectionService`类中添加新的检测方法
2. 在`detectFraud`方法中调用新的检测方法
3. 添加必要的配置参数到`application.properties`

### 调整自动扩缩容配置

修改`k8s/hpa.yaml`文件中的阈值和策略，然后应用更新：
kubectl apply -f k8s/hpa.yaml