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
	duckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	"knative.dev/pkg/kmeta"
	"knative.dev/pkg/webhook"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// Broker collects a pool of events that are consumable using Triggers. Brokers
// provide a well-known endpoint for event delivery that senders can use with
// minimal knowledge of the event routing strategy. Receivers use Triggers to
// request delivery of events from a Broker's pool to a specific URL or
// Addressable endpoint.
type Broker struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the desired state of the Broker.
	Spec BrokerSpec `json:"spec,omitempty"`

	// Status represents the current state of the Broker. This data may be out of
	// date.
	// +optional
	Status BrokerStatus `json:"status,omitempty"`
}

var (
	// Check that Broker can be validated, can be defaulted, and has immutable fields.
	_ apis.Validatable   = (*Broker)(nil)
	_ apis.Defaultable   = (*Broker)(nil)
	_ apis.Immutable     = (*Broker)(nil)
	_ runtime.Object     = (*Broker)(nil)
	_ webhook.GenericCRD = (*Broker)(nil)

	// Check that we can create OwnerReferences to a Broker.
	_ kmeta.OwnerRefable = (*Broker)(nil)
)

// This should be duck so that Broker can also use this
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type ChannelTemplateSpec struct {
	metav1.TypeMeta `json:",inline"`

	// Spec defines the Spec to use for each channel created. Passed
	// in verbatim to the Channel CRD as Spec section.
	// +optional
	Spec runtime.RawExtension `json:"spec"`
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
	Spec runtime.RawExtension `json:"spec"`
}

type BrokerSpec struct {
	// DeprecatedChannelTemplate, if specified will be used to create all the Channels used internally by the
	// Broker. Only Provisioner and Arguments may be specified. If left unspecified, the default
	// Channel for the namespace will be used.
	//
	// +optional
	DeprecatedChannelTemplate *ChannelSpec `json:"channelTemplate,omitempty"`

	// ChannelTemplate specifies which Channel CRD to use to create all the Channels used internally by the
	// Broker.
	ChannelTemplate ChannelTemplateSpec `json:"channelTemplateSpec"`
}

// BrokerStatus represents the current state of a Broker.
type BrokerStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`

	// Broker is Addressable. It currently exposes the endpoint as a
	// fully-qualified DNS name which will distribute traffic over the
	// provided targets from inside the cluster.
	//
	// It generally has the form {broker}-router.{namespace}.svc.{cluster domain name}
	Address duckv1alpha1.Addressable `json:"address,omitempty"`

	// TriggerChannel is an objectref to the object for the TriggerChannel
	TriggerChannel *corev1.ObjectReference `json:"triggerChannel,omitempty"`

	// IngressChannel is an objectref to the object for the IngressChannel
	IngressChannel *corev1.ObjectReference `json:"IngressChannel,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// BrokerList is a collection of Brokers.
type BrokerList struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Broker `json:"items"`
}

// GetGroupVersionKind returns GroupVersionKind for Brokers
func (t *Broker) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("Broker")
}

// GetSpec returns the spec of the Broker.
func (b *Broker) GetSpec() interface{} {
	return b.Spec
}
