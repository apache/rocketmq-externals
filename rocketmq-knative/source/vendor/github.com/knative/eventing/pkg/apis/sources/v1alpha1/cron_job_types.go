/*
Copyright 2019 The Knative Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1alpha1

import (
	"fmt"

	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	"knative.dev/pkg/kmeta"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +k8s:defaulter-gen=true

// CronJobSource is the Schema for the cronjobsources API.
type CronJobSource struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   CronJobSourceSpec   `json:"spec,omitempty"`
	Status CronJobSourceStatus `json:"status,omitempty"`
}

// TODO: Check that CronJobSource can be validated and can be defaulted.

var (
	// Check that it is a runtime object.
	_ runtime.Object = (*CronJobSource)(nil)

	// Check that we can create OwnerReferences to a Configuration.
	_ kmeta.OwnerRefable = (*CronJobSource)(nil)
)

const (
	// CronJobEventType is the CronJob CloudEvent type.
	CronJobEventType = "dev.knative.cronjob.event"
)

// CronJobEventSource returns the CronJob CloudEvent source.
func CronJobEventSource(namespace, cronJobName string) string {
	return fmt.Sprintf("/apis/v1/namespaces/%s/cronjobsources/%s", namespace, cronJobName)
}

type CronJobRequestsSpec struct {
	ResourceCPU    string `json:"cpu,omitempty"`
	ResourceMemory string `json:"memory,omitempty"`
}

type CronJobLimitsSpec struct {
	ResourceCPU    string `json:"cpu,omitempty"`
	ResourceMemory string `json:"memory,omitempty"`
}

type CronJobResourceSpec struct {
	Requests CronJobRequestsSpec `json:"requests,omitempty"`
	Limits   CronJobLimitsSpec   `json:"limits,omitempty"`
}

// CronJobSourceSpec defines the desired state of the CronJobSource.
type CronJobSourceSpec struct {
	// Schedule is the cronjob schedule.
	// +required
	Schedule string `json:"schedule"`

	// Data is the data posted to the target function.
	Data string `json:"data,omitempty"`

	// Sink is a reference to an object that will resolve to a domain name to use as the sink.
	// +optional
	Sink *corev1.ObjectReference `json:"sink,omitempty"`

	// ServiceAccoutName is the name of the ServiceAccount that will be used to run the Receive
	// Adapter Deployment.
	ServiceAccountName string `json:"serviceAccountName,omitempty"`

	// Resource limits and Request specifications of the Receive Adapter Deployment
	Resources CronJobResourceSpec `json:"resources,omitempty"`
}

// GetGroupVersionKind returns the GroupVersionKind.
func (s *CronJobSource) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("CronJobSource")
}

// CronJobSourceStatus defines the observed state of CronJobSource.
type CronJobSourceStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`

	// SinkURI is the current active sink URI that has been configured for the CronJobSource.
	// +optional
	SinkURI string `json:"sinkUri,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// CronJobSourceList contains a list of CronJobSources.
type CronJobSourceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []CronJobSource `json:"items"`
}
