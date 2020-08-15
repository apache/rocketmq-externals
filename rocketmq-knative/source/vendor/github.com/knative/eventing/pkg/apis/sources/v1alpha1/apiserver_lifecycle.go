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
	// ApiServerConditionReady has status True when the ApiServerSource is ready to send events.
	ApiServerConditionReady = apis.ConditionReady

	// ApiServerConditionSinkProvided has status True when the ApiServerSource has been configured with a sink target.
	ApiServerConditionSinkProvided apis.ConditionType = "SinkProvided"

	// ApiServerConditionDeployed has status True when the ApiServerSource has had it's deployment created.
	ApiServerConditionDeployed apis.ConditionType = "Deployed"

	// ApiServerConditionEventTypeProvided has status True when the ApiServerSource has been configured with its event types.
	ApiServerConditionEventTypeProvided apis.ConditionType = "EventTypesProvided"
)

var apiserverCondSet = apis.NewLivingConditionSet(
	ApiServerConditionSinkProvided,
	ApiServerConditionDeployed,
)

// GetCondition returns the condition currently associated with the given type, or nil.
func (s *ApiServerSourceStatus) GetCondition(t apis.ConditionType) *apis.Condition {
	return apiserverCondSet.Manage(s).GetCondition(t)
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (s *ApiServerSourceStatus) InitializeConditions() {
	apiserverCondSet.Manage(s).InitializeConditions()
}

// MarkSink sets the condition that the source has a sink configured.
func (s *ApiServerSourceStatus) MarkSink(uri string) {
	s.SinkURI = uri
	if len(uri) > 0 {
		apiserverCondSet.Manage(s).MarkTrue(ApiServerConditionSinkProvided)
	} else {
		apiserverCondSet.Manage(s).MarkUnknown(ApiServerConditionSinkProvided, "SinkEmpty", "Sink has resolved to empty.%s", "")
	}
}

// MarkNoSink sets the condition that the source does not have a sink configured.
func (s *ApiServerSourceStatus) MarkNoSink(reason, messageFormat string, messageA ...interface{}) {
	apiserverCondSet.Manage(s).MarkFalse(ApiServerConditionSinkProvided, reason, messageFormat, messageA...)
}

// MarkDeployed sets the condition that the source has been deployed.
func (s *ApiServerSourceStatus) MarkDeployed() {
	apiserverCondSet.Manage(s).MarkTrue(ApiServerConditionDeployed)
}

// MarkEventTypes sets the condition that the source has set its event type.
func (s *ApiServerSourceStatus) MarkEventTypes() {
	apiserverCondSet.Manage(s).MarkTrue(ApiServerConditionEventTypeProvided)
}

// MarkNoEventTypes sets the condition that the source does not its event type configured.
func (s *ApiServerSourceStatus) MarkNoEventTypes(reason, messageFormat string, messageA ...interface{}) {
	apiserverCondSet.Manage(s).MarkFalse(ApiServerConditionEventTypeProvided, reason, messageFormat, messageA...)
}

// IsReady returns true if the resource is ready overall.
func (s *ApiServerSourceStatus) IsReady() bool {
	return apiserverCondSet.Manage(s).IsHappy()
}
