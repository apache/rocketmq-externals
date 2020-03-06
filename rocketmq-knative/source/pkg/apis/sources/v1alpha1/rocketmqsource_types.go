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
	"fmt"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"knative.dev/pkg/apis/duck"
	duckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
)



var _ runtime.Object = (*RocketMQSource)(nil)

var _ = duck.VerifyType(&RocketMQSource{}, &duckv1alpha1.Conditions{})

const (
	RocketMQSourceEventType = "apache.rocketmq"
)


func RocketMQEventSource(topic string) string {
	return fmt.Sprintf("%s", topic)
}

const (
	RocketMQConditionReady = duckv1alpha1.ConditionReady

	RocketMQConditionSinkProvided duckv1alpha1.ConditionType = "SinkProvided"

	RocketMQConditionTransformerProvided duckv1alpha1.ConditionType = "TransformerProvided"

	RocketMQConditionDeployed duckv1alpha1.ConditionType = "Deployed"

	RocketMQConditionSubscribed duckv1alpha1.ConditionType = "Subscribed"

	RocketMQConditionEventTypesProvided duckv1alpha1.ConditionType = "EventTypesProvided"
)

var RocketMQSourceCondSet = duckv1alpha1.NewLivingConditionSet(
	RocketMQConditionSinkProvided,
	RocketMQConditionDeployed,
	RocketMQConditionSubscribed)


type RocketMQSource struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   RocketMQSourceSpec   `json:"spec,omitempty"`
	Status RocketMQSourceStatus `json:"status,omitempty"`
}

type RocketMQSourceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []RocketMQSource `json:"items"`
}

func init() {
	SchemeBuilder.Register(&RocketMQSource{}, &RocketMQSourceList{})
}


type RocketMQSourceSpec struct {
	AccessToken SecretValueFromSource `json:"accessToken"`
	Topic string `json:"topic,omitempty"`
	NamesrvAddr string `json:"namesrvAddr,omitempty"`
	Namespace string `json:"namespace,omitempty"`
	GroupName string `json:"groupName,omitempty"`
	Sink *corev1.ObjectReference `json:"sink,omitempty"`
	ServiceAccountName string `json:"serviceAccountName,omitempty"`
}


type RocketMQSourceStatus struct {
	duckv1alpha1.Status `json:",inline"`
	SinkURI string `json:"sinkUri,omitempty"`
}


func (s *RocketMQSourceStatus) GetCondition(t duckv1alpha1.ConditionType) *duckv1alpha1.Condition {
	return RocketMQSourceCondSet.Manage(s).GetCondition(t)
}

// IsReady returns true if the resource is ready overall.
func (s *RocketMQSourceStatus) IsReady() bool {
	return RocketMQSourceCondSet.Manage(s).IsHappy()
}

// InitializeConditions sets relevant unset conditions to Unknown state.
func (s *RocketMQSourceStatus) InitializeConditions() {
	RocketMQSourceCondSet.Manage(s).InitializeConditions()
}

// MarkSink sets the condition that the source has a sink configured.
func (s *RocketMQSourceStatus) MarkSink(uri string) {
	s.SinkURI = uri
	if len(uri) > 0 {
		RocketMQSourceCondSet.Manage(s).MarkTrue(RocketMQConditionSinkProvided)
	} else {
		RocketMQSourceCondSet.Manage(s).MarkUnknown(RocketMQConditionSinkProvided, "SinkEmpty", "Sink has resolved to empty.")
	}
}
func (s *RocketMQSourceStatus) MarkNoSink(reason, messageFormat string, messageA ...interface{}) {
	RocketMQSourceCondSet.Manage(s).MarkFalse(RocketMQConditionSinkProvided, reason, messageFormat, messageA...)
}

func (s *RocketMQSourceStatus) MarkNoTransformer(reason, messageFormat string, messageA ...interface{}) {
	RocketMQSourceCondSet.Manage(s).MarkFalse(RocketMQConditionTransformerProvided, reason, messageFormat, messageA...)
}

// MarkDeployed sets the condition that the source has been deployed.
func (s *RocketMQSourceStatus) MarkDeployed() {
	RocketMQSourceCondSet.Manage(s).MarkTrue(RocketMQConditionDeployed)
}

// MarkDeploying sets the condition that the source is deploying.
func (s *RocketMQSourceStatus) MarkDeploying(reason, messageFormat string, messageA ...interface{}) {
	RocketMQSourceCondSet.Manage(s).MarkUnknown(RocketMQConditionDeployed, reason, messageFormat, messageA...)
}

// MarkNotDeployed sets the condition that the source has not been deployed.
func (s *RocketMQSourceStatus) MarkNotDeployed(reason, messageFormat string, messageA ...interface{}) {
	RocketMQSourceCondSet.Manage(s).MarkFalse(RocketMQConditionDeployed, reason, messageFormat, messageA...)
}

func (s *RocketMQSourceStatus) MarkSubscribed() {
	RocketMQSourceCondSet.Manage(s).MarkTrue(RocketMQConditionSubscribed)
}

// MarkEventTypes sets the condition that the source has created its event types.
func (s *RocketMQSourceStatus) MarkEventTypes() {
	RocketMQSourceCondSet.Manage(s).MarkTrue(RocketMQConditionEventTypesProvided)
}

// MarkNoEventTypes sets the condition that the source does not its event types configured.
func (s *RocketMQSourceStatus) MarkNoEventTypes(reason, messageFormat string, messageA ...interface{}) {
	RocketMQSourceCondSet.Manage(s).MarkFalse(RocketMQConditionEventTypesProvided, reason, messageFormat, messageA...)
}

// SecretValueFromSource represents the source of a secret value
type SecretValueFromSource struct {
	// The Secret key to select from.
	SecretKeyRef *corev1.SecretKeySelector `json:"secretKeyRef,omitempty"`
}

type Credentials struct {
	Url             string `json:"url"`
	AccessKeyId     string `json:"accessKeyId"`
	AccessKeySecret string `json:"accessKeySecret"`
}
