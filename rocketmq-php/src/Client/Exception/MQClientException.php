<?php

namespace RocketMQ\Client\Exception;

class MQClientException extends \Exception
{

    public function setResponseCode($code)
    {
        $this->responseCode = $code;
        return $this;
    }
}