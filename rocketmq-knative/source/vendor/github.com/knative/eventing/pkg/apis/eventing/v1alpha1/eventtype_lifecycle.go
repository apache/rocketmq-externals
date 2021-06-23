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

var eventTypeCondSet = apis.NewLivingConditionSet(EventTypeConditionBrokerExists, EventTypeConditionBrokerReady)

const (
	EventTypeConditionReady                           = apis.ConditionReady
	EventTypeConditionBrokerExists apis.ConditionType = "BrokerExists"
	EventTypeConditionBrokerReady  apis.ConditionType = "BrokerReady"
)

// GetCondition returns the condition currently associated with the given type, or nil.
func (et *EventTypeStatus) GetCondition(t apis.ConditionType) *apis.Condition {
	return eventTypeCondSet.Manage(et).GetCondition(t)
}

// IsReady returns true if the resource is ready overall.
func (et *EventTypeStatus) IsReady() bool {
	return eventTypeCondSet.Manage(et).IsHappy()
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (et *EventTypeStatus) InitializeConditions() {
	eventTypeCondSet.Manage(et).InitializeConditions()
}

func (et *EventTypeStatus) MarkBrokerExists() {
	eventTypeCondSet.Manage(et).MarkTrue(EventTypeConditionBrokerExists)
}

func (et *EventTypeStatus) MarkBrokerDoesNotExist() {
	eventTypeCondSet.Manage(et).MarkFalse(EventTypeConditionBrokerExists, "BrokerDoesNotExist", "Broker does not exist")
}

func (et *EventTypeStatus) MarkBrokerReady() {
	eventTypeCondSet.Manage(et).MarkTrue(EventTypeConditionBrokerReady)
}

func (et *EventTypeStatus) MarkBrokerNotReady() {
	eventTypeCondSet.Manage(et).MarkFalse(EventTypeConditionBrokerReady, "BrokerNotReady", "Broker is not ready")
}
