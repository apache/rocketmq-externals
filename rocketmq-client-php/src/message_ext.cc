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

#include "message_ext.h"

void registerMessageExt(Php::Namespace &rocketMQNamespace){
    Php::Class<MessageExt> messageExtClass("MessageExt");

    messageExtClass.method<&MessageExt::parseTopicFilterType>("parseTopicFilterType", { Php::ByVal("filterType", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getStoreHostString>("getStoreHostString");

    messageExtClass.method<&MessageExt::getQueueId>("getQueue");
    messageExtClass.method<&MessageExt::setQueueId>("setQueueId", { Php::ByVal("queueId", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getBornTimestamp>("getBornTimestamp");
    messageExtClass.method<&MessageExt::setBornTimestamp>("setBornTimestamp", { Php::ByVal("bornTimestamp", Php::Type::Numeric), });


    messageExtClass.method<&MessageExt::getMsgId>("getMsgId");
    messageExtClass.method<&MessageExt::setMsgId>("setMsgId", { Php::ByVal("msgId", Php::Type::String), });

    messageExtClass.method<&MessageExt::getOffsetMsgId>("getOffsetMsgId");
    messageExtClass.method<&MessageExt::setOffsetMsgId>("setOffsetMsgId", { Php::ByVal("offsetMsgId", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getBodyCRC>("getBodyCRC");
    messageExtClass.method<&MessageExt::setBodyCRC>("setBodyCRC", { Php::ByVal("bodyCRC", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getQueueOffset>("getQueueOffset");
    messageExtClass.method<&MessageExt::setQueueOffset>("setQueueOffset", { Php::ByVal("queueOffset", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getCommitLogOffset>("getCommitLogOffset");
    messageExtClass.method<&MessageExt::setCommitLogOffset>("setCommitLogOffset", { Php::ByVal("physicOffset", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getStoreSize>("getStoreSize");
    messageExtClass.method<&MessageExt::setStoreSize>("setStoreSize", { Php::ByVal("storeSize", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::getReconsumeTimes>("getReconsumeTimes");

    messageExtClass.method<&MessageExt::getPreparedTransactionOffset>("getPreparedTransactionOffset");
    messageExtClass.method<&MessageExt::setPreparedTransactionOffset>("setPreparedTransactionOffset", { Php::ByVal("preparedTransactionOffset", Php::Type::Numeric), });

    messageExtClass.method<&MessageExt::toString>("toString");
    messageExtClass.method<&MessageExt::getMessage>("getMessage");
    rocketMQNamespace.add(messageExtClass);
}
