/*
Copyright 2019 The Knative Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1alpha1

import (
	"knative.dev/pkg/apis"
	"knative.dev/pkg/apis/duck"
	"knative.dev/pkg/apis/duck/v1alpha1"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// Channelable is a skeleton type wrapping Subscribable and Addressable in the manner we expect resource writers
// defining compatible resources to embed it. We will typically use this type to deserialize
// Channelable ObjectReferences and access their subscription and address data.  This is not a real resource.
type Channelable struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec is the part where the Channelable fulfills the Subscribable contract.
	Spec ChannelableSpec `json:"spec,omitempty"`

	Status ChannelableStatus `json:"status,omitempty"`
}

// ChannelableSpec contains Spec of the Channelable object
type ChannelableSpec struct {
	SubscribableTypeSpec `json:",inline"`
}

// ChannelableStatus contains the Status of a Channelable object.
type ChannelableStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`
	// AddressStatus is the part where the Channelable fulfills the Addressable contract.
	v1alpha1.AddressStatus `json:",inline"`
	// Subscribers is populated with the statuses of each of the Channelable's subscribers.
	SubscribableTypeStatus `json:",inline"`
}

var (
	// Verify Channelable resources meet duck contracts.
	_ duck.Populatable = (*Channelable)(nil)
	_ apis.Listable    = (*Channelable)(nil)
)

// Populate implements duck.Populatable
func (c *Channelable) Populate() {
	c.Spec.Subscribable = &Subscribable{
		// Populate ALL fields
		Subscribers: []SubscriberSpec{{
			UID:           "2f9b5e8e-deb6-11e8-9f32-f2801f1b9fd1",
			Generation:    1,
			SubscriberURI: "call1",
			ReplyURI:      "sink2",
		}, {
			UID:           "34c5aec8-deb6-11e8-9f32-f2801f1b9fd1",
			Generation:    2,
			SubscriberURI: "call2",
			ReplyURI:      "sink2",
		}},
	}
	c.Status = ChannelableStatus{
		AddressStatus: v1alpha1.AddressStatus{
			Address: &v1alpha1.Addressable{
				// Populate ALL fields
				Addressable: duckv1beta1.Addressable{
					URL: &apis.URL{
						Scheme: "http",
						Host:   "test-domain",
					},
				},
				Hostname: "test-domain",
			},
		},
		SubscribableTypeStatus: SubscribableTypeStatus{
			SubscribableStatus: &SubscribableStatus{
				Subscribers: []SubscriberStatus{{
					UID:                "2f9b5e8e-deb6-11e8-9f32-f2801f1b9fd1",
					ObservedGeneration: 1,
					Ready:              corev1.ConditionTrue,
					Message:            "Some message",
				}, {
					UID:                "34c5aec8-deb6-11e8-9f32-f2801f1b9fd1",
					ObservedGeneration: 2,
					Ready:              corev1.ConditionFalse,
					Message:            "Some message",
				}},
			},
		},
	}
}

// GetListType implements apis.Listable
func (c *Channelable) GetListType() runtime.Object {
	return &ChannelableList{}
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// ChannelableList is a list of Channelable resources.
type ChannelableList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata"`

	Items []Channelable `json:"items"`
}
