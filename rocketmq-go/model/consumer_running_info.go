package model

import "encoding/json"

type ConsumerRunningInfo struct {
	Properties map[string]string `json:"properties"`
	MqTable    map[MessageQueue]ProcessQueueInfo `json:"mqTable"`
	// todo
	//private TreeSet<SubscriptionData> subscriptionSet = new TreeSet<SubscriptionData>();
	//
	//private TreeMap<MessageQueue, ProcessQueueInfo> mqTable = new TreeMap<MessageQueue, ProcessQueueInfo>();
	//
	//private TreeMap<String/* Topic */, ConsumeStatus> statusTable = new TreeMap<String, ConsumeStatus>();
	//
	//private String jstack;
}

func (self *ConsumerRunningInfo)Encode() (jsonByte []byte, err error) {
	mqTableJsonStr := "{"
	first := true
	var keyJson []byte
	var valueJson []byte

	for key, value := range self.MqTable {
		keyJson, err = json.Marshal(key)
		if err != nil {
			return
		}
		valueJson, err = json.Marshal(value)
		if err != nil {
			return
		}
		if first == false {
			mqTableJsonStr =  mqTableJsonStr +","
		}
		mqTableJsonStr = mqTableJsonStr + string(keyJson) + ":" + string(valueJson)
		first = false
	}
	mqTableJsonStr = mqTableJsonStr + "}"
	var propertiesJson []byte
	propertiesJson, err = json.Marshal(self.Properties)
	if err != nil {
		return
	}
	jsonByte = self.formatEncode("\"properties\"", string(propertiesJson), "\"mqTable\"", string(mqTableJsonStr))
	return
}
func (self *ConsumerRunningInfo)formatEncode(kVList ...string) ([]byte) {
	jsonStr := "{"
	first := true
	for i := 0; i + 1 < len(kVList); i += 2 {
		if first == false {
			jsonStr = jsonStr+","
		}
		keyJson := kVList[i]
		valueJson := kVList[i + 1]

		jsonStr = jsonStr + string(keyJson) + ":" + string(valueJson)

		first = false
	}
	jsonStr = jsonStr + "}"
	return []byte(jsonStr)

}