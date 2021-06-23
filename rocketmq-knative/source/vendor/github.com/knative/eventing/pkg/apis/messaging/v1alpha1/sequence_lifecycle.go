/*
 * Copyright 2019 The Knative Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1alpha1

import (
	duckv1alpha1 "github.com/knative/eventing/pkg/apis/duck/v1alpha1"
	eventingv1alpha1 "github.com/knative/eventing/pkg/apis/eventing/v1alpha1"
	"knative.dev/pkg/apis"
	pkgduckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
	corev1 "k8s.io/api/core/v1"
)

var pCondSet = apis.NewLivingConditionSet(SequenceConditionReady, SequenceConditionChannelsReady, SequenceConditionSubscriptionsReady, SequenceConditionAddressable)

const (
	// SequenceConditionReady has status True when all subconditions below have been set to True.
	SequenceConditionReady = apis.ConditionReady

	// SequenceChannelsReady has status True when all the channels created as part of
	// this sequence are ready.
	SequenceConditionChannelsReady apis.ConditionType = "ChannelsReady"

	// SequenceSubscriptionsReady has status True when all the subscriptions created as part of
	// this sequence are ready.
	SequenceConditionSubscriptionsReady apis.ConditionType = "SubscriptionsReady"

	// SequenceConditionAddressable has status true when this Sequence meets
	// the Addressable contract and has a non-empty hostname.
	SequenceConditionAddressable apis.ConditionType = "Addressable"
)

// GetCondition returns the condition currently associated with the given type, or nil.
func (ps *SequenceStatus) GetCondition(t apis.ConditionType) *apis.Condition {
	return pCondSet.Manage(ps).GetCondition(t)
}

// IsReady returns true if the resource is ready overall.
func (ps *SequenceStatus) IsReady() bool {
	return pCondSet.Manage(ps).IsHappy()
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (ps *SequenceStatus) InitializeConditions() {
	pCondSet.Manage(ps).InitializeConditions()
}

// PropagateSubscriptionStatuses sets the SubscriptionStatuses and SequenceConditionSubscriptionsReady based on
// the status of the incoming subscriptions.
func (ps *SequenceStatus) PropagateSubscriptionStatuses(subscriptions []*eventingv1alpha1.Subscription) {
	ps.SubscriptionStatuses = make([]SequenceSubscriptionStatus, len(subscriptions))
	allReady := true
	// If there are no subscriptions, treat that as a False case. Could go either way, but this seems right.
	if len(subscriptions) == 0 {
		allReady = false

	}
	for i, s := range subscriptions {
		ps.SubscriptionStatuses[i] = SequenceSubscriptionStatus{
			Subscription: corev1.ObjectReference{
				APIVersion: s.APIVersion,
				Kind:       s.Kind,
				Name:       s.Name,
				Namespace:  s.Namespace,
			},
		}
		readyCondition := s.Status.GetCondition(eventingv1alpha1.SubscriptionConditionReady)
		if readyCondition != nil {
			ps.SubscriptionStatuses[i].ReadyCondition = *readyCondition
			if readyCondition.Status != corev1.ConditionTrue {
				allReady = false
			}
		} else {
			allReady = false
		}

	}
	if allReady {
		pCondSet.Manage(ps).MarkTrue(SequenceConditionSubscriptionsReady)
	} else {
		ps.MarkSubscriptionsNotReady("SubscriptionsNotReady", "Subscriptions are not ready yet, or there are none")
	}
}

// PropagateChannelStatuses sets the ChannelStatuses and SequenceConditionChannelsReady based on the
// status of the incoming channels.
func (ps *SequenceStatus) PropagateChannelStatuses(channels []*duckv1alpha1.Channelable) {
	ps.ChannelStatuses = make([]SequenceChannelStatus, len(channels))
	allReady := true
	// If there are no channels, treat that as a False case. Could go either way, but this seems right.
	if len(channels) == 0 {
		allReady = false

	}
	for i, c := range channels {
		ps.ChannelStatuses[i] = SequenceChannelStatus{
			Channel: corev1.ObjectReference{
				APIVersion: c.APIVersion,
				Kind:       c.Kind,
				Name:       c.Name,
				Namespace:  c.Namespace,
			},
		}
		// TODO: Once the addressable has a real status to dig through, use that here instead of
		// addressable, because it might be addressable but not ready.
		address := c.Status.AddressStatus.Address
		if address != nil {
			ps.ChannelStatuses[i].ReadyCondition = apis.Condition{Type: apis.ConditionReady, Status: corev1.ConditionTrue}
		} else {
			ps.ChannelStatuses[i].ReadyCondition = apis.Condition{Type: apis.ConditionReady, Status: corev1.ConditionFalse, Reason: "NotAddressable", Message: "Channel is not addressable"}
			allReady = false
		}

		// Mark the Sequence address as the Address of the first channel.
		if i == 0 {
			ps.setAddress(address)
		}
	}
	if allReady {
		pCondSet.Manage(ps).MarkTrue(SequenceConditionChannelsReady)
	} else {
		ps.MarkChannelsNotReady("ChannelsNotReady", "Channels are not ready yet, or there are none")
	}
}

func (ps *SequenceStatus) MarkChannelsNotReady(reason, messageFormat string, messageA ...interface{}) {
	pCondSet.Manage(ps).MarkFalse(SequenceConditionChannelsReady, reason, messageFormat, messageA...)
}

func (ps *SequenceStatus) MarkSubscriptionsNotReady(reason, messageFormat string, messageA ...interface{}) {
	pCondSet.Manage(ps).MarkFalse(SequenceConditionSubscriptionsReady, reason, messageFormat, messageA...)
}

func (ps *SequenceStatus) MarkAddressableNotReady(reason, messageFormat string, messageA ...interface{}) {
	pCondSet.Manage(ps).MarkFalse(SequenceConditionAddressable, reason, messageFormat, messageA...)
}

func (ps *SequenceStatus) setAddress(address *pkgduckv1alpha1.Addressable) {
	ps.Address = address

	if address == nil {
		pCondSet.Manage(ps).MarkFalse(SequenceConditionAddressable, "emptyHostname", "hostname is the empty string")
		return
	}
	if address.URL != nil || address.Hostname != "" {
		pCondSet.Manage(ps).MarkTrue(SequenceConditionAddressable)
	} else {
		ps.Address.Hostname = ""
		ps.Address.URL = nil
		pCondSet.Manage(ps).MarkFalse(SequenceConditionAddressable, "emptyHostname", "hostname is the empty string")
	}
}
