DATA_HOME=${1:-/home/admin/data}
ROCKETMQ_VERSION=${2:-latest}

echo "DATA_HOME=${DATA_HOME} ROCKETMQ_VERSION=${ROCKETMQ_VERSION}"

# Start nameserver
docker run -d -v ${DATA_HOME}/namesrv/logs:/home/rocketmq/logs --name rmqnamesrv rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqnamesrv

