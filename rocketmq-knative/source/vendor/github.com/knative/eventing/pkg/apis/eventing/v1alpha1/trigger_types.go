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
	"knative.dev/pkg/apis"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	"knative.dev/pkg/kmeta"
	"knative.dev/pkg/webhook"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// Trigger represents a request to have events delivered to a consumer from a
// Broker's event pool.
type Trigger struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the desired state of the Trigger.
	Spec TriggerSpec `json:"spec,omitempty"`

	// Status represents the current state of the Trigger. This data may be out of
	// date.
	// +optional
	Status TriggerStatus `json:"status,omitempty"`
}

var (
	// Check that Trigger can be validated, can be defaulted, and has immutable fields.
	_ apis.Validatable   = (*Trigger)(nil)
	_ apis.Defaultable   = (*Trigger)(nil)
	_ apis.Immutable     = (*Trigger)(nil)
	_ runtime.Object     = (*Trigger)(nil)
	_ webhook.GenericCRD = (*Trigger)(nil)

	// Check that we can create OwnerReferences to a Trigger.
	_ kmeta.OwnerRefable = (*Trigger)(nil)
)

type TriggerSpec struct {
	// Broker is the broker that this trigger receives events from. If not specified, will default
	// to 'default'.
	Broker string `json:"broker,omitempty"`

	// Filter is the filter to apply against all events from the Broker. Only events that pass this
	// filter will be sent to the Subscriber. If not specified, will default to allowing all events.
	//
	// +optional
	Filter *TriggerFilter `json:"filter,omitempty"`

	// Subscriber is the addressable that receives events from the Broker that pass the Filter. It
	// is required.
	Subscriber *SubscriberSpec `json:"subscriber,omitempty"`
}

type TriggerFilter struct {
	SourceAndType *TriggerFilterSourceAndType `json:"sourceAndType,omitempty"`
}

// TriggerFilterSourceAndType filters events based on exact matches on the cloud event's type and
// source attributes. Only exact matches will pass the filter. Either or both type and source can
// use the value 'Any' to indicate all strings match.
type TriggerFilterSourceAndType struct {
	Type   string `json:"type,omitempty"`
	Source string `json:"source,omitempty"`
}

// TriggerStatus represents the current state of a Trigger.
type TriggerStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`

	// SubscriberURI is the resolved URI of the receiver for this Trigger.
	SubscriberURI string `json:"subscriberURI,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// TriggerList is a collection of Triggers.
type TriggerList struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Trigger `json:"items"`
}

// GetGroupVersionKind returns GroupVersionKind for Triggers
func (t *Trigger) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("Trigger")
}

// GetSpec returns the spec of the Trigger.
func (t *Trigger) GetSpec() interface{} {
	return t.Spec
}
