##### ActiveConnector fully-qualified name
org.apache.rocketmq.connect.rabbitmq.connector.RabbitmqSourceConnector


##### parameter configuration

parameter | effect | required |default
---|--- |--- | ---
rabbtimq.url | The URL of the RabbtiMQ broker | yes | null
rabbtimq.username | The username to use when connecting to RabbtiMQ | yes |  null
rabbtimq.password|  The password to use when connecting to RabbtiMQ    | yes  | null
jms.destination.name | The name of the JMS destination (queue or topic) to read from   |  yes | null
jms.destination.type | The type of JMS destination, which is either queue or topic | yes | null
jms.message.selector | The message selector that should be applied to messages in the destination    |  no  | null 
jms.session.acknowledge.mode | The acknowledgement mode for the JMS Session  | null | Session.AUTO_ACKNOWLEDGE
jms.session.transacted | Flag to determine if the session is transacted and the session completely controls. the message delivery by either committing or rolling back the session      | null | false

