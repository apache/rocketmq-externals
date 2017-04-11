<?php

namespace RocketMQ\Common\Help;

class FAQUrl
{
    const APPLY_TOPIC_URL = //
        "http://rocketmq.apache.org/docs/faq/";

    const NAME_SERVER_ADDR_NOT_EXIST_URL = //
        "http://rocketmq.apache.org/docs/faq/";

    const GROUP_NAME_DUPLICATE_URL = //
        "http://rocketmq.apache.org/docs/faq/";

    const CLIENT_PARAMETER_CHECK_URL = //
        "http://rocketmq.apache.org/docs/faq/";

    const SUBSCRIPTION_GROUP_NOT_EXIST = //
        "http://rocketmq.apache.org/docs/faq/";

    const CLIENT_SERVICE_NOT_OK = //
        "http://rocketmq.apache.org/docs/faq/";

    // FAQ: No route info of this topic, TopicABC
    const NO_TOPIC_ROUTE_INFO = //
        "http://rocketmq.apache.org/docs/faq/";

    const LOAD_JSON_EXCEPTION = //
        "http://rocketmq.apache.org/docs/faq/";

    const SAME_GROUP_DIFFERENT_TOPIC = //
        "http://rocketmq.apache.org/docs/faq/";

    const MQLIST_NOT_EXIST = //
        "http://rocketmq.apache.org/docs/faq/";

    const UNEXPECTED_EXCEPTION_URL = //
        "http://rocketmq.apache.org/docs/faq/";

    const SEND_MSG_FAILED = //
        "http://rocketmq.apache.org/docs/faq/";

    const UNKNOWN_HOST_EXCEPTION = //
        "http://rocketmq.apache.org/docs/faq/";

    const TIP_STRING_BEGIN = "\nSee ";
    const TIP_STRING_END = " for further details.";

    public static function suggestTodo($url)
    {
        return static::TIP_STRING_BEGIN . $url . static::TIP_STRING_END;
    }

    public static function attachDefaultURL($errorMessage)
    {
        if ($errorMessage !== null) {
            $index = strpos($errorMessage, static::TIP_STRING_BEGIN);
            if (false === $index) {
                return $errorMessage . "\n" . "For more information, please visit the url, " . static::UNEXPECTED_EXCEPTION_URL;
            }
        }

        return $errorMessage;
    }
}