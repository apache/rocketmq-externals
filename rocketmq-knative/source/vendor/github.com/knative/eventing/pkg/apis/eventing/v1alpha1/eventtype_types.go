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
	"knative.dev/pkg/webhook"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type EventType struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the desired state of the EventType.
	Spec EventTypeSpec `json:"spec,omitempty"`

	// Status represents the current state of the EventType.
	// This data may be out of date.
	// +optional
	Status EventTypeStatus `json:"status,omitempty"`
}

// Check that EventType can be validated, can be defaulted, and has immutable fields.
var _ apis.Validatable = (*EventType)(nil)
var _ apis.Defaultable = (*EventType)(nil)
var _ apis.Immutable = (*EventType)(nil)
var _ runtime.Object = (*EventType)(nil)
var _ webhook.GenericCRD = (*EventType)(nil)

type EventTypeSpec struct {
	// Type represents the CloudEvents type. It is authoritative.
	Type string `json:"type"`
	// Source is a URI, it represents the CloudEvents source.
	Source string `json:"source"`
	// Schema is a URI, it represents the CloudEvents schemaurl extension attribute.
	// It may be a JSON schema, a protobuf schema, etc. It is optional.
	// +optional
	Schema string `json:"schema,omitempty"`
	// Broker refers to the Broker that can provide the EventType.
	Broker string `json:"broker"`
	// Description is an optional field used to describe the EventType, in any meaningful way.
	// +optional
	Description string `json:"description,omitempty"`
}

// EventTypeStatus represents the current state of a EventType.
type EventTypeStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// EventTypeList is a collection of EventTypes.
type EventTypeList struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []EventType `json:"items"`
}
