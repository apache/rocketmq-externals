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
	"knative.dev/pkg/apis"
)

const (
	// ContainerSourceConditionReady has status True when the ContainerSource is ready to send events.
	ContainerConditionReady = apis.ConditionReady

	// ContainerConditionSinkProvided has status True when the ContainerSource has been configured with a sink target.
	ContainerConditionSinkProvided apis.ConditionType = "SinkProvided"

	// ContainerConditionDeployed has status True when the ContainerSource has had it's deployment created.
	ContainerConditionDeployed apis.ConditionType = "Deployed"
)

var containerCondSet = apis.NewLivingConditionSet(
	ContainerConditionSinkProvided,
	ContainerConditionDeployed,
)

// GetCondition returns the condition currently associated with the given type, or nil.
func (s *ContainerSourceStatus) GetCondition(t apis.ConditionType) *apis.Condition {
	return containerCondSet.Manage(s).GetCondition(t)
}

// IsReady returns true if the resource is ready overall.
func (s *ContainerSourceStatus) IsReady() bool {
	return containerCondSet.Manage(s).IsHappy()
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (s *ContainerSourceStatus) InitializeConditions() {
	containerCondSet.Manage(s).InitializeConditions()
}

// MarkSink sets the condition that the source has a sink configured.
func (s *ContainerSourceStatus) MarkSink(uri string) {
	s.SinkURI = uri
	if len(uri) > 0 {
		containerCondSet.Manage(s).MarkTrue(ContainerConditionSinkProvided)
	} else {
		containerCondSet.Manage(s).MarkUnknown(ContainerConditionSinkProvided, "SinkEmpty", "Sink has resolved to empty.%s", "")
	}
}

// MarkNoSink sets the condition that the source does not have a sink configured.
func (s *ContainerSourceStatus) MarkNoSink(reason, messageFormat string, messageA ...interface{}) {
	containerCondSet.Manage(s).MarkFalse(ContainerConditionSinkProvided, reason, messageFormat, messageA...)
}

// IsDeployed returns true if the Deployed condition has status true, otherwise
// false.
func (s *ContainerSourceStatus) IsDeployed() bool {
	c := containerCondSet.Manage(s).GetCondition(ContainerConditionDeployed)
	if c != nil {
		return c.IsTrue()
	}
	return false
}

// MarkDeployed sets the condition that the source has been deployed.
func (s *ContainerSourceStatus) MarkDeployed() {
	containerCondSet.Manage(s).MarkTrue(ContainerConditionDeployed)
}

// MarkDeploying sets the condition that the source is deploying.
func (s *ContainerSourceStatus) MarkDeploying(reason, messageFormat string, messageA ...interface{}) {
	containerCondSet.Manage(s).MarkUnknown(ContainerConditionDeployed, reason, messageFormat, messageA...)
}

// MarkNotDeployed sets the condition that the source has not been deployed.
func (s *ContainerSourceStatus) MarkNotDeployed(reason, messageFormat string, messageA ...interface{}) {
	containerCondSet.Manage(s).MarkFalse(ContainerConditionDeployed, reason, messageFormat, messageA...)
}
