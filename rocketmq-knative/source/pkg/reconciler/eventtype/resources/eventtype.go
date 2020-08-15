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
	"regexp"
	"strings"

	eventingv1alpha1 "github.com/knative/eventing/pkg/apis/eventing/v1alpha1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/validation"
)

const (
	// Number of characters to keep available just in case the prefix used in generateName
	// exceeds the maximum allowed for k8s names.
	generateNameSafety = 10
)

// Only allow alphanumeric, '-' or '.'.
var validChars = regexp.MustCompile(`[^-\.a-z0-9]+`)

// MakeEventType creates the in-memory representation of the EventType in the specified namespace, with the given spec,
// and label.
func MakeEventType(spec eventingv1alpha1.EventTypeSpec, namespace string, lbl map[string]string) eventingv1alpha1.EventType {
	return eventingv1alpha1.EventType{
		ObjectMeta: metav1.ObjectMeta{
			GenerateName: fmt.Sprintf("%s-", toDNS1123Subdomain(spec.Type)),
			Labels:       lbl,
			Namespace:    namespace,
		},
		Spec: spec,
	}
}

// Converts 'name' to a valid DNS1123 subdomain, required for object names in K8s.
func toDNS1123Subdomain(name string) string {
	// If it is not a valid DNS1123 subdomain, make it a valid one.
	if msgs := validation.IsDNS1123Subdomain(name); len(msgs) != 0 {
		// If the length exceeds the max, cut it and leave some room for a potential generated UUID.
		if len(name) > validation.DNS1123SubdomainMaxLength {
			name = name[:validation.DNS1123SubdomainMaxLength-generateNameSafety]
		}
		name = strings.ToLower(name)
		name = validChars.ReplaceAllString(name, "")
		// Only start/end with alphanumeric.
		name = strings.Trim(name, "-.")
	}
	return name
}
