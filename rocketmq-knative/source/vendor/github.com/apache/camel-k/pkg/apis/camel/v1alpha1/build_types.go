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

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// BuildSpec defines the desired state of Build
type BuildSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	Meta           metav1.ObjectMeta       `json:"meta,omitempty"`
	Image          string                  `json:"image,omitempty"`
	Steps          []string                `json:"steps,omitempty"`
	CamelVersion   string                  `json:"camelVersion,omitempty"`
	RuntimeVersion string                  `json:"runtimeVersion,omitempty"`
	Platform       IntegrationPlatformSpec `json:"platform,omitempty"`
	Sources        []SourceSpec            `json:"sources,omitempty"`
	Resources      []ResourceSpec          `json:"resources,omitempty"`
	Dependencies   []string                `json:"dependencies,omitempty"`
	BuildDir       string                  `json:"buildDir,omitempty"`
}

// BuildStatus defines the observed state of Build
type BuildStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	Phase       BuildPhase  `json:"phase,omitempty"`
	Image       string      `json:"image,omitempty"`
	BaseImage   string      `json:"baseImage,omitempty"`
	PublicImage string      `json:"publicImage,omitempty"`
	Artifacts   []Artifact  `json:"artifacts,omitempty"`
	Error       string      `json:"error,omitempty"`
	Failure     *Failure    `json:"failure,omitempty"`
	StartedAt   metav1.Time `json:"startedAt,omitempty"`
	// Change to Duration / ISO 8601 when CRD uses OpenAPI spec v3
	// https://github.com/OAI/OpenAPI-Specification/issues/845
	Duration string `json:"duration,omitempty"`
}

// BuildPhase --
type BuildPhase string

const (
	// BuildKind --
	BuildKind string = "Build"

	// BuildPhaseInitial --
	BuildPhaseInitial BuildPhase = ""
	// BuildPhaseScheduling --
	BuildPhaseScheduling BuildPhase = "Scheduling"
	// BuildPhasePending --
	BuildPhasePending BuildPhase = "Pending"
	// BuildPhaseRunning --
	BuildPhaseRunning BuildPhase = "Running"
	// BuildPhaseSucceeded --
	BuildPhaseSucceeded BuildPhase = "Succeeded"
	// BuildPhaseFailed --
	BuildPhaseFailed BuildPhase = "Failed"
	// BuildPhaseInterrupted --
	BuildPhaseInterrupted = "Interrupted"
	// BuildPhaseError --
	BuildPhaseError BuildPhase = "Error"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// Build is the Schema for the builds API
// +k8s:openapi-gen=true
type Build struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   BuildSpec   `json:"spec,omitempty"`
	Status BuildStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// BuildList contains a list of Build
type BuildList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Build `json:"items"`
}

func init() {
	SchemeBuilder.Register(&Build{}, &BuildList{})
}
