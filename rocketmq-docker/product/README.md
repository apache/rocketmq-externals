# 生产级可用的RocketMQ Docker部署

## 背景

这是一个简单的使用说明，旨在说明如何在生产环境中使用可持久的存储和配置信息，在不同的网络节点下部署NameServer集群和主从模式的Broker集群。注意：这里只包含配置及启动容器，没有提及容器的监控和管理，容器机器的DNS配置，消息的分布和可靠性存储等细节。这一部分需要结合Kubernetes的功能实现RocketMQ-Operator相关的高级部署功能。

## 部署和运行容器的步骤

1. 确定要部署的宿主机(物理或虚拟机)的IP和DNS信息，宿主机的存储文件路径，确保相关的端口(9876, 10911, 10912, 10909)未被占用
2. 准备broker启动的配置文件，根据Cluster的要求，选择参考的配置文件，并对必要属性参数进行修改
3. 调用docker容器启动脚本，设置参数，启动容器 （从[这里](<https://hub.docker.com/apache/rocketmq>)查找RocketMQ镜像版本）
4. 验证容器启动状态

## 目录结构

product /

​    | -  conf / 几种典型的集群的配置 

​    | - start-ns.sh 启动name-server容器的脚本，每个name-server容器调用一次该脚本

​    | - start-broker.sh 启动broker容器的脚本，在不同的容器宿主机上执行该脚本，创建不同的broker集群成员

   | - REAMD.md 说明文件

## 一个具体的使用案例

配置一个2m-2s-async的集群

### 启动nameserver集群 

注意：可以跳过此步骤，如果使用已经存在的nameserver集群

1. 确认要部署nameserver的宿主机，确定需要映射容器持久化的目录(DATA_HOME)和RocketMQ镜像版本(ROCKETMQ_VERSION)

2. 运行脚本 start-ns.sh, 例如：

   ```
   sh start-ns.sh /home/nameserver/data 4.5.0
   ```

3. 如果有多个nameserver要启动，重复上述步骤。

### 启动broker集群

1. 确定要连接的NameServer集群的地址

2. 确认要部署broker-a master的宿主机，确定需要映射容器持久化的目录(DATA_HOME)，例如，确定宿主机的DATA_HOME目录为 /home/broker/data/; 则需要把参考的conf/2m-2s-async/broker-a.properties 文件复制到 /home/broker/data/conf/2m-2s-async/broker-a.properties

   修改broker-a.properties文件的brokerIP1配置为宿主机的dns-hostname(注 #3)

3. 确定ROCKETMQ_VERSION， 运行脚本 start-broker.sh, 例如：

   ```
   sh start-broker.sh /home/broker/data 4.5.0 "ns1:9876;ns2:9876" conf/2m-2s-async/broker-a.properties
   ```

4. 确定broker容器正确启动 (注意： 宿主机目录DATA_HOME要对容器rocketmq用户开放读取权限)

5. 确认要部署broker-a slave的宿主机，确定需要映射容器持久化的目录(DATA_HOME)，例如，确定宿主机的DATA_HOME目录为 /home/broker/data/; 则需要把参考的conf/2m-2s-async/broker-a-s.properties 文件复制到 /home/broker/data/conf/2m-2s-async/broker-a-s.properties

   修改broker-a.properties文件的brokerIP1配置为宿主机的dns-hostname, brokerIP2为master所在的宿主机dns-hostname

6. 确定ROCKETMQ_VERSION， 运行脚本 start-broker.sh, 例如：

   ```
   sh start-broker.sh /home/broker/data 4.5.0 "ns1:9876;ns2:9876" conf/2m-2s-async/broker-a-s.properties
   ```

7. 确定broker容器正确启动 (注意： 宿主机目录DATA_HOME要对容器rocketmq用户开放读取权限)

8. 重复上述步骤，创建broker-b的主从broker容器

## 注意事项

1. 保证宿主机存储目录的权限

   由于broker容器需要在宿主机的DATA_HOME目录下要写如需要持久化的数据，如，操作日志和消息存储文件，所以要求开放DATA_HOME目录下的权限，确保broker启动和运行时能够写入相关的文件。

   一个案例： 当启动broker后，一段时间时间后broker自动退出，没有任何日志写入，这个可能就是由于容器没有写入DATA_HOME/logs目录权限导致。

2. 在脚本中(start-broker.sh, start-ns.sh)中声明外部映射端口

   在相关的脚本中已经定义了相关的默认映射端口，如果用户有特别需求(如端口已经被其它应用占用)，则需要修改shell脚本，定义新的端口映射。

3. 建议使用DNS配置broker和name-server地址

   运行在docker容器中的broker使用brokerIP1来指定所在的宿主机的地址，并将这个地址注册到NameServer中，以便RocketMQ客户端通过NameServer取得可以外部可用的broker地址，但是一个好的实践是使用dns-hostname,来定义相关的ip地址，这样在大规模broker进行变化或者ip地址迁移时，不会对已经部署的容器有影响。