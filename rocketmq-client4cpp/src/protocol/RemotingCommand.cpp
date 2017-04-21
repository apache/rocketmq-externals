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
#include "RemotingCommand.h"

#include <sstream>
#include <string>
#include <stdlib.h>
#include <string.h>
#include <json/json.h>
#include "SocketUtil.h"
#include "CommandCustomHeader.h"
#include "MQVersion.h"

namespace rmq
{

kpr::AtomicInteger RemotingCommand::s_seqNumber = 0;
volatile int RemotingCommand::s_configVersion = MQVersion::s_CurrentVersion;

RemotingCommand::RemotingCommand(int code)
    : m_code(code), m_language("CPP"), m_version(0), m_opaque(s_seqNumber++),
      m_flag(0), m_remark(""), m_pCustomHeader(NULL),
      m_dataLen(0), m_pData(NULL), m_bodyLen(0), m_pBody(NULL), m_releaseBody(false)
{
}

RemotingCommand::RemotingCommand(int code,
                                 const std::string& language,
                                 int version,
                                 int opaque,
                                 int flag,
                                 const std::string& remark,
                                 CommandCustomHeader* pCustomHeader)
    : m_code(code), m_language(language), m_version(version), m_opaque(opaque),
      m_flag(flag), m_remark(remark), m_pCustomHeader(pCustomHeader),
      m_dataLen(0), m_pData(NULL), m_bodyLen(0), m_pBody(NULL), m_releaseBody(false)
{

}

RemotingCommand::~RemotingCommand()
{
	if (m_pData)
	{
    	delete[] m_pData;
    }

    if (m_releaseBody)
    {
        delete[] m_pBody;
        m_bodyLen = 0;
        m_pBody = NULL;
    }

    // TODO: maybe memleak
    if (m_pCustomHeader)
    {
        delete m_pCustomHeader;
        m_pCustomHeader = NULL;
    }
}

void RemotingCommand::encode()
{
    std::string extHeader = "{}";
    if (m_pCustomHeader)
    {
        m_pCustomHeader->encode(extHeader);
    }

    std::stringstream ss;
    ss << "{"
       << CODE_STRING << m_code << ","
       << language_STRING << "\"CPP\","
       << version_STRING << m_version << ","
       << opaque_STRING << m_opaque << ","
       << flag_STRING << m_flag << ","
       << remark_STRING << "\"" << m_remark << "\","
       << extFields_STRING << extHeader
       << "}";

	/* protocol:
	 * | 4        | 4           | headerlen    | bodylen    |
	 * | 1-length | 2-headerlen | 3-headerdata | 4-bodydata |
	 */
    int headLen = ss.str().size();
    m_dataLen = 8 + headLen + m_bodyLen;
    m_pData = new char[m_dataLen];

	//length = len(2 + 3 + 4)
    int tmp = htonl(4 + headLen + m_bodyLen);
    memcpy(m_pData, &tmp, 4);

	//headerlength = len(3)
    tmp = htonl(headLen);
    memcpy(m_pData + 4, &tmp, 4);

    //headerdata
    memcpy(m_pData + 8, ss.str().c_str(), headLen);

    //bodydata
    if (m_pBody)
    {
    	memcpy(m_pData + 8 + headLen, m_pBody, m_bodyLen);
    }

    //RMQ_DEBUG("encode|%s%s", ss.str().c_str(), m_pBody ? std::string(m_pBody, m_bodyLen).c_str() : "");
}

std::string RemotingCommand::toString() const
{
	std::string extHeader;
    if (m_pCustomHeader)
    {
        m_pCustomHeader->encode(extHeader);
    }

    std::stringstream ss;
    ss << "{"
       << CODE_STRING << m_code << ","
       << language_STRING << "\"CPP\","
       << version_STRING << m_version << ","
       << opaque_STRING << m_opaque << ","
       << flag_STRING << m_flag << ","
       << remark_STRING << "\"" << m_remark << "\"";
    if (!extHeader.empty())
    {
        ss << "," << extFields_STRING << extHeader;
    }
    ss << "}";

    if (m_pBody)
    {
        ss << "|" << m_bodyLen << "|" << std::string(m_pBody, m_bodyLen);
    }

    return ss.str();
}


const char* RemotingCommand::getData()
{
    return m_pData;
}

int RemotingCommand::getDataLen()
{
    return m_dataLen;
}

const char* RemotingCommand::getBody()
{
    return m_pBody;
}

int RemotingCommand::getBodyLen()
{
    return m_bodyLen;
}

void RemotingCommand::setBody(char* pData, int len, bool copy)
{
    m_releaseBody = copy;

    if (copy)
    {
        m_pBody = new char[len];
        m_bodyLen = len;
        memcpy(m_pBody, pData, len);
    }
    else
    {
        m_pBody = pData;
        m_bodyLen = len;
    }
}

RemotingCommand* RemotingCommand::decode(const char* pData, int len)
{
    Json::Reader reader;
    Json::Value object;

    int headLen;
    memcpy(&headLen, pData + 4, 4);
    headLen = ntohl(headLen);

    //RMQ_DEBUG("decode[%d,%d,%d]|%s%s", len, headLen, len - 8 - headLen, std::string(pData + 8, headLen).c_str(),
    //          std::string(pData + 8 + headLen, len - 8 - headLen).c_str());

    if (!reader.parse(pData + 8, pData + 8 + headLen, object))
    {
        RMQ_ERROR("parse header fail, %s", std::string(pData + 8, headLen).c_str());
        return NULL;
    }

    int code = object["code"].asInt();
    std::string language = object["language"].asString();
    int version = object["version"].asInt();
    int opaque = object["opaque"].asInt();
    int flag = object["flag"].asInt();

    Json::Value v = object["remark"];
    std::string remark = "";
    if (!v.isNull())
    {
        remark = object["remark"].asString();
    }

    RemotingCommand* cmd = new RemotingCommand(code,
            language,
            version,
            opaque,
            flag,
            remark,
            NULL);

    int bodyLen = len - 8 - headLen;
    if (bodyLen > 0)
    {
        cmd->setBody((char*)(pData + 8 + headLen), bodyLen, true);
    }

    return cmd;
}

CommandCustomHeader* RemotingCommand::makeCustomHeader(int code, const char* pData, int len)
{
    Json::Reader reader;
    Json::Value object;

    int headLen;
    memcpy(&headLen, pData + 4, 4);
    headLen = ntohl(headLen);

    if (!reader.parse(pData + 8, pData + 8 + headLen, object))
    {
        RMQ_ERROR("parse header fail, %s", std::string(pData + 8, headLen).c_str());
        return NULL;
    }

    if (object.isMember("extFields") && object["extFields"].isObject() && object["extFields"].size() > 0)
    {
        CommandCustomHeader* pCustomHeader = CommandCustomHeader::decode(
        	code, object["extFields"], isResponseType());
        if (pCustomHeader == NULL)
        {
        	RMQ_WARN("invalid extFields, %d, %s", code, std::string(pData + 8, headLen).c_str());
        }

        setCommandCustomHeader(pCustomHeader);
        return pCustomHeader;
    }

    return NULL;
}


RemotingCommand* RemotingCommand::createRequestCommand(int code, CommandCustomHeader* pCustomHeader)
{
    RemotingCommand* cmd = new RemotingCommand(code);
    cmd->setCommandCustomHeader(pCustomHeader);
    setCmdVersion(cmd);

    return cmd;
}

RemotingCommand* RemotingCommand::createResponseCommand(int code, const std::string& remark)
{
	return createResponseCommand(code, remark, NULL);
}


RemotingCommand* RemotingCommand::createResponseCommand(int code, const std::string& remark,
	CommandCustomHeader* pCustomHeader)
{
    RemotingCommand* cmd = new RemotingCommand(code);
    cmd->markResponseType();
    cmd->setRemark(remark);
    setCmdVersion(cmd);

    if (pCustomHeader)
    {
    	cmd->setCommandCustomHeader(pCustomHeader);
    }

    return cmd;
}


void RemotingCommand::markResponseType()
{
    int bits = 1 << RPC_TYPE;
    m_flag |= bits;
}

bool RemotingCommand::isResponseType()
{
    int bits = 1 << RPC_TYPE;
    return (m_flag & bits) == bits;
}

void RemotingCommand::markOnewayRPC()
{
    int bits = 1 << RPC_ONEWAY;
    m_flag |= bits;
}

bool RemotingCommand::isOnewayRPC()
{
    int bits = 1 << RPC_ONEWAY;
    return (m_flag & bits) == bits;
}

void RemotingCommand::setCmdVersion(RemotingCommand* pCmd)
{
    if (s_configVersion >= 0)
    {
        pCmd->setVersion(s_configVersion);
    }
    else
    {
        int value = MQVersion::s_CurrentVersion;
        pCmd->setVersion(value);
        s_configVersion = value;
    }
}

int RemotingCommand::getCode()
{
    return m_code;
}

void RemotingCommand::setCode(int code)
{
    m_code = code;
}

std::string RemotingCommand::getLanguage()
{
    return m_language;
}

void RemotingCommand::setLanguage(const std::string& language)
{
    m_language = language;
}

int RemotingCommand::getVersion()
{
    return m_version;
}

void RemotingCommand::setVersion(int version)
{
    m_version = version;
}

int RemotingCommand::getOpaque()
{
    return m_opaque;
}

void RemotingCommand::setOpaque(int opaque)
{
    m_opaque = opaque;
}

int RemotingCommand::getFlag()
{
    return m_flag;
}

void RemotingCommand::setFlag(int flag)
{
    m_flag = flag;
}

std::string RemotingCommand::getRemark()
{
    return m_remark;
}

void RemotingCommand::setRemark(const std::string& remark)
{
    m_remark = remark;
}

void RemotingCommand::setCommandCustomHeader(CommandCustomHeader* pCommandCustomHeader)
{
    m_pCustomHeader = pCommandCustomHeader;
}

CommandCustomHeader* RemotingCommand::getCommandCustomHeader()
{
    return m_pCustomHeader;
}

RemotingCommandType RemotingCommand::getType()
{
    if (isResponseType())
    {
        return RESPONSE_COMMAND;
    }

    return REQUEST_COMMAND;
}

}
