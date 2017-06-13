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

#ifndef __PULLRESULTEXT_H__
#define __PULLRESULTEXT_H__

#include "PullResult.h"

namespace rmq
{

  struct PullResultExt : public PullResult
  {
      PullResultExt(PullStatus pullStatus,
                    long long nextBeginOffset,
                    long long minOffset,
                    long long maxOffset,
                    std::list<MessageExt*>& msgFoundList,
                    long suggestWhichBrokerId,
                    const char* messageBinary,
                    int messageBinaryLen)
          : PullResult(pullStatus,
                       nextBeginOffset,
                       minOffset,
                       maxOffset,
                       msgFoundList),
          suggestWhichBrokerId(suggestWhichBrokerId),
          messageBinary(messageBinary),
          messageBinaryLen(messageBinaryLen)
      {

      }

      long suggestWhichBrokerId;
      const char* messageBinary;
      int messageBinaryLen;
  };
}

#endif
