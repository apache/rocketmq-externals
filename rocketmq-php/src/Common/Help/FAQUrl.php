<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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