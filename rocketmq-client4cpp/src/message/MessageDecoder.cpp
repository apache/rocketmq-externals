/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "MessageDecoder.h"

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sstream>
#include "MessageExt.h"
#include "MessageSysFlag.h"
#include "UtilAll.h"

namespace rmq
{

const char MessageDecoder::NAME_VALUE_SEPARATOR = 1;
const char MessageDecoder::PROPERTY_SEPARATOR = 2;
const int MessageDecoder::MSG_ID_LENGTH = 8 + 8;

int MessageDecoder::MessageMagicCodePostion = 4;
int MessageDecoder::MessageFlagPostion = 16;
int MessageDecoder::MessagePhysicOffsetPostion = 28;
int MessageDecoder::MessageStoreTimestampPostion = 56;

std::string MessageDecoder::createMessageId(sockaddr& addr, long long offset)
{
    struct sockaddr_in sa;
    memcpy(&sa, &addr, sizeof(sockaddr));
    sa.sin_family = AF_INET;

    int port = ntohs(sa.sin_port);
    port = htonl(port);
    int ip = sa.sin_addr.s_addr;

    unsigned char* buf = new unsigned char[MSG_ID_LENGTH];
    offset = h2nll(offset);
    memcpy(buf, &ip, 4);
    memcpy(buf + 4, &port, 4);
    memcpy(buf + 8, &offset, 8);

    char* str = new char[2 * MSG_ID_LENGTH + 1];
    memset(str, 0, 2 * MSG_ID_LENGTH + 1);

    for (int i = 0; i < MSG_ID_LENGTH; i++)
    {
        char tmp[3];
        tmp[2] = 0;

        snprintf(tmp, sizeof(tmp), "%02X", buf[i]);
        strncat(str, tmp, sizeof(tmp));
    }

    std::string ret = str;

    delete[] buf;
    delete[] str;

    return ret;
}

MessageId MessageDecoder::decodeMessageId(const std::string& msgId)
{
    std::string ipstr = msgId.substr(0, 8);
    std::string portstr = msgId.substr(8, 8);
    std::string offsetstr = msgId.substr(16);

    char* end;
    int ipint = strtoul(ipstr.c_str(), &end, 16);
    int portint = strtoul(portstr.c_str(), &end, 16);

    long long offset = UtilAll::hexstr2ull(offsetstr.c_str());

    offset = n2hll(offset);

    portint = ntohl(portint);
    short port = portint;

    struct sockaddr_in sa;
    sa.sin_family = AF_INET;
    sa.sin_port = htons(port);
    sa.sin_addr.s_addr = ipint;

    sockaddr addr;
    memcpy(&addr, &sa, sizeof(sockaddr));

    MessageId id(addr, offset);

    return id;
}

MessageExt* MessageDecoder::decode(const char* pData, int len, int& offset)
{
    return decode(pData, len, offset, true);
}

MessageExt* MessageDecoder::decode(const char* pData, int len, int& offset, bool readBody)
{
	MessageExt* msgExt = NULL;

    try
    {
        msgExt = new MessageExt();

        // 1 TOTALSIZE
        int storeSize;
        memcpy(&storeSize, pData, 4);
        storeSize = ntohl(storeSize);

        msgExt->setStoreSize(storeSize);

        // 2 MAGICCODE sizeof(int)

        // 3 BODYCRC
        int bodyCRC;
        memcpy(&bodyCRC, pData + 2 * sizeof(int), 4);
        bodyCRC = ntohl(bodyCRC);
        msgExt->setBodyCRC(bodyCRC);

        // 4 QUEUEID
        int queueId;
        memcpy(&queueId, pData + 3 * sizeof(int), 4);
        queueId = ntohl(queueId);
        msgExt->setQueueId(queueId);

        // 5 FLAG
        int flag ;

        memcpy(&flag, pData + 4 * sizeof(int), 4);
        flag = ntohl(flag);

        msgExt->setFlag(flag);

        // 6 QUEUEOFFSET
        long long queueOffset;
        memcpy(&queueOffset, pData + 5 * sizeof(int), 8);
        queueOffset = n2hll(queueOffset);
        msgExt->setQueueOffset(queueOffset);

        // 7 PHYSICALOFFSET
        long long physicOffset;

        memcpy(&physicOffset, pData + 7 * sizeof(int), 8);
        physicOffset = n2hll(physicOffset);
        msgExt->setCommitLogOffset(physicOffset);

        // 8 SYSFLAG
        int sysFlag;

        memcpy(&sysFlag, pData + 9 * sizeof(int), 4);
        sysFlag = ntohl(sysFlag);
        msgExt->setSysFlag(sysFlag);

        // 9 BORNTIMESTAMP
        long long bornTimeStamp;
        memcpy(&bornTimeStamp, pData + 10 * sizeof(int), 8);
        bornTimeStamp = n2hll(bornTimeStamp);

        msgExt->setBornTimestamp(bornTimeStamp);

        // 10 BORNHOST
        int bornHost;//c0 a8 00 68  192.168.0.104 c0 a8 00 68 00 00 c4 04
        memcpy(&bornHost, pData + 12 * sizeof(int), 4);

        int port;
        memcpy(&port, pData + 13 * sizeof(int), 4);
        port = ntohl(port);

        struct sockaddr_in sa;
        sa.sin_family = AF_INET;
        sa.sin_port = htons(port);
        sa.sin_addr.s_addr = bornHost;

        sockaddr bornAddr;
        memcpy(&bornAddr, &sa, sizeof(sockaddr));
        msgExt->setBornHost(bornAddr);

        // 11 STORETIMESTAMP
        long long storeTimestamp;
        memcpy(&storeTimestamp, pData + 14 * sizeof(int), 8);
        storeTimestamp = n2hll(storeTimestamp);
        msgExt->setStoreTimestamp(storeTimestamp);

        // 12 STOREHOST
        int storeHost;
        memcpy(&storeHost, pData + 16 * sizeof(int), 4);
        memcpy(&port, pData + 17 * sizeof(int), 4);
        port = ntohl(port);

        sa.sin_family = AF_INET;
        sa.sin_port = htons(port);
        sa.sin_addr.s_addr = storeHost;

        sockaddr storeAddr;
        memcpy(&storeAddr, &sa, sizeof(sockaddr));

        msgExt->setStoreHost(storeAddr);

        // 13 RECONSUMETIMES
        int reconsumeTimes;
        memcpy(&reconsumeTimes, pData + 18 * sizeof(int), 4);
        reconsumeTimes = ntohl(reconsumeTimes);
        msgExt->setReconsumeTimes(reconsumeTimes);

        // 14 Prepared Transaction Offset
        long long preparedTransactionOffset;
        memcpy(&preparedTransactionOffset, pData + 19 * sizeof(int), 8);
        preparedTransactionOffset = n2hll(preparedTransactionOffset);
        msgExt->setPreparedTransactionOffset(preparedTransactionOffset);

        // 15 BODY
        int bodyLen = 0;
        memcpy(&bodyLen, pData + 21 * sizeof(int), 4);
        bodyLen = ntohl(bodyLen);

        if (bodyLen > 0)
        {
            if (readBody)
            {
                const char* body = pData + 22 * sizeof(int);
                int newBodyLen = bodyLen;

                // uncompress body
                if ((sysFlag & MessageSysFlag::CompressedFlag) == MessageSysFlag::CompressedFlag)
                {
                    unsigned char* pOut;
                    int outLen;

                    if (UtilAll::decompress(body, bodyLen, &pOut, &outLen))
                    {
                        msgExt->setBody((char*)pOut, outLen);
                        free(pOut);
                    }
                    else
                    {
                        msgExt->setBody(body, newBodyLen);
                    }
                }
                else
                {
                    msgExt->setBody(body, newBodyLen);
                }
            }
            else
            {

            }
        }

        // 16 TOPIC
        int topicLen = *(pData + 22 * sizeof(int) + bodyLen);

        char* tmp = new char[topicLen + 1];

        memcpy(tmp, pData + 22 * sizeof(int) + bodyLen + 1, topicLen);
        tmp[topicLen] = 0;
        std::string topic = tmp;

        delete[] tmp;

        msgExt->setTopic(topic);

        // 17 properties
        short propertiesLength;
        memcpy(&propertiesLength, pData + 22 * sizeof(int) + bodyLen + 1 + topicLen, 2);
        propertiesLength = ntohs(propertiesLength);

        if (propertiesLength > 0)
        {
            char* properties = new char[propertiesLength + 1];
            memcpy(properties, pData + 22 * sizeof(int) + bodyLen + 1 + topicLen + 2, propertiesLength);
            properties[propertiesLength] = 0;
            std::string propertiesString = properties;
            std::map<std::string, std::string> map;
            string2messageProperties(map, propertiesString);
            msgExt->setProperties(map);
            delete[] properties;
        }

        offset = 22 * sizeof(int) + bodyLen + 1 + topicLen + 2 + propertiesLength;

        // ÏûÏ¢ID
        std::string msgId =  createMessageId(storeAddr, physicOffset);
        msgExt->setMsgId(msgId);

        return msgExt;
    }
    catch (...)
    {
    	RMQ_ERROR("decode exception");
		if (msgExt)
		{
			delete msgExt;
			msgExt = NULL;
		}
    }

    return NULL;
}

std::list<MessageExt*> MessageDecoder::decodes(const char* pData, int len)
{
    return decodes(pData, len, true);
}

std::list<MessageExt*> MessageDecoder::decodes(const char* pData, int len, bool readBody)
{
    std::list<MessageExt*> list;

    int offset = 0;
    while (offset < len)
    {
        int tmp;
        MessageExt* msg = decode(pData + offset, len, tmp);
        list.push_back(msg);
        offset += tmp;
    }

    return list;
}

std::string MessageDecoder::messageProperties2String(const std::map<std::string, std::string>& properties)
{
    std::stringstream ss;

    std::map<std::string, std::string>::const_iterator it = properties.begin();

    for (; it != properties.end(); it++)
    {
        ss << it->first << NAME_VALUE_SEPARATOR << it->second << PROPERTY_SEPARATOR;
    }

    return ss.str();
}

void MessageDecoder::string2messageProperties(std::map<std::string, std::string>& properties,
        std::string& propertiesString)
{
    std::vector<std::string> out;
    UtilAll::Split(out, propertiesString, PROPERTY_SEPARATOR);

    for (size_t i = 0; i < out.size(); i++)
    {
        std::vector<std::string> outValue;
        UtilAll::Split(outValue, out[i], NAME_VALUE_SEPARATOR);

        if (outValue.size() == 2)
        {
            properties[outValue[0]] = outValue[1];
        }
    }
}

}
