                                                                                                                                                                                                                            # Support AMQP protocol for RocketMQ

### AMQP
  AMQP is a wire-level protocol for asynchronous messaging. Every byte of transmitted data is specified. So it has a truly interoperable cross-platform messaging standard.  AMQP is often compared to JMS but it has the message formatting unlike JMS. AMQP publishes its specification in a downloadable XML format.

### Design
Since the AMQP is a wire-level protocol, it’s difficult to support it in RocketMQ server directly. So that I have come up with an idea to implement an AMQP proxy server and react with the RocketMQ cluster using RocketMQ client. 
Normally AMQP works as following.
