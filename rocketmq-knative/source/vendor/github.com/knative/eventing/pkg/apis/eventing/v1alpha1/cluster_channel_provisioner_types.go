/*
Copyright 2018 The Knative Authors

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
	"knative.dev/pkg/apis"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	"knative.dev/pkg/kmeta"
	"knative.dev/pkg/webhook"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// +genclient
// +genclient:nonNamespaced
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// ClusterChannelProvisioner encapsulates a provisioning strategy for the
// backing resources required to realize a particular resource type.
type ClusterChannelProvisioner struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the Types provisioned by this Provisioner.
	Spec ClusterChannelProvisionerSpec `json:"spec"`

	// Status is the current status of the Provisioner.
	// +optional
	Status ClusterChannelProvisionerStatus `json:"status,omitempty"`
}

var (
	// Check that ClusterChannelProvisioner can be validated and can be defaulted.
	_ apis.Validatable   = (*ClusterChannelProvisioner)(nil)
	_ apis.Defaultable   = (*ClusterChannelProvisioner)(nil)
	_ runtime.Object     = (*ClusterChannelProvisioner)(nil)
	_ webhook.GenericCRD = (*ClusterChannelProvisioner)(nil)

	// Check that we can create OwnerReferences to a ClusterChannelProvisioner.
	_ kmeta.OwnerRefable = (*ClusterChannelProvisioner)(nil)
)

// ClusterChannelProvisionerSpec is the spec for a ClusterChannelProvisioner resource.
type ClusterChannelProvisionerSpec struct {
	// TODO By enabling the status subresource metadata.generation should increment
	// thus making this property obsolete.
	//
	// We should be able to drop this property with a CRD conversion webhook
	// in the future
	//
	// +optional
	DeprecatedGeneration int64 `json:"generation,omitempty"`
}

var ccProvCondSet = apis.NewLivingConditionSet()

// ClusterChannelProvisionerStatus is the status for a ClusterChannelProvisioner resource
type ClusterChannelProvisionerStatus struct {
	// inherits duck/v1beta1 Status, which currently provides:
	// * ObservedGeneration - the 'Generation' of the Service that was last processed by the controller.
	// * Conditions - the latest available observations of a resource's current state.
	duckv1beta1.Status `json:",inline"`
}

const (
	// ClusterChannelProvisionerConditionReady has status True when the Controller reconciling objects
	// controlled by it is ready to control them.
	ClusterChannelProvisionerConditionReady = apis.ConditionReady
)

// GetCondition returns the condition currently associated with the given type, or nil.
func (ps *ClusterChannelProvisionerStatus) GetCondition(t apis.ConditionType) *apis.Condition {
	return ccProvCondSet.Manage(ps).GetCondition(t)
}

// IsReady returns true if the resource is ready overall.
func (ps *ClusterChannelProvisionerStatus) IsReady() bool {
	return ccProvCondSet.Manage(ps).IsHappy()
}

// MarkProvisionerNotReady sets the condition that the provisioner is not ready to provision backing resource.
func (ps *ClusterChannelProvisionerStatus) MarkNotReady(reason, messageFormat string, messageA ...interface{}) {
	ccProvCondSet.Manage(ps).MarkFalse(ClusterChannelProvisionerConditionReady, reason, messageFormat, messageA...)
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (ps *ClusterChannelProvisionerStatus) InitializeConditions() {
	ccProvCondSet.Manage(ps).InitializeConditions()
}

// MarkReady marks this ClusterChannelProvisioner as Ready=true.
//
// Note that this is not the normal pattern for duck conditions, but because there is (currently)
// no other condition on ClusterChannelProvisioners, the normal IsReady() logic doesn't work well.
func (ps *ClusterChannelProvisionerStatus) MarkReady() {
	ccProvCondSet.Manage(ps).MarkTrue(ClusterChannelProvisionerConditionReady)
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// ClusterChannelProvisionerList is a list of ClusterChannelProvisioner resources
type ClusterChannelProvisionerList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata"`

	Items []ClusterChannelProvisioner `json:"items"`
}

// GetGroupVersionKind return GroupVersionKind for ClusterChannelProvisioner
func (ccp *ClusterChannelProvisioner) GetGroupVersionKind() schema.GroupVersionKind {
	return SchemeGroupVersion.WithKind("ClusterChannelProvisioner")
}
