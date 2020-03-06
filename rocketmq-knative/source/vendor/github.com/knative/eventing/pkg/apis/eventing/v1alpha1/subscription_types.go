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
	"knative.dev/pkg/apis"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	"knative.dev/pkg/webhook"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +k8s:defaulter-gen=true

// Subscription routes events received on a Channel to a DNS name and
// corresponds to the subscriptions.channels.knative.dev CRD.
type Subscription struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata"`
	Spec              SubscriptionSpec   `json:"spec"`
	Status            SubscriptionStatus `json:"status,omitempty"`
}

// Check that Subscription can be validated, can be defaulted, and has immutable fields.
var _ apis.Validatable = (*Subscription)(nil)
var _ apis.Defaultable = (*Subscription)(nil)
var _ apis.Immutable = (*Subscription)(nil)
var _ runtime.Object = (*Subscription)(nil)
var _ webhook.GenericCRD = (*Subscription)(nil)

// SubscriptionSpec specifies the Channel for incoming events, a Subscriber target
// for processing those events and where to put the result of the processing. Only
// From (where the events are coming from) is always required. You can optionally
// only Process the events (results in no output events) by leaving out the Result.
// You can also perform an identity transformation on the incoming events by leaving
// out the Subscriber and only specifying Result.
//
// The following are all valid specifications:
// channel --[subscriber]--> reply
// Sink, no outgoing events:
// channel -- subscriber
// no-op function (identity transformation):
// channel --> reply
type SubscriptionSpec struct {
	// TODO By enabling the status subresource metadata.generation should increment
	// thus making this property obsolete.
	//
	// We should be able to drop this property with a CRD conversion webhook
	// in the future
	//
	// +optional
	DeprecatedGeneration int64 `json:"generation,omitempty"`

	// Reference to a channel that will be used to create the subscription
	// for receiving events. The channel must have spec.subscriptions
	// list which will then be modified accordingly.
	//
	// You can specify only the following fields of the ObjectReference:
	//   - Kind
	//   - APIVersion
	//   - Name
	//  The resource pointed by this ObjectReference must meet the Subscribable contract
	//  with a pointer to the Subscribable duck type. If the resource does not meet this contract,
	//  it will be reflected in the Subscription's status.

	// This field is immutable. We have no good answer on what happens to
	// the events that are currently in the channel being consumed from
	// and what the semantics there should be. For now, you can always
	// delete the Subscription and recreate it to point to a different
	// channel, giving the user more control over what semantics should
	// be used (drain the channel first, possibly have events dropped,
	// etc.)
	Channel corev1.ObjectReference `json:"channel"`

	// Subscriber is reference to (optional) function for processing events.
	// Events from the Channel will be delivered here and replies are
	// sent to a channel as specified by the Reply.
	// +optional
	Subscriber *SubscriberSpec `json:"subscriber,omitempty"`

	// Reply specifies (optionally) how to handle events returned from
	// the Subscriber target.
	// +optional
	Reply *ReplyStrategy `json:"reply,omitempty"`
}

// SubscriberSpec specifies the reference to an object that's expected to
// provide the resolved target of the action.
// Currently we inspect the objects Status and see if there's a predefined
// Status field that we will then use to dispatch events to be processed by
// the target. Currently must resolve to a k8s service.
// Note that in the future we should try to utilize subresources (/resolve ?) to
// make this cleaner, but CRDs do not support subresources yet, so we need
// to rely on a specified Status field today. By relying on this behaviour
// we can utilize a dynamic client instead of having to understand all
// kinds of different types of objects. As long as they adhere to this
// particular contract, they can be used as a Target.
//
// This ensures that we can support external targets and for ease of use
// we also allow for an URI to be specified.
// There of course is also a requirement for the resolved SubscriberSpec to
// behave properly at the data plane level.
// TODO: Add a pointer to a real spec for this.
// For now, this means: Receive an event payload, and respond with one of:
// success and an optional response event, or failure.
// Delivery failures may be retried by the channel
type SubscriberSpec struct {
	// Only one of these can be specified

	// Reference to an object that will be used to find the target
	// endpoint, which should implement the Addressable duck type.
	// For example, this could be a reference to a Route resource
	// or a Knative Service resource.
	// TODO: Specify the required fields the target object must
	// have in the status.
	// You can specify only the following fields of the ObjectReference:
	//   - Kind
	//   - APIVersion
	//   - Name
	// +optional
	Ref *corev1.ObjectReference `json:"ref,omitempty"`

	// Deprecated: Use URI instead.
	// Reference to a 'known' endpoint where no resolving is done.
	// http://k8s-service for example
	// http://myexternalhandler.example.com/foo/bar
	// +optional
	DeprecatedDNSName *string `json:"dnsName,omitempty"`

	// Reference to a 'known' endpoint where no resolving is done.
	// http://k8s-service for example
	// http://myexternalhandler.example.com/foo/bar
	// +optional
	URI *string `json:"uri,omitempty"`
}

// ReplyStrategy specifies the handling of the SubscriberSpec's returned replies.
// If no SubscriberSpec is specified, the identity function is assumed.
type ReplyStrategy struct {
	// You can specify only the following fields of the ObjectReference:
	//   - Kind
	//   - APIVersion
	//   - Name
	//  The resource pointed by this ObjectReference must meet the Addressable contract
	//  with a reference to the Addressable duck type. If the resource does not meet this contract,
	//  it will be reflected in the Subscription's status.
	// +optional
	Channel *corev1.ObjectReference `json:"channel,omitempty"`
}

// SubscriptionStatus (computed) for a subscription
type SubscriptionStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`

	// PhysicalSubscription is the fully resolved values that this Subscription represents.
	PhysicalSubscription SubscriptionStatusPhysicalSubscription `json:"physicalSubscription,omitempty"`
}

// SubscriptionStatusPhysicalSubscription represents the fully resolved values for this
// Subscription.
type SubscriptionStatusPhysicalSubscription struct {
	// SubscriberURI is the fully resolved URI for spec.subscriber.
	SubscriberURI string `json:"subscriberURI,omitempty"`

	// ReplyURI is the fully resolved URI for the spec.reply.
	ReplyURI string `json:"replyURI,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// SubscriptionList returned in list operations
type SubscriptionList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata"`
	Items           []Subscription `json:"items"`
}

// GetGroupVersionKind returns GroupVersionKind for Subscriptions
func (t *Subscription) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("Subscription")
}

// GetSpec returns the spec of the Subscription.
func (s *Subscription) GetSpec() interface{} {
	return s.Spec
}
