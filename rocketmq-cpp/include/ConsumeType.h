/********************************************************************
author: qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __CONSUMETYPE_H__
#define __CONSUMETYPE_H__

namespace metaq {
//<!***************************************************************************
enum ConsumeType {
  CONSUME_ACTIVELY,
  CONSUME_PASSIVELY,
};

//<!***************************************************************************
enum ConsumeFromWhere {
  /**
  *new consumer will consume from end offset of queue, 
  * and then consume from last consumed offset of queue follow-up 
  */
  CONSUME_FROM_LAST_OFFSET,

  // @Deprecated
  CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST,
  // @Deprecated
  CONSUME_FROM_MIN_OFFSET,
  // @Deprecated
  CONSUME_FROM_MAX_OFFSET,
  /**
  *new consumer will consume from first offset of queue, 
  * and then consume from last consumed offset of queue follow-up 
  */
  CONSUME_FROM_FIRST_OFFSET,
  /**
  *new consumer will consume from the queue offset specified by timestamp, 
  * and then consume from last consumed offset of queue follow-up 
  */
  CONSUME_FROM_TIMESTAMP,
};

//<!***************************************************************************
enum MessageModel {
  BROADCASTING,
  CLUSTERING,
};
//<!***************************************************************************
}  //<!end namespace;
#endif
