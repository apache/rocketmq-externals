##### ActiveConnector fully-qualified name
org.apache.rocketmq.connect.activemq.connector.ActivemqConnector


##### parameter configuration

parameter | effect | required |default
---|--- |--- | ---
activemq.url | activemq ip and port | yes | null
activemq.username | userName | no |  null
activemq.password|  password    | no  | null
jms.destination.name | The name of the queue or topic being read   |  yes | null
jms.destination.type | Read the type：queue or topic | yes | null
jms.message.selector | selector    |  no  | null 
jms.session.acknowledge.mode | Message to confirm  | null | Session.AUTO_ACKNOWLEDGE
jms.session.transacted | Is transacted session      | null | false
