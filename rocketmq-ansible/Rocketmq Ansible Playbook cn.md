Rocketmq playbook提供rocketmq集群部署和rocketmq exporter部署功能。

Rocketmq playbook集成了部署环境初始化、源码包下载、操作系统参数调优、broker最佳配置参数、rocketmq集群部署、rocketmq exporter部署、rocketmq exporter接入、开机自启动等任务编排到一起。

Rocketmq playbook可以嵌入在CI/CD流程中或者编排到terraform流程中，这在自动化运维或者vdc一键部署（SDE）有非常重要的意义。

## 使用说明
## 先决条件

安装ansible。Ansible是一个自动化运维工具，可以进行配置管理和应用部署。实现了批量系统配置、批量程序部署、批量运行命令等功能。

安装文档参考官网

[https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html)

## Playbook结构

Rocketmq playbook入口文件为rocketmq.yml。rocketmq.yml包含namesrv.yml、broker.yml、exporter.yml3个子playbook。rocketmq.yml可以单独执行部署一个完整的rocketmq集群或者3个子playbook单独执行。hosts文件配置了namesrv、broker、exporter部署的机器列表及变量，当使用terraform编排时hosts可以当做变量传递。

rocketmq-ansible

│

│  broker.yml#部署broker

│  exporter.yml#部署exporter

│  hosts#部署机器列表及变量

│  namesrv.yml#部署namesrv

│  rocketmq.yml#playbook入口文件

│  

├─roles

│  ├─broker

│  │  ├─tasks

│  │  │      main.yml   #部署broker流程

│  │  │      

│  │  ├─templates

│  │  │      broker.conf.j2   #broker最佳配置模版

│  │  │      logback_broker.xml.j2   #broker logback配置模版

│  │  │      mqbroker.service    #broker开机自启动脚本模版

│  │  │      

│  │  └─vars

│  │          main.yml   #broker.yml使用的变量

│  │          

│  ├─exporter

│  │  ├─files

│  │  │      mqexportershutdown.sh   #exporter停止脚本

│  │  │      

│  │  ├─tasks

│  │  │      main.yml    #exporter部署流程

│  │  │      

│  │  ├─templates

│  │  │      mqexporter.service   #exporter开机自动脚本模版

│  │  │      mqexporter.sh.j2    #exporter启动脚本模版

│  │  │      

│  │  └─vars

│  │          main.yml   #exporter.yml使用的变量

│  │          

│  └─namesrv

│      ├─tasks

│      │      main.yml   #namesrv部署流程

│      │      

│      ├─templates

│      │      logback_namesrv.xml.j2   #namesrv logback配置模版

│      │      mqnamesrv.service   #namesrv开机自启动脚本模版

│      │      

│      └─vars

│              main.yml   #namesrv.yml使用的变量

│              

└─vars

        main.yml   #rocketmq.yml使用的变量

## Playbook执行

ansible-playbook /path/rocketmq.yml -i /path/hosts

## rocketmq.yml
rocketmq.yml描述了使用linux root用户部署，在执行3个子playbook执行之前做一些部署环境初始化的任务，创建应用文件目录和数据文件目录。

## namesrv.yml
namesrv.yml描述了部署namesrv的过程。包含了创建部署目录、下载rocketmq编译代码、修改日志文件目录、添加开机自启动机制、启动进程等任务。

## broker.yml
broker.yml描述了部署broker的过程。包含了创建部署目录、下载rocketmq编译代码、修改日志文件目录、优化操作系统参数、优化broker配置、添加开机自启动机制、启动进程等任务。

## exporter.yml
exporter.yml描述了部署rocketmq exporter的过程。包含了创建部署目录、下载rocketmq exporter编译代码、生成启动和停止脚本、添加开机自启动机制、启动进程等任务。

