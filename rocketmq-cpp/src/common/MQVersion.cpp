/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "MQVersion.h"

namespace metaq {
int MQVersion::s_CurrentVersion = MQVersion::V3_1_8;

//<!************************************************************************
const char* MQVersion::getVersionDesc(int value) {
  switch (value) {
    // case V1_0_0:
    // return "V1_0_0";
  }
  return "";
}
//<!***************************************************************************
}  //<!end namespace;
