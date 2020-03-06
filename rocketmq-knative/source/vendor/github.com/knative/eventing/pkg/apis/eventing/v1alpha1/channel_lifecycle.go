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
	"time"

	"knative.dev/pkg/apis"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var chanCondSet = apis.NewLivingConditionSet(ChannelConditionProvisioned, ChannelConditionAddressable, ChannelConditionProvisionerInstalled)

const (
	// ChannelConditionReady has status True when the Channel is ready to
	// accept traffic.
	ChannelConditionReady = apis.ConditionReady
	// ChannelConditionProvisioned has status True when the Channel's
	// backing resources have been provisioned.
	ChannelConditionProvisioned apis.ConditionType = "Provisioned"

	// ChannelConditionAddressable has status true when this Channel meets
	// the Addressable contract and has a non-empty hostname.
	ChannelConditionAddressable apis.ConditionType = "Addressable"

	// ChannelConditionProvisionerInstalled has status true when the channel is being watched
	// by the provisioner's channel controller (in other words, the provisioner is installed)
	ChannelConditionProvisionerInstalled apis.ConditionType = "ProvisionerInstalled"
)

// GetCondition returns the condition currently associated with the given type, or nil.
func (cs *ChannelStatus) GetCondition(t apis.ConditionType) *apis.Condition {
	return chanCondSet.Manage(cs).GetCondition(t)
}

// IsReady returns true if the resource is ready overall.
func (cs *ChannelStatus) IsReady() bool {
	return chanCondSet.Manage(cs).IsHappy()
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (cs *ChannelStatus) InitializeConditions() {
	chanCondSet.Manage(cs).InitializeConditions()
	// Channel-default-controller sets ChannelConditionProvisionerInstalled=False, and it needs to be set to True by individual controllers
	// This is done so that each individual channel controller gets it for free.
	// It is also implied here that the channel-default-controller never calls InitializeConditions(), while individual channel controllers
	// call InitializeConditions() as one of the first things in its reconcile loop.
	cs.MarkProvisionerInstalled()
}

// MarkProvisioned sets ChannelConditionProvisioned condition to True state.
func (cs *ChannelStatus) MarkProvisioned() {
	chanCondSet.Manage(cs).MarkTrue(ChannelConditionProvisioned)
}

// MarkNotProvisioned sets ChannelConditionProvisioned condition to False state.
func (cs *ChannelStatus) MarkNotProvisioned(reason, messageFormat string, messageA ...interface{}) {
	chanCondSet.Manage(cs).MarkFalse(ChannelConditionProvisioned, reason, messageFormat, messageA...)
}

// MarkProvisionerInstalled sets ChannelConditionProvisionerInstalled condition to True state.
func (cs *ChannelStatus) MarkProvisionerInstalled() {
	chanCondSet.Manage(cs).MarkTrue(ChannelConditionProvisionerInstalled)
}

// MarkProvisionerNotInstalled sets ChannelConditionProvisionerInstalled condition to False state.
func (cs *ChannelStatus) MarkProvisionerNotInstalled(reason, messageFormat string, messageA ...interface{}) {
	chanCondSet.Manage(cs).MarkFalse(ChannelConditionProvisionerInstalled, reason, messageFormat, messageA...)
}

// MarkDeprecated adds a warning condition that this Channel is deprecated and will stop working in
// the future. Note that this does not affect the Ready condition.
func (cs *ChannelStatus) MarkDeprecated(reason, msg string) {
	dc := apis.Condition{
		Type:               "Deprecated",
		Reason:             reason,
		Status:             v1.ConditionTrue,
		Severity:           apis.ConditionSeverityWarning,
		Message:            msg,
		LastTransitionTime: apis.VolatileTime{Inner: metav1.NewTime(time.Now())},
	}
	for i, c := range cs.Conditions {
		if c.Type == dc.Type {
			cs.Conditions[i] = dc
			return
		}
	}
	cs.Conditions = append(cs.Conditions, dc)
}

// SetAddress makes this Channel addressable by setting the hostname. It also
// sets the ChannelConditionAddressable to true.
func (cs *ChannelStatus) SetAddress(url *apis.URL) {
	if url != nil {
		cs.Address.Hostname = url.Host
		cs.Address.URL = url
		chanCondSet.Manage(cs).MarkTrue(ChannelConditionAddressable)
	} else {
		cs.Address.Hostname = ""
		cs.Address.URL = nil
		chanCondSet.Manage(cs).MarkFalse(ChannelConditionAddressable, "emptyHostname", "hostname is the empty string")
	}
}
