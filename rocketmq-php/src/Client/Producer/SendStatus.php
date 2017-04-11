<?php

namespace RocketMQ\Client\Producer;

class SendStatus
{
    const SEND_OK = 0;
    const FLUSH_DISK_TIMEOUT = 1;
    const FLUSH_SLAVE_TIMEOUT = 2;
    const SLAVE_NOT_AVAILABLE = 3;
}