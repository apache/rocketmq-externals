
#ifndef __SESSIONCREDENTIALS_H__
#define __SESSIONCREDENTIALS_H__

#include "RocketMQClient.h"

namespace rocketmq {

class SessionCredentials {
 public:
  static const std::string AccessKey;
  static const std::string SecretKey;
  static const std::string Signature;
  static const std::string SignatureMethod;
  static const std::string ONSChannelKey;

  SessionCredentials(std::string input_accessKey, std::string input_secretKey,
                     const std::string& input_authChannel)
      : accessKey(input_accessKey),
        secretKey(input_secretKey),
        authChannel(input_authChannel) {}
  SessionCredentials() : authChannel("ALIYUN") {}
  ~SessionCredentials() {}

  std::string getAccessKey() const { return accessKey; }

  void setAccessKey(std::string input_accessKey) { accessKey = input_accessKey; }

  std::string getSecretKey() const { return secretKey; }

  void setSecretKey(std::string input_secretKey) { secretKey = input_secretKey; }

  std::string getSignature() const { return signature; }

  void setSignature(std::string input_signature) { signature = input_signature; }

  std::string getSignatureMethod() const { return signatureMethod; }

  void setSignatureMethod(std::string input_signatureMethod) {
    signatureMethod = input_signatureMethod;
  }

  std::string getAuthChannel() const { return authChannel; }

  void setAuthChannel(std::string input_channel) { authChannel = input_channel; }

  bool isValid() const {
    if (accessKey.empty() || secretKey.empty() || authChannel.empty())
      return false;

    return true;
  }

 private:
  std::string accessKey;
  std::string secretKey;
  std::string signature;
  std::string signatureMethod;
  std::string authChannel;
};
}
#endif
