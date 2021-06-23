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

package sinks

import (
	"context"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes/scheme"
	duckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
)

var (
	addressableDNS = "addressable.sink.svc.cluster.local"
	addressableURL = fmt.Sprintf("http://%s", addressableDNS)

	addressableName       = "testsink"
	addressableKind       = "Sink"
	addressableAPIVersion = "duck.knative.dev/v1alpha1"

	serviceName       = "test-service"
	serviceKind       = "Service"
	serviceAPIVersion = "v1"
	serviceDNSName    = "http://test-service.testnamespace.svc/"

	unaddressableName       = "testunaddressable"
	unaddressableKind       = "KResource"
	unaddressableAPIVersion = "duck.knative.dev/v1alpha1"
	unaddressableResource   = "kresources.duck.knative.dev"

	testNS = "testnamespace"
)

func init() {
	// Add types to scheme
	_ = duckv1alpha1.AddToScheme(scheme.Scheme)
}

func TestGetSinkURI(t *testing.T) {
	testCases := map[string]struct {
		objects   []runtime.Object
		namespace string
		want      string
		wantErr   error
		ref       *corev1.ObjectReference
	}{
		"happy - hostname": {
			objects: []runtime.Object{
				getAddressableWithHostname(),
			},
			namespace: testNS,
			ref:       getAddressableRef(),
			want:      addressableURL,
		},
		"happy - uri": {
			objects: []runtime.Object{
				getAddressable(),
			},
			namespace: testNS,
			ref:       getAddressableRef(),
			want:      addressableURL,
		},
		"nil hostname": {
			objects: []runtime.Object{
				getAddressableNilHostname(),
			},
			namespace: testNS,
			ref:       getUnaddressableRef(),
			wantErr:   fmt.Errorf(`sink "testnamespace/testunaddressable" (duck.knative.dev/v1alpha1, Kind=KResource) contains an empty hostname`),
		},
		"nil sink": {
			objects: []runtime.Object{
				getAddressableNilHostname(),
			},
			namespace: testNS,
			ref:       nil,
			wantErr:   fmt.Errorf(`sink ref is nil`),
		},
		"v1Service": {
			objects: []runtime.Object{
				v1Service(),
			},
			namespace: testNS,
			ref:       getServiceRef(),
			want:      serviceDNSName,
		},
		"nil address": {
			objects: []runtime.Object{
				getAddressableNilAddress(),
			},
			namespace: testNS,
			ref:       nil,
			wantErr:   fmt.Errorf(`sink ref is nil`),
		},
		"notSink": {
			objects: []runtime.Object{
				getAddressableNoStatus(),
			},
			namespace: testNS,
			ref:       getUnaddressableRef(),
			wantErr:   fmt.Errorf(`sink "testnamespace/testunaddressable" (duck.knative.dev/v1alpha1, Kind=KResource) does not contain address`),
		},
		"notFound": {
			namespace: testNS,
			ref:       getUnaddressableRef(),
			wantErr:   fmt.Errorf(`%s "%s" not found`, unaddressableResource, unaddressableName),
		},
	}
	for n, tc := range testCases {
		t.Run(n, func(t *testing.T) {
			ctx := context.Background()
			client := fake.NewFakeClient(tc.objects...)
			uri, gotErr := GetSinkURI(ctx, client, tc.ref, tc.namespace)
			if gotErr != nil {
				if tc.wantErr != nil {
					if diff := cmp.Diff(tc.wantErr.Error(), gotErr.Error()); diff != "" {
						t.Errorf("%s: unexpected error (-want, +got) = %v", n, diff)
					}
				} else {
					t.Errorf("%s: unexpected error %v", n, gotErr.Error())
				}
			}
			if gotErr == nil {
				got := uri
				if diff := cmp.Diff(tc.want, got); diff != "" {
					t.Errorf("%s: unexpected object (-want, +got) = %v", n, diff)
				}
			}
		})
	}
}

func v1Service() *unstructured.Unstructured {
	return &unstructured.Unstructured{
		Object: map[string]interface{}{
			"apiVersion": serviceAPIVersion,
			"kind":       serviceKind,
			"metadata": map[string]interface{}{
				"namespace": testNS,
				"name":      serviceName,
			},
		},
	}
}

func getAddressable() *unstructured.Unstructured {
	return &unstructured.Unstructured{
		Object: map[string]interface{}{
			"apiVersion": addressableAPIVersion,
			"kind":       addressableKind,
			"metadata": map[string]interface{}{
				"namespace": testNS,
				"name":      addressableName,
			},
			"status": map[string]interface{}{
				"address": map[string]interface{}{
					"url": addressableURL,
				},
			},
		},
	}
}

func getAddressableWithHostname() *unstructured.Unstructured {
	a := getAddressable()
	a.Object["status"] = map[string]interface{}{
		"address": map[string]interface{}{
			"hostname": addressableDNS,
		},
	}
	return a
}

func getAddressableNoStatus() *unstructured.Unstructured {
	return &unstructured.Unstructured{
		Object: map[string]interface{}{
			"apiVersion": unaddressableAPIVersion,
			"kind":       unaddressableKind,
			"metadata": map[string]interface{}{
				"namespace": testNS,
				"name":      unaddressableName,
			},
		},
	}
}

func getAddressableNilAddress() *unstructured.Unstructured {
	return &unstructured.Unstructured{
		Object: map[string]interface{}{
			"apiVersion": unaddressableAPIVersion,
			"kind":       unaddressableKind,
			"metadata": map[string]interface{}{
				"namespace": testNS,
				"name":      unaddressableName,
			},
			"status": map[string]interface{}{
				"address": map[string]interface{}(nil),
			},
		},
	}
}

func getAddressableNilHostname() *unstructured.Unstructured {
	return &unstructured.Unstructured{
		Object: map[string]interface{}{
			"apiVersion": unaddressableAPIVersion,
			"kind":       unaddressableKind,
			"metadata": map[string]interface{}{
				"namespace": testNS,
				"name":      unaddressableName,
			},
			"status": map[string]interface{}{
				"address": map[string]interface{}{
					"hostname": nil,
				},
			},
		},
	}
}

func getServiceRef() *corev1.ObjectReference {
	return &corev1.ObjectReference{
		Kind:       serviceKind,
		Name:       serviceName,
		APIVersion: serviceAPIVersion,
	}
}

func getAddressableRef() *corev1.ObjectReference {
	return &corev1.ObjectReference{
		Kind:       addressableKind,
		Name:       addressableName,
		APIVersion: addressableAPIVersion,
	}
}

func getUnaddressableRef() *corev1.ObjectReference {
	return &corev1.ObjectReference{
		Kind:       unaddressableKind,
		Name:       unaddressableName,
		APIVersion: unaddressableAPIVersion,
	}
}
