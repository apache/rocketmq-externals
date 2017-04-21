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

#include "ConsumeType.h"

namespace rmq
{

const char* getConsumeTypeString(ConsumeType type)
{
    switch (type)
    {
        case CONSUME_ACTIVELY:
            return "CONSUME_ACTIVELY";
        case CONSUME_PASSIVELY:
            return "CONSUME_PASSIVELY";
    }

    return "UnknowConsumeType";
}

const char* getConsumeFromWhereString(ConsumeFromWhere type)
{
    switch (type)
    {
        case CONSUME_FROM_LAST_OFFSET:
            return "CONSUME_FROM_LAST_OFFSET";
        case CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST:
            return "CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST";
        case CONSUME_FROM_MAX_OFFSET:
            return "CONSUME_FROM_MAX_OFFSET";
        case CONSUME_FROM_MIN_OFFSET:
            return "CONSUME_FROM_MIN_OFFSET";
        case CONSUME_FROM_FIRST_OFFSET:
            return "CONSUME_FROM_FIRST_OFFSET";
        case CONSUME_FROM_TIMESTAMP:
            return "CONSUME_FROM_TIMESTAMP";
    }

    return "UnknowConsumeFromWhere";
}

const char* getMessageModelString(MessageModel type)
{
    switch (type)
    {
        case CLUSTERING:
            return "CLUSTERING";
        case BROADCASTING:
            return "BROADCASTING";
    }

    return "UnknowMessageModel";
}

}

