/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __REMOTINGSERIALIZABLE_H__
#define __REMOTINGSERIALIZABLE_H__
#include "json/json.h"

namespace metaq {
//<!***************************************************************************
class RemotingSerializable {
 public:
  virtual ~RemotingSerializable(){};
  virtual void Encode(std::string& outData) = 0;
};

//<!************************************************************************
}  //<!end namespace;

#endif
