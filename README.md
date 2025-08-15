# 实时风险交易检测系统Demo

一个基于AWS EKS 实时风险交易检测系统。

## 架构概览

系统主要组件：
- **风险交易检测服务**：基于Spring Boot的Java应用，通过消息队列消息，匹配检测规则，实现欺诈检测逻辑
- **AWS SQS**：接收和缓冲交易消息
- **AWS SNS**：发送欺诈检测警报到邮箱里
- **AWS CloudWatch**：日志和监控
- **Kubernetes (EKS)**：容器编排平台，提供高可用性和弹性

项目代码结构说明：

```plaintext
fraud-detection/
├── src/
│   ├── main/
│   │   ├── java/com/fraud/
│   │   │   ├── FraudDetectionApplication.java       # Service Start Class
│   │   │   ├── config/
│   │   │   │   ├── MapperConfig.java                # Object serialization Bean
│   │   │   │   ├── AwsConfig.java                   # AWS Client Bean
│   │   │   ├── model/
│   │   │   │   ├── Transaction.java                 # Transaction Data Model
│   │   │   │   └── FraudResult.java                 # Fraud Alarm Result
│   │   │   ├── service/
│   │   │   │   ├── FraudDetectionService.java       # Fraud Detect Service
│   │   │   │   ├── SnsNotificationService.java      # SNS Notification Service
│   │   │   │   └── SqsConsumerService.java          # SQS Consumer Service
│   │   └── resources/
│   │       ├── application.properties               # Spring Boot Application Properties
│   │       └── logback-spring.xml               # Logconfig(Including log to CloudWatch)
│   └── test/
│       └── java/com/fraud/
│           ├── service/
│           │   ├── ConsumerServiceTest.java         # SqsConsumerService ut
│           │   └── FraudDetectionServiceTest.java   # FraudDetectionService（SQS交互）
│           └── resources/
│               └── application-test.properties      # test application config
│               └── logback-test.xml                 # test logback config
├── k8s/
│   ├── deployment.yaml                              # K8s Deployment config
│   ├── service.yaml                                 # K8s Service config
│   ├── hpa.yaml                                     # Horizontal Pod Autoscaler Config
├── pom.xml                                          # Maven pom
├── README.md                                      # Architecture&Deploy&Test Description
├── Integration Test.doc                             # Integration Test Dock

```

主要流程说明:

    A[交易生产者] -->|发送交易数据| B(AWS SQS队列)
    B -->|实时消费| C[Fraud Detection Service（K8s Pod）]
    C -->|规则检测| D{欺诈判断}
    D -->|是| E[生成欺诈告警（CloudWatch Logs）]
    D -->|否| F[正常处理]
    G[K8s HPA] -->|自动扩缩容| C
    H[AWS CloudWatch] -->|监控日志| C


## 功能说明

1. **实时欺诈检测**：
   - 基于规则的检测机制
   - 支持多种欺诈规则（金额阈值、可疑账户、高危地区等）
   - 低延迟处理交易（正常延迟1s）

2. **高可用性**：
   - 部署在Kubernetes集群
   - 自动扩缩容
   - 健康检查和自动恢复

3. **可扩展性**：
   - 水平扩展能力应对交易峰值

## 部署前置条件

- AWS账户

- AWS中有节点可以连接到EKS集群

- AWS CLI已配置

- kubectl已安装

- Docker已安装

- Maven 3.6+ 和 Java 17+


## 部署步骤

### 1. 准备AWS资源
#### 创建SQS队列
aws sqs create-queue --queue-name transaction-queue

#### 创建SNS主题
aws sns create-topic --name fraud-alerts
### 2. 构建应用和Docker镜像
#### 构建应用
mvn clean package -DskipTests

#### 构建Docker镜像
登录ECR 获取临时登录命令
docker build -t fraud-detection-system:1.0.0 .

#### 推送镜像到ECR
**实际使用需要替换 846697434276.dkr.ecr.ap-southeast-1.amazonaws.com/dlf:latest 为自己的AWS 私有镜像仓库地址**
docker tag fraud-detection-system:1.0.0  846697434276.dkr.ecr.ap-southeast-1.amazonaws.com/dlf:latest
docker push 846697434276.dkr.ecr.ap-southeast-1.amazonaws.com/dlf:latest

### 3. 部署到Kubernetes (EKS)
#### 创建命名空间
kubectl create namespace fraud-system

#### 创建配置映射
实际使用需要替换自己SQS和SNS的ARN
kubectl create configmap fraud-config \
  --namespace fraud-system \
  --from-literal=aws.region=ap-southeast-1 \
  --from-literal=aws.sqs.queue.url=https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo  \
  --from-literal=aws.sns.topic.arn=arn:aws:sns:ap-southeast-1:846697434276:fraud-alerts

### 4. 部署应用
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

### 5. 部署是否成功验证

```bash
# 查看Pod状态
kubectl get pods -n fraud-detection

# 查看日志
kubectl logs <pod-name> -n fraud-detection

# 查看HPA状态
kubectl get hpa -n fraud-detection
```

## 测试

### 1. 单元测试
`mvn test`

### 2.模拟发送测试交易

可以使用AWS CLI向SQS队列发送测试交易：

```bash
aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12345",
    "accountId": "ACCT-123",
    "amount": 150629.0,
    "location": "HighRiskCountry1",
    "transactionTime": "2023-06-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-3"
```

也可以用代码中的test-send-queue-msg.sh 发送，queue-url替换成实际值

#### 3. POD查看

- 查看应用日志：`kubectl logs -f <pod-name> -n fraud-system`
- 在AWS CloudWatch中查看日志

## 扩展指南

### 添加新的欺诈检测规则

1. 在`FraudDetectionService`类中添加新的检测方法
2. 在`detectFraud`方法中调用新的检测方法
3. 添加必要的配置参数到`application.properties`
4. 后续考虑对检测规则插件化，减少代码修改

### 调整自动扩缩容配置

修改`k8s/hpa.yaml`文件中的阈值和策略，然后应用更新：
kubectl apply -f k8s/hpa.yaml

## 后续优化点

1. SQS中开启死信队列，需要有定时任务对处理失败的消息单独处理（触发告警，人为分析或者统一经验规则处理）
2. 对于判断有欺诈行为的事务，未来考虑持久化到特定存储中
3. 容器中服务访问SQS遵循最小权限配置，替换当前获取AK/SK方式。
4. AKS 容器节点支持多可用区设置