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
package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// IntegrationContextSpec defines the desired state of IntegrationContext
type IntegrationContextSpec struct {
	Image         string               `json:"image,omitempty"`
	Dependencies  []string             `json:"dependencies,omitempty"`
	Profile       TraitProfile         `json:"profile,omitempty"`
	Traits        map[string]TraitSpec `json:"traits,omitempty"`
	Configuration []ConfigurationSpec  `json:"configuration,omitempty"`
	Repositories  []string             `json:"repositories,omitempty"`
}

// IntegrationContextStatus defines the observed state of IntegrationContext
type IntegrationContextStatus struct {
	Phase          IntegrationContextPhase `json:"phase,omitempty"`
	BaseImage      string                  `json:"baseImage,omitempty"`
	Image          string                  `json:"image,omitempty"`
	PublicImage    string                  `json:"publicImage,omitempty"`
	Digest         string                  `json:"digest,omitempty"`
	Artifacts      []Artifact              `json:"artifacts,omitempty"`
	Failure        *Failure                `json:"failure,omitempty"`
	CamelVersion   string                  `json:"camelVersion,omitempty"`
	RuntimeVersion string                  `json:"runtimeVersion,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// IntegrationContext is the Schema for the integrationcontexts API
// +k8s:openapi-gen=true
type IntegrationContext struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   IntegrationContextSpec   `json:"spec,omitempty"`
	Status IntegrationContextStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// IntegrationContextList contains a list of IntegrationContext
type IntegrationContextList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []IntegrationContext `json:"items"`
}

// IntegrationContextPhase --
type IntegrationContextPhase string

const (
	// IntegrationContextKind --
	IntegrationContextKind string = "IntegrationContext"

	// IntegrationContextTypePlatform --
	IntegrationContextTypePlatform = "platform"

	// IntegrationContextTypeUser --
	IntegrationContextTypeUser = "user"

	// IntegrationContextTypeExternal --
	IntegrationContextTypeExternal = "external"

	// IntegrationContextPhaseBuildSubmitted --
	IntegrationContextPhaseBuildSubmitted IntegrationContextPhase = "Build Submitted"
	// IntegrationContextPhaseBuildRunning --
	IntegrationContextPhaseBuildRunning IntegrationContextPhase = "Build Running"
	// IntegrationContextPhaseReady --
	IntegrationContextPhaseReady IntegrationContextPhase = "Ready"
	// IntegrationContextPhaseError --
	IntegrationContextPhaseError IntegrationContextPhase = "Error"
)

func init() {
	SchemeBuilder.Register(&IntegrationContext{}, &IntegrationContextList{})
}
