#!/bin/sh
if [ -z "$ROCKETMQ_CONSOLE_HOME" ] ; then
  ## resolve links - $0 may be a link to maven's home
  PRG="$0"

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG="`dirname "$PRG"`/$link"
    fi
  done

  saveddir=`pwd`

  ROCKETMQ_CONSOLE_HOME=`dirname "$PRG"`/..

  # make it fully qualified
  ROCKETMQ_CONSOLE_HOME=`cd "$ROCKETMQ_CONSOLE_HOME" && pwd`

  cd "$saveddir"
fi

export ROCKETMQ_CONSOLE_HOME

sh ${ROCKETMQ_CONSOLE_HOME}/bin/runserver.sh org.apache.rocketmq.console.App $@

