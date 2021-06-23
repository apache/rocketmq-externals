/*
 * Copyright 2018 The Knative Authors
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
	eventingduck "github.com/knative/eventing/pkg/apis/duck/v1alpha1"
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

// Channel is an abstract resource that implements the Addressable contract.
// The Provisioner provisions infrastructure to accepts events and
// deliver to Subscriptions.
type Channel struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the desired state of the Channel.
	Spec ChannelSpec `json:"spec,omitempty"`

	// Status represents the current state of the Channel. This data may be out of
	// date.
	// +optional
	Status ChannelStatus `json:"status,omitempty"`
}

var (
	// Check that Channel can be validated, can be defaulted, and has immutable fields.
	_ apis.Validatable   = (*Channel)(nil)
	_ apis.Defaultable   = (*Channel)(nil)
	_ apis.Immutable     = (*Channel)(nil)
	_ runtime.Object     = (*Channel)(nil)
	_ webhook.GenericCRD = (*Channel)(nil)

	// Check that we can create OwnerReferences to a Channel.
	_ kmeta.OwnerRefable = (*Channel)(nil)
)

// ChannelSpec specifies the Provisioner backing a channel and the configuration
// arguments for a Channel.
type ChannelSpec struct {
	// TODO By enabling the status subresource metadata.generation should increment
	// thus making this property obsolete.
	//
	// We should be able to drop this property with a CRD conversion webhook
	// in the future
	//
	// +optional
	DeprecatedGeneration int64 `json:"generation,omitempty"`

	// Provisioner defines the name of the Provisioner backing this channel.
	Provisioner *corev1.ObjectReference `json:"provisioner,omitempty"`

	// Arguments defines the arguments to pass to the Provisioner which
	// provisions this Channel.
	// +optional
	Arguments *runtime.RawExtension `json:"arguments,omitempty"`

	// Channel conforms to Duck type Subscribable.
	Subscribable *eventingduck.Subscribable `json:"subscribable,omitempty"`
}

// ChannelStatus represents the current state of a Channel.
type ChannelStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`

	// Channel is Addressable. It currently exposes the endpoint as a
	// fully-qualified DNS name which will distribute traffic over the
	// provided targets from inside the cluster.
	//
	// It generally has the form {channel}.{namespace}.svc.{cluster domain name}
	Address duckv1alpha1.Addressable `json:"address,omitempty"`

	// Internal is status unique to each ClusterChannelProvisioner.
	// +optional
	Internal *runtime.RawExtension `json:"internal,omitempty"`

	eventingduck.SubscribableTypeStatus `json:",inline"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// ChannelList is a collection of Channels.
type ChannelList struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Channel `json:"items"`
}

// GetGroupVersionKind returns GroupVersionKind for Channels
func (c *Channel) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("Channel")
}

// GetSpec returns the spec of the Channel.
func (c *Channel) GetSpec() interface{} {
	return c.Spec
}
