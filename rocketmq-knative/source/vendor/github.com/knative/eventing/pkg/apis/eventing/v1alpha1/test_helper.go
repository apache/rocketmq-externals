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
	duckv1alpha1 "github.com/knative/eventing/pkg/apis/duck/v1alpha1"
	"knative.dev/pkg/apis"
	pkgduckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
	duckv1beta1 "knative.dev/pkg/apis/duck/v1beta1"
	v1 "k8s.io/api/apps/v1"
)

type testHelper struct{}

// TestHelper contains helpers for unit tests.
var TestHelper = testHelper{}

func (testHelper) ReadyChannelStatus() *ChannelStatus {
	cs := &ChannelStatus{}
	cs.MarkProvisionerInstalled()
	cs.MarkProvisioned()
	cs.SetAddress(&apis.URL{Scheme: "http", Host: "foo"})
	return cs
}

func (t testHelper) NotReadyChannelStatus() *ChannelStatus {
	cs := t.ReadyChannelStatus()
	cs.MarkNotProvisioned("foo", "bar")
	return cs
}

func (testHelper) ReadyChannelStatusCRD() *duckv1alpha1.ChannelableStatus {
	cs := &duckv1alpha1.ChannelableStatus{
		Status: duckv1beta1.Status{},
		AddressStatus: pkgduckv1alpha1.AddressStatus{
			Address: &pkgduckv1alpha1.Addressable{
				Addressable: duckv1beta1.Addressable{
					URL: &apis.URL{Scheme: "http", Host: "foo"},
				},
				Hostname: "foo",
			},
		},
		SubscribableTypeStatus: duckv1alpha1.SubscribableTypeStatus{}}
	return cs
}

func (t testHelper) NotReadyChannelStatusCRD() *duckv1alpha1.ChannelableStatus {
	return &duckv1alpha1.ChannelableStatus{}
}

func (testHelper) ReadySubscriptionStatus() *SubscriptionStatus {
	ss := &SubscriptionStatus{}
	ss.MarkChannelReady()
	ss.MarkReferencesResolved()
	ss.MarkAddedToChannel()
	return ss
}

func (testHelper) NotReadySubscriptionStatus() *SubscriptionStatus {
	ss := &SubscriptionStatus{}
	ss.MarkReferencesResolved()
	return ss
}

func (t testHelper) ReadyBrokerStatus() *BrokerStatus {
	bs := &BrokerStatus{}
	bs.PropagateIngressDeploymentAvailability(t.AvailableDeployment())
	bs.PropagateIngressChannelReadiness(t.ReadyChannelStatus())
	bs.PropagateTriggerChannelReadiness(t.ReadyChannelStatus())
	bs.PropagateIngressSubscriptionReadiness(t.ReadySubscriptionStatus())
	bs.PropagateFilterDeploymentAvailability(t.AvailableDeployment())
	bs.SetAddress(&apis.URL{Scheme: "http", Host: "foo"})
	return bs
}

func (t testHelper) ReadyBrokerStatusDeprecated() *BrokerStatus {
	bs := &BrokerStatus{}
	bs.MarkDeprecated("ClusterChannelProvisionerDeprecated", "Provisioners are deprecated and will be removed in 0.8. Recommended replacement is CRD based channels using spec.channelTemplateSpec.")
	bs.PropagateIngressDeploymentAvailability(t.AvailableDeployment())
	bs.PropagateIngressChannelReadiness(t.ReadyChannelStatus())
	bs.PropagateTriggerChannelReadiness(t.ReadyChannelStatus())
	bs.PropagateIngressSubscriptionReadiness(t.ReadySubscriptionStatus())
	bs.PropagateFilterDeploymentAvailability(t.AvailableDeployment())
	bs.SetAddress(&apis.URL{Scheme: "http", Host: "foo"})
	return bs
}

func (t testHelper) ReadyTriggerStatus() *TriggerStatus {
	ts := &TriggerStatus{}
	ts.InitializeConditions()
	ts.SubscriberURI = "http://foo/"
	ts.PropagateBrokerStatus(t.ReadyBrokerStatus())
	ts.PropagateSubscriptionStatus(t.ReadySubscriptionStatus())
	return ts
}

func (testHelper) UnavailableDeployment() *v1.Deployment {
	d := &v1.Deployment{}
	d.Name = "unavailable"
	d.Status.Conditions = []v1.DeploymentCondition{
		{
			Type:   v1.DeploymentAvailable,
			Status: "False",
		},
	}
	return d
}

func (t testHelper) AvailableDeployment() *v1.Deployment {
	d := t.UnavailableDeployment()
	d.Name = "available"
	d.Status.Conditions = []v1.DeploymentCondition{
		{
			Type:   v1.DeploymentAvailable,
			Status: "True",
		},
	}
	return d
}
