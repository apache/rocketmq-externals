Rocketmq Playbook provides the Rocketmq cluster deployment and Rocketmq exporter deployment function.

Rocketmq Playbook integrates deployment environment initialization, source pack download, operating system parameter tuning, broker optimal configuration parameters, Rocketmq cluster deployment, Rocketmq deployment, Rocketmq export access, and startup.

The Rocketmq Playbook can be embedded in CI/CD processes or choreographed into Terraform processes, making it important for automated operations or VDC one-click deployment (SDE).

## instructions
The Rocketmq Playbook entry file is rocketmq.yml.

Rocketmq.yml contains namesrv.yml, broker.yml, and exporter.

Rocketmq.yml can be executed separately by deploying a full RocketMQ cluster or three child Playbooks.

The hosts file is configured with a list of machines and variables for Namesrv, Broker, and exporter deployment. 

The hosts file can be used as a variable.


rocketmq-ansible

│

│  broker.yml   #Deploy the broker

│  exporter.yml   #Deploy the exporter

│  hosts   #Deploy machine list and variables

│  namesrv.yml   #Deploy the namesrv

│  rocketmq.yml   #Playbook entry file

│  

├─roles

│  ├─broker

│  │  ├─tasks

│  │  │      main.yml   #Deploy the Broker process

│  │  │      

│  │  ├─templates

│  │  │      broker.conf.j2   #Broker best configuration template

│  │  │      logback_broker.xml.j2   #Broker Logback configuration template

│  │  │      mqbroker.service    #Self boot automatic script template

│  │  │      

│  │  └─vars

│  │          main.yml   #The variable used by broker.yml

│  │          

│  ├─exporter

│  │  ├─files

│  │  │      mqexportershutdown.sh   #Exporter stop script

│  │  │      

│  │  ├─tasks

│  │  │      main.yml    #Export Deployment Process

│  │  │      

│  │  ├─templates

│  │  │      mqexporter.service   #Self boot automatic script template

│  │  │      mqexporter.sh.j2    #Exporter startup script template

│  │  │      

│  │  └─vars

│  │          main.yml   #A variable used by exporter.yml

│  │          

│  └─namesrv

│      ├─tasks

│      │      main.yml   #Namesrv deployment process

│      │      

│      ├─templates

│      │      logback_namesrv.xml.j2   #Namesrv logback configuration template

│      │      mqnamesrv.service   #Self boot automatic script template

│      │      

│      └─vars

│              main.yml   #The variable used by namesrv.yml

│              

└─vars

        main.yml   #The variable used by rocketmq.yml


## rocketmq.yml
rocketmq. yml describes how to deploy as Linux root user, perform some deployment environment initialization tasks, and create application file directories and data file directories before executing three sub-Playbooks.

## namesrv.yml
namesrv.yml describes the process of deploying namesrv. 

This includes creating a deployment directory, downloading rocketMQ compilation code, modifying the log file directory, adding a startup mechanism, and starting processes.

## broker.yml
broker.yml describes the process of deploying the broker. 

This includes creating a deployment directory, downloading RocketMQ compiled code, modifying log file directories, optimizing operating system parameters, optimizing broker configuration, adding a startup mechanism, and starting processes.

## exporter.yml
exporter.yml describes the process of deploying RocketMQ exporter. 

It contains tasks such as creating a deployment directory, downloading RocketMQ exporter compile code, generating startup and stop scripts, adding a self-boot mechanism, and starting processes.

