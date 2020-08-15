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

// IntegrationSpec defines the desired state of Integration
type IntegrationSpec struct {
	Replicas           *int32               `json:"replicas,omitempty"`
	Sources            []SourceSpec         `json:"sources,omitempty"`
	Resources          []ResourceSpec       `json:"resources,omitempty"`
	Context            string               `json:"context,omitempty"`
	Dependencies       []string             `json:"dependencies,omitempty"`
	Profile            TraitProfile         `json:"profile,omitempty"`
	Traits             map[string]TraitSpec `json:"traits,omitempty"`
	Configuration      []ConfigurationSpec  `json:"configuration,omitempty"`
	Repositories       []string             `json:"repositories,omitempty"`
	ServiceAccountName string               `json:"serviceAccountName,omitempty"`
}

// IntegrationStatus defines the observed state of Integration
type IntegrationStatus struct {
	Phase            IntegrationPhase    `json:"phase,omitempty"`
	Digest           string              `json:"digest,omitempty"`
	Image            string              `json:"image,omitempty"`
	Dependencies     []string            `json:"dependencies,omitempty"`
	Context          string              `json:"context,omitempty"`
	GeneratedSources []SourceSpec        `json:"generatedSources,omitempty"`
	Failure          *Failure            `json:"failure,omitempty"`
	CamelVersion     string              `json:"camelVersion,omitempty"`
	RuntimeVersion   string              `json:"runtimeVersion,omitempty"`
	Configuration    []ConfigurationSpec `json:"configuration,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// Integration is the Schema for the integrations API
// +k8s:openapi-gen=true
type Integration struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   IntegrationSpec   `json:"spec,omitempty"`
	Status IntegrationStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// IntegrationList contains a list of Integration
type IntegrationList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Integration `json:"items"`
}

// DataSpec --
type DataSpec struct {
	Name        string `json:"name,omitempty"`
	Content     string `json:"content,omitempty"`
	ContentRef  string `json:"contentRef,omitempty"`
	ContentKey  string `json:"contentKey,omitempty"`
	Compression bool   `json:"compression,omitempty"`
}

// ResourceType --
type ResourceType string

// ResourceSpec --
type ResourceSpec struct {
	DataSpec
	Type      ResourceType `json:"type,omitempty"`
	MountPath string       `json:"mountPath,omitempty"`
}

const (
	// ResourceTypeData --
	ResourceTypeData ResourceType = "data"
	// ResourceTypeOpenAPI --
	ResourceTypeOpenAPI ResourceType = "openapi"
)

// SourceSpec --
type SourceSpec struct {
	DataSpec
	Language Language `json:"language,omitempty"`
}

// Language --
type Language string

const (
	// LanguageJavaSource --
	LanguageJavaSource Language = "java"
	// LanguageJavaClass --
	LanguageJavaClass Language = "class"
	// LanguageGroovy --
	LanguageGroovy Language = "groovy"
	// LanguageJavaScript --
	LanguageJavaScript Language = "js"
	// LanguageXML --
	LanguageXML Language = "xml"
	// LanguageKotlin --
	LanguageKotlin Language = "kts"
	// LanguageYamlFlow --
	LanguageYamlFlow Language = "flow"
)

// Languages is the list of all supported languages
var Languages = []Language{
	LanguageJavaSource,
	LanguageJavaClass,
	LanguageGroovy,
	LanguageJavaScript,
	LanguageXML,
	LanguageKotlin,
	LanguageYamlFlow,
}

// IntegrationPhase --
type IntegrationPhase string

const (
	// IntegrationKind --
	IntegrationKind string = "Integration"

	// IntegrationPhaseInitial --
	IntegrationPhaseInitial IntegrationPhase = ""
	// IntegrationPhaseWaitingForPlatform --
	IntegrationPhaseWaitingForPlatform IntegrationPhase = "Waiting For Platform"
	// IntegrationPhaseBuildingContext --
	IntegrationPhaseBuildingContext IntegrationPhase = "Building Context"
	// IntegrationPhaseResolvingContext --
	IntegrationPhaseResolvingContext IntegrationPhase = "Resolving Context"
	// IntegrationPhaseDeploying --
	IntegrationPhaseDeploying IntegrationPhase = "Deploying"
	// IntegrationPhaseRunning --
	IntegrationPhaseRunning IntegrationPhase = "Running"
	// IntegrationPhaseError --
	IntegrationPhaseError IntegrationPhase = "Error"
	// IntegrationPhaseDeleting --
	IntegrationPhaseDeleting IntegrationPhase = "Deleting"
)

func init() {
	SchemeBuilder.Register(&Integration{}, &IntegrationList{})
}
