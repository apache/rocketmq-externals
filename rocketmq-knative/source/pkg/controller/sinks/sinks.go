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

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"knative.dev/pkg/apis/duck"
	duckv1alpha1 "knative.dev/pkg/apis/duck/v1alpha1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// GetSinkURI retrieves the sink URI from the object referenced by the given
// ObjectReference.
func GetSinkURI(ctx context.Context, c client.Client, sink *corev1.ObjectReference, namespace string) (string, error) {
	if sink == nil {
		return "", fmt.Errorf("sink ref is nil")
	}

	u := &unstructured.Unstructured{}
	u.SetGroupVersionKind(sink.GroupVersionKind())
	err := c.Get(ctx, client.ObjectKey{Namespace: namespace, Name: sink.Name}, u)
	if err != nil {
		return "", err
	}

	objIdentifier := fmt.Sprintf("\"%s/%s\" (%s)", u.GetNamespace(), u.GetName(), u.GroupVersionKind())
	// Special case v1/Service to allow it be addressable
	if u.GroupVersionKind().Kind == "Service" && u.GroupVersionKind().Version == "v1" {
		return fmt.Sprintf("http://%s.%s.svc/", u.GetName(), u.GetNamespace()), nil
	}

	t := duckv1alpha1.AddressableType{}
	err = duck.FromUnstructured(u, &t)
	if err != nil {
		return "", fmt.Errorf("failed to deserialize sink %s: %v", objIdentifier, err)
	}

	if t.Status.Address == nil {
		return "", fmt.Errorf("sink %s does not contain address", objIdentifier)
	}

	url := t.Status.Address.GetURL()
	if url.Host == "" {
		return "", fmt.Errorf("sink %s contains an empty hostname", objIdentifier)
	}
	return url.String(), nil
}
