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
namespace RocketMQ\Common\Message;

class Message
{

    private $serialVersionUID = 8445773977080406428;
    private $topic;
    private $flag;
    private $properties;
    private $body;

    public function __construct(...$args)
    {
        $c = count($args);
        switch ($c) {
            case 0:
                break;
            case 2:
                $this->initMessage($args[0], "", "", 0, $args[1], true);
                break;
            case 3:
                $this->initMessage($args[0], $args[1], "", 0, $args[2], true);
                break;
            case 4:
                $this->initMessage($args[0], $args[1], $args[2], 0, $args[3], true);
                break;

        }

    }

    public function initMessage($topic, $tags, $keys = '', $flag = 0, $body, $waitStoreMsgOK)
    {
        $this->topic = $topic;
        $this->flag = $flag;
        $this->body = $body;
        if (null !== $tag && strlen($tags) > 0) {
            $this->setTags(tags);
        }
        if (null !== $keys && strlen($keys) > 0) {
            $this->setKeys($keys);
        }
        $this->setWaitStoreMsgOk($waitStoreMsgOK);
    }

    public function putProperty($name, $value)
    {
        if (null === $this->properties) {
            $this->properties = [];
        }
        $this->properties[$name] = $value;
    }

    public function clearProperty($name)
    {
        if (null === $this->properties) {
            unset($this->properties[$name]);
        }
    }

    public function putUserProperty($name, $value)
    {
        //TODO
    }

    public function getUserProperty($name)
    {
        return $this->getProperty($name);
    }

    public function getProperty($name)
    {
        if (null === $this->properties) {
            $this->properties = [];
        }
        return (array_key_exists($name, $this->properties)) ? $this->properties[$name] : null;
    }

    public function gtTopic()
    {
        return $this->topic;
    }

    public function setTopic($topic)
    {
        $this->topic = $topic;
    }

    public function getTags()
    {
        return $this->getProperty(MessageConst::PROPERTY_TAGS);
    }

    public function setTags($tags)
    {
        $this->putProperty(MessageConst::PROPERTY_TAGS, $tags);
    }

    public function setKeys($keys)
    {
        $keys = implode(MessageConst::KEY_SEPARATOR, $keys);
        $this->putProperty(MessageConst::PROPERTY_KEYS, $keys);
    }

    public function getKeys()
    {
        return $this->getProperty(MessageConst::PROPERTY_KEYS);
    }

    public function setWaitStroreMsgOk($waitStoreMsgOK)
    {

    }

    public function getDelayTimeLevel()
    {
        $t = $this->getProperty(MessageConst . PROPERTY_DELAY_TIME_LEVEL);
        if ($t !== null) {
            return (int)$t;
        }

        return 0;
    }

    public function setDelayTimeLevel($level)
    {
        $this->putProperty(MessageConst::PROPERTY_DELAY_TIME_LEVEL, $level);
    }

    public function isWaitStoreMsgOK()
    {
        $result = $this->getProperty(MessageConst::PROPERTY_WAIT_STORE_MSG_OK);
        if (null === $result) {
            return true;
        }

        return (bool)$result;
    }

    public function setWaitStoreMsgOK($waitStoreMsgOK)
    {
        $this->putProperty(MessageConst::PROPERTY_WAIT_STORE_MSG_OK, $waitStoreMsgOK);
    }

    public function getFlag()
    {
        return $this->flag;
    }

    public function setFlag($flag)
    {
        $this->flag = $flag;
    }

    public function getBody()
    {
        return $this->body;
    }

    public function setBody($body)
    {
        $this->body = $body;
    }

    public function getProperties()
    {
        return $this->properties;
    }

    public function setProperties($properties)
    {
        $this->properties = $properties;
    }

    public function getBuyerId()
    {
        return $this->getProperty(MessageConst::PROPERTY_BUYER_ID);
    }

    public function setBuyerId($buyerId)
    {
        $this->putProperty(MessageConst::PROPERTY_BUYER_ID, $buyerId);
    }


    public function toString()
    {
        return "Message [topic=" . $this->topic . ", flag=" . $this->flag . ", properties=" . $this->properties . ", body="
            . ($this->body != null ? strlen($this->body) : 0) . "]";
    }
}