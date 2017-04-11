<?php

namespace RocketMQ\Common;

class System
{
    /**
     * Get java like time millis
     * @return mixed
     */
    public static function currentTimeMillis()
    {
        return microtime(true) * 10000;
    }
}