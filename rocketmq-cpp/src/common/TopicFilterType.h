/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __TOPICFILTERTYPE_H__
#define __TOPICFILTERTYPE_H__

namespace metaq {
//<!***************************************************************************
enum TopicFilterType {
  /**
   * each msg could only have one tag
   */
  SINGLE_TAG,
  /**
   * not support now
   */
  MULTI_TAG
};
//<!***************************************************************************
}  //<!end namespace;
#endif
