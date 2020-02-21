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
package resources

import (
	"fmt"

	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/apis/sources/v1alpha1"
	v1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// ReceiveAdapterArgs are the arguments needed to create a RocketMQ  Source Receive Adapter. Every
// field is required.
type ReceiveAdapterArgs struct {
	Image          string
	Source         *v1alpha1.RocketMQSource
	Labels         map[string]string
	SubscriptionID string
	SinkURI        string
//	TransformerURI string
}

// MakeReceiveAdapter generates (but does not insert into K8s) the Receive Adapter Deployment for
func MakeReceiveAdapter(args *ReceiveAdapterArgs) *v1.Deployment {
	replicas := int32(1)
	secretName := ""
	seretKey := ""
	if(args.Source.Spec.AccessToken.SecretKeyRef != nil){
		secretName = args.Source.Spec.AccessToken.SecretKeyRef.Name
		seretKey = args.Source.Spec.AccessToken.SecretKeyRef.Key
	}

	return &v1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Namespace:    args.Source.Namespace,
			GenerateName: fmt.Sprintf("rocketmq-%s-", args.Source.Name),
			Labels:       args.Labels,
		},
		Spec: v1.DeploymentSpec{
			Selector: &metav1.LabelSelector{
				MatchLabels: args.Labels,
			},
			Replicas: &replicas,
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Annotations: map[string]string{
						"sidecar.istio.io/inject": "true",
					},
					Labels: args.Labels,
				},
				Spec: corev1.PodSpec{
					ServiceAccountName: args.Source.Spec.ServiceAccountName,
					Containers: []corev1.Container{
						{
							Name:  "receive-adapter",
							Image: args.Image,
							Env: []corev1.EnvVar{
								{
									Name:  "SECRET_NAME",
									Value:secretName,
								},
								{
									Name:  "SECRET_KEY",
									Value:seretKey,
								},
								{
									Name: "NAMESPACE",
									ValueFrom: &corev1.EnvVarSource{
										FieldRef: &corev1.ObjectFieldSelector{
											FieldPath: "metadata.namespace",
										},
									},
								},
								{
									Name:  "TOPIC",
									Value: args.Source.Spec.Topic,
								},
								{
									Name:  "SUBSCRIPTION_ID",
									Value: args.SubscriptionID,
								},
								{
									Name:  "SINK_URI",
									Value: args.SinkURI,
								},
								{
									Name:  "NAMESRVADDR",
									Value: args.Source.Spec.NamesrvAddr,
								},
								{
									Name:  "RNAMESPACE",
									Value: args.Source.Spec.Namespace,
								},
								{
									Name:  "GROUPNAME",
									Value: args.Source.Spec.GroupName,
								},
							},
						},
					},
				},
			},
		},
	}
}
