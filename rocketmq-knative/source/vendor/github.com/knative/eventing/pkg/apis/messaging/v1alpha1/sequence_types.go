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
	eventingv1alpha1 "github.com/knative/eventing/pkg/apis/eventing/v1alpha1"
	"knative.dev/pkg/apis"
	duckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	"knative.dev/pkg/webhook"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// Sequence defines a sequence of Subscribers that will be wired in
// series through Channels and Subscriptions.
type Sequence struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the desired state of the Sequence.
	Spec SequenceSpec `json:"spec,omitempty"`

	// Status represents the current state of the Sequence. This data may be out of
	// date.
	// +optional
	Status SequenceStatus `json:"status,omitempty"`
}

// Check that Sequence can be validated, can be defaulted, and has immutable fields.
var _ apis.Validatable = (*Sequence)(nil)
var _ apis.Defaultable = (*Sequence)(nil)

// TODO: make appropriate fields immutable.
//var _ apis.Immutable = (*Sequence)(nil)
var _ runtime.Object = (*Sequence)(nil)
var _ webhook.GenericCRD = (*Sequence)(nil)

// This should be duck so that Broker can also use this
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type ChannelTemplateSpec struct {
	metav1.TypeMeta `json:",inline"`

	// Spec defines the Spec to use for each channel created. Passed
	// in verbatim to the Channel CRD as Spec section.
	// +optional
	Spec *runtime.RawExtension `json:"spec,omitempty"`
}

// Internal version of ChannelTemplateSpec that includes ObjectMeta so that
// we can easily create new Channels off of it.
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type ChannelTemplateSpecInternal struct {
	metav1.TypeMeta `json:",inline"`

	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the Spec to use for each channel created. Passed
	// in verbatim to the Channel CRD as Spec section.
	// +optional
	Spec *runtime.RawExtension `json:"spec,omitempty"`
}

type SequenceSpec struct {
	// Steps is the list of Subscribers (processors / functions) that will be called in the order
	// provided.
	Steps []eventingv1alpha1.SubscriberSpec `json:"steps"`

	// ChannelTemplate specifies which Channel CRD to use
	ChannelTemplate ChannelTemplateSpec `json:"channelTemplate"`

	// Reply is a Reference to where the result of the last Subscriber gets sent to.
	//
	// You can specify only the following fields of the ObjectReference:
	//   - Kind
	//   - APIVersion
	//   - Name
	//
	//  The resource pointed by this ObjectReference must meet the Addressable contract
	//  with a reference to the Addressable duck type. If the resource does not meet this contract,
	//  it will be reflected in the Subscription's status.
	// +optional
	Reply *corev1.ObjectReference `json:"reply,omitempty"`
}

type SequenceChannelStatus struct {
	// Channel is the reference to the underlying channel.
	Channel corev1.ObjectReference `json:"channel"`

	// ReadyCondition indicates whether the Channel is ready or not.
	ReadyCondition apis.Condition `json:"ready"`
}

type SequenceSubscriptionStatus struct {
	// Subscription is the reference to the underlying Subscription.
	Subscription corev1.ObjectReference `json:"subscription"`

	// ReadyCondition indicates whether the Subscription is ready or not.
	ReadyCondition apis.Condition `json:"ready"`
}

// SequenceStatus represents the current state of a Sequence.
type SequenceStatus struct {
	// inherits duck/v1alpha1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`

	// SubscriptionStatuses is an array of corresponding Subscription statuses.
	// Matches the Spec.Steps array in the order.
	SubscriptionStatuses []SequenceSubscriptionStatus

	// ChannelStatuses is an array of corresponding Channel statuses.
	// Matches the Spec.Steps array in the order.
	ChannelStatuses []SequenceChannelStatus

	// AddressStatus is the starting point to this Sequence. Sending to this
	// will target the first subscriber.
	// It generally has the form {channel}.{namespace}.svc.{cluster domain name}
	duckv1alpha1.AddressStatus `json:",inline"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// SequenceList is a collection of Sequences.
type SequenceList struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Sequence `json:"items"`
}

// GetGroupVersionKind returns GroupVersionKind for InMemoryChannels
func (p *Sequence) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("Sequence")
}
