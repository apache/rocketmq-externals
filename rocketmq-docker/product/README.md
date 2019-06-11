# Config Reference of RocketMQ Docker in production

## Background

This is a simple instructions for how to use a persistent storage and configuration information in a production environment to deploy a NameServer cluster and a master-slave broker cluster under distributed network nodes. 

Note: Here only contains the configuration and startup Docker containers, without mentioning the container's monitoring and management, the container machine's DNS configuration, message distribution and reliability storage details. This part needs to depend on the advanced deployment capabilities related to RocketMQ-Operator in conjunction with the capabilities of Kubernetes.

## Steps to deploy and run docker containers

1. Determine the IP and DNS information of the host (physical or virtual machine) to be deployed with NameServer or Broker, the storage file location in the hosted node, and ensure that the relevant ports (9876, 10911, 10912, 10909) are not occupied.
2. Prepare the configuration file used by the broker, select the reference configuration file according to the requirements of the Cluster, and modify the necessary attribute parameters in the file.
3. Call the docker container startup script, set the docker parameters, and start the container (look for the RocketMQ image version from [here]())
4. Verify the container startup status

## Directory Structure

product /

​    | -  conf /   (Several typical cluster configuration references )

​    | - start-ns.sh (Shell script for starting a name-server container, which is called once for each name-server container on different node)

​    | - start-broker.sh (Shell script for starting a broker container, which is called once for creating different broker cluster member on different node)

   | - README.md 

   | - README_cn.md

## Use Case 

How to config a 2m-2s-async cluster in Docker style.

### Startup nameserver cluster 

Note: You can skip this step if you use an existing nameserver cluster

1. Confirm the host machine where the nameserver is to be deployed and copy the product directory into the host. Determine the directory (DATA_HOME) where the container persistences content (logs/storage) on the host,  as well as the RocketMQ image version (ROCKETMQ_VERSION)

2. Run the script start-ns.sh, for example:

   ```
   sh start-ns.sh /home/nameserver/data 4.5.0
   ```

3. Repeat above steps if there are multiple nameservers in the cluster.

### Startup broker cluster

1. Confirm the NameServer Cluster address. (fomart e.g. "ns1:9876;ns2:9876;...")

2. Confirm the host machine where the broker-a master is to be deployed，determine the directory (DATA_HOME) where the container persistences content (logs/storage) on the host, e.g. DATA_HOME is set as /home/broker/data/;  then you need to copy the reference config file conf/2m-2s-async/broker-a.properties as /home/broker/data/conf/2m-2s-async/broker-a.properties in the  host.

   Change file broker-a.properties and make the property 'brokerIP1' value as the dns-hostname(Precautions #3) of the host.

3. Confirm the ROCKETMQ_VERSION (e.g. 4.5.0)， start broker with shell script start-broker.sh through  the following command:

   ```
   sh start-broker.sh /home/broker/data 4.5.0 "ns1:9876;ns2:9876" conf/2m-2s-async/broker-a.properties
   ```

4. Check if the broker container is start up correctly (Note：The dir DATA_HOME in host needs to open read/write permissions  for the rocketmq user in the container, Precautions #1)

5. Confirm the host machine where the broker-a slave is to be deployed，determine the directory (DATA_HOME) where the container persistences content (logs/storage) on the host, e.g. DATA_HOME is set as /home/broker/data/;  then you need to copy the reference config file conf/2m-2s-async/broker-a-s.properties as /home/broker/data/conf/2m-2s-async/broker-a-s.properties in the  host.

   Change file broker-a-s.properties and the proeprty 'brokerIP1' valueas the dns-hostname of the host.

6. Confirm the ROCKETMQ_VERSION，start slave broker with shell script start-broker.sh:

   ```
   sh start-broker.sh /home/broker/data 4.5.0 "ns1:9876;ns2:9876" conf/2m-2s-async/broker-a-s.properties
   ```

7. Check if the broker container is start up correctly.

8. Repeat above steps to create master and slave broker docker conatiners.

## Precautions

1. Ensure the DATA_HOME directory r/w permissions

   The broker container needs to write data that needs to be persisted in the DATA_HOME directory of the host, these data include operation logs and message storage files. It is required to open the permissions in the DATA_HOME directory to ensure that the relevant files can be written when the broker is started and running. 
      A case: After starting the broker, the broker automatically quits after a period of time, without any log writes, this may be due to the container does not write DATA_HOME / logs directory permissions.

2. Declare the external map port in the script (start-broker.sh, start-ns.sh)
   The default mapping ports have been defined in the relevant script. If the user has special requirements (such as a port is already occupied by other applications), you need to modify the shell script to define a new port mapping.

3. Recommended to use DNS to configure the broker and name-server address.

   The broker running in the docker container uses the property brokerIP1 to specify the address of the host it is on, and register/publish this address in the NameServer so that the RocketMQ client can obtain externally available broker addresses through the NameServer. When specifying the brokerIP1 property value, a good practice is to use dns- Hostname (instead of the direct IP address), so that when a large-scale broker changes or ip address migration, it will not affect the deployed containers.