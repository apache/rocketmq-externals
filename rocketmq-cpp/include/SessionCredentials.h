
#ifndef __SESSIONCREDENTIALS_H__
#define __SESSIONCREDENTIALS_H__

#include "RocketMQClient.h"

namespace metaq {

class SessionCredentials {
 public:
  static const string AccessKey;
  static const string SecretKey;
  static const string Signature;
  static const string SignatureMethod;
  static const string ONSChannelKey;

  SessionCredentials(string input_accessKey, string input_secretKey,
                     const string& input_authChannel)
      : accessKey(input_accessKey),
        secretKey(input_secretKey),
        authChannel(input_authChannel) {}
  SessionCredentials() : authChannel("ALIYUN") {}
  ~SessionCredentials() {}

  string getAccessKey() const { return accessKey; }

  void setAccessKey(string input_accessKey) { accessKey = input_accessKey; }

  string getSecretKey() const { return secretKey; }

  void setSecretKey(string input_secretKey) { secretKey = input_secretKey; }

  string getSignature() const { return signature; }

  void setSignature(string input_signature) { signature = input_signature; }

  string getSignatureMethod() const { return signatureMethod; }

  void setSignatureMethod(string input_signatureMethod) {
    signatureMethod = input_signatureMethod;
  }

  string getAuthChannel() const { return authChannel; }

  void setAuthChannel(string input_channel) { authChannel = input_channel; }

  bool isValid() const {
    if (accessKey.empty() || secretKey.empty() || authChannel.empty())
      return false;

    return true;
  }

 private:
  string accessKey;
  string secretKey;
  string signature;
  string signatureMethod;
  string authChannel;
};
}
#endif
