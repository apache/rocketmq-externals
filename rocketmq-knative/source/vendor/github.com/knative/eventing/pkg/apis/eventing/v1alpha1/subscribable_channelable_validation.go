/*
Copyright 2018 The Knative Authors

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
	"reflect"

	"github.com/google/go-cmp/cmp"
	"knative.dev/pkg/apis"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/equality"
)

func isChannelEmpty(f corev1.ObjectReference) bool {
	return equality.Semantic.DeepEqual(f, corev1.ObjectReference{})
}

// Valid if it is a valid object reference.
func isValidChannel(f corev1.ObjectReference) *apis.FieldError {
	return IsValidObjectReference(f)
}

func IsValidObjectReference(f corev1.ObjectReference) *apis.FieldError {
	return checkRequiredObjectReferenceFields(f).
		Also(checkDisallowedObjectReferenceFields(f))
}

// Check the corev1.ObjectReference to make sure it has the required fields. They
// are not checked for anything more except that they are set.
func checkRequiredObjectReferenceFields(f corev1.ObjectReference) *apis.FieldError {
	var errs *apis.FieldError
	if f.Name == "" {
		errs = errs.Also(apis.ErrMissingField("name"))
	}
	if f.APIVersion == "" {
		errs = errs.Also(apis.ErrMissingField("apiVersion"))
	}
	if f.Kind == "" {
		errs = errs.Also(apis.ErrMissingField("kind"))
	}
	return errs
}

// Check the corev1.ObjectReference to make sure it only has the following fields set:
// Name, Kind, APIVersion
// If any other fields are set and is not the Zero value, returns an apis.FieldError
// with the fieldpaths for all those fields.
func checkDisallowedObjectReferenceFields(f corev1.ObjectReference) *apis.FieldError {
	disallowedFields := []string{}
	// See if there are any fields that have been set that should not be.
	// TODO: Hoist this kind of stuff into pkg repository.
	s := reflect.ValueOf(f)
	typeOf := s.Type()
	for i := 0; i < s.NumField(); i++ {
		field := s.Field(i)
		fieldName := typeOf.Field(i).Name
		if fieldName == "Name" || fieldName == "Kind" || fieldName == "APIVersion" {
			continue
		}
		if !cmp.Equal(field.Interface(), reflect.Zero(field.Type()).Interface()) {
			disallowedFields = append(disallowedFields, fieldName)
		}
	}
	if len(disallowedFields) > 0 {
		fe := apis.ErrDisallowedFields(disallowedFields...)
		fe.Details = "only name, apiVersion and kind are supported fields"
		return fe
	}
	return nil

}
