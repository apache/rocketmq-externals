/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package rocketmq

import (
	corev1 "k8s.io/api/core/v1"
)

// AliMNSChannelStatus is the struct saved to Channel's status.internal if the Channel's provisioner
// is gcp-pubsub. It is used to send data to the dispatcher from the controller.
type AliMNSChannelStatus struct {
	// Secret is the Secret that contains the credential to use.
	Secret *corev1.ObjectReference `json:"secret"`
	// SecretKey is the key in Secret that contains the credential to use.
	SecretKey string `json:"secretKey"`
	// Topic is the name of the MNS Topic created in MNS to represent this Channel.
	Topic string `json:"topic,omitempty"`
	// Subscriptions is the list of Knative Eventing Subscriptions to this Channel, each paired with
	// the MNS Subscription in MNS that represents it.
	Subscriptions []AliMNSSubscriptionStatus `json:"subscriptions,omitempty"`
}

// AliMNSSubscriptionStatus represents the saved status of a gcp-pubsub Channel.
type AliMNSSubscriptionStatus struct {
	// Ref is a reference to the Knative Eventing Subscription that this status represents.
	// +optional
	Ref *corev1.ObjectReference `json:"ref,omitempty"`
	// SubscriberURI is a copy of the SubscriberURI of this Subscription.
	// +optional
	SubscriberURI string `json:"subscriberURI,omitempty"`
	// ReplyURI is a copy of the ReplyURI of this Subscription.
	// +optional
	ReplyURI string `json:"replyURI,omitempty"`

	// Subscription is the name of the MNS Subscription resource in MNS that represents this
	// Eventing Subscription.
	Subscription string `json:"subscription,omitempty"`
}

// IsEmpty determines if this AliMNSChannelStatus is equivalent to &AliMNSChannelStatus{}. It
// exists because slices are not compared by golang's ==.
func (pcs *AliMNSChannelStatus) IsEmpty() bool {
	if pcs.Secret != nil {
		return false
	}
	if pcs.SecretKey != "" {
		return false
	}
	if pcs.Topic != "" {
		return false
	}
	if len(pcs.Subscriptions) > 0 {
		return false
	}
	return true
}
