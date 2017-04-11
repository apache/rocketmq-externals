<?php

namespace RocketMQ\Client\Common;

class ClientErrorCode
{
    const CONNECT_BROKER_EXCEPTION = 10001;
    const ACCESSS_BROKER_TIMEOUT = 10002;
    const BROKER_NOT_EXIST_EXCEPTION = 10003;
    const  NO_NAME_SERVER_EXCEPTION = 10004;
    const NOT_FOUND_TOPIC_EXCEPTION = 10005;
}