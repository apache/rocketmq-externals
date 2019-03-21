# Architecture

![](images/architecture.png)

As shown in above picture, the modules of the bridge are:
- ConnectionManager: manage the connections between the client and bridge
- Protocol Converter: translate the MQTT protocol packets to Bridge internal Message
- Downstream Module:
    - receive messages from client
    - handle the *SUBSCRIBING* requests
    - push messages to *CLIENTS* according to TOPIC
    - forward messages to other *BRIDGES* if necessary
- Message Store:
    - persistent MESSAGES which needs to be retained
    - query OFFLINE messages for any one client
    - query message by *MessageId*
- Subscription Store:
    - query SUBSCRIBED clients by TOPIC
    - maintain the mapping from *TOPIC* to *CLIENTS*
