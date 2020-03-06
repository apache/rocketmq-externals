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

// CamelScheme --
type CamelScheme struct {
	ID      string `json:"id" yaml:"id"`
	Passive bool   `json:"passive" yaml:"passive"`
	HTTP    bool   `json:"http" yaml:"http"`
}

// CamelArtifactExclusion --
type CamelArtifactExclusion struct {
	GroupID    string `json:"groupId" yaml:"groupId"`
	ArtifactID string `json:"artifactId" yaml:"artifactId"`
}

// CamelArtifactDependency represent a maven's dependency
type CamelArtifactDependency struct {
	GroupID    string                   `json:"groupId" yaml:"groupId"`
	ArtifactID string                   `json:"artifactId" yaml:"artifactId"`
	Version    string                   `json:"version,omitempty" yaml:"version,omitempty"`
	Exclusions []CamelArtifactExclusion `json:"exclusions,omitempty" yaml:"exclusions,omitempty"`
}

// CamelArtifact --
type CamelArtifact struct {
	CamelArtifactDependency `json:",inline" yaml:",inline"`
	Schemes                 []CamelScheme   `json:"schemes,omitempty" yaml:"schemes,omitempty"`
	Languages               []string        `json:"languages,omitempty" yaml:"languages,omitempty"`
	DataFormats             []string        `json:"dataformats,omitempty" yaml:"dataformats,omitempty"`
	Dependencies            []CamelArtifact `json:"dependencies,omitempty" yaml:"dependencies,omitempty"`
}

// CamelCatalogSpec defines the desired state of CamelCatalog
type CamelCatalogSpec struct {
	Version   string                   `json:"version" yaml:"version"`
	Artifacts map[string]CamelArtifact `json:"artifacts" yaml:"artifacts"`
}

// CamelCatalogStatus defines the observed state of CamelCatalog
type CamelCatalogStatus struct {
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// CamelCatalog is the Schema for the camelcatalogs API
// +k8s:openapi-gen=true
type CamelCatalog struct {
	metav1.TypeMeta   `json:",inline" yaml:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty" yaml:"metadata,omitempty"`

	Status CamelCatalogStatus `json:"status,omitempty" yaml:"status,omitempty"`
	Spec   CamelCatalogSpec   `json:"spec,omitempty" yaml:"spec,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// CamelCatalogList contains a list of CamelCatalog
type CamelCatalogList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []CamelCatalog `json:"items"`
}

const (
	// CamelCatalogKind --
	CamelCatalogKind string = "CamelCatalog"
)

func init() {
	SchemeBuilder.Register(&CamelCatalog{}, &CamelCatalogList{})
}
