/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MESSAGEDECODER_H__
#define __MESSAGEDECODER_H__

#include "MQClientException.h"
#include "MQMessageExt.h"
#include "MQMessageId.h"
#include "MemoryInputStream.h"
#include "SocketUtil.h"

namespace metaq {
//<!***************************************************************************
class MQDecoder {
 public:
  static string createMessageId(sockaddr addr, int64 offset);
  static MQMessageId decodeMessageId(const string& msgId);

  //<!½âÎö½á¹û;
  static void decodes(const MemoryBlock* mem, vector<MQMessageExt>& mqvec);

  static void decodes(const MemoryBlock* mem, vector<MQMessageExt>& mqvec,
                      bool readBody);

  static string messageProperties2String(const map<string, string>& properties);
  static void string2messageProperties(const string& propertiesString,
                                       map<string, string>& properties);

 private:
  static MQMessageExt* decode(MemoryInputStream& byteBuffer);
  static MQMessageExt* decode(MemoryInputStream& byteBuffer, bool readBody);

 public:
  static const char NAME_VALUE_SEPARATOR;
  static const char PROPERTY_SEPARATOR;
  static const int MSG_ID_LENGTH;
  static int MessageMagicCodePostion;
  static int MessageFlagPostion;
  static int MessagePhysicOffsetPostion;
  static int MessageStoreTimestampPostion;
};
}  //<!end namespace;

#endif
