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

package sdk

import (
	"errors"
	"fmt"
	"reflect"
)

// StatusAccessor is the interface for a Resource that implements the getter and
// setter for accessing a Condition collection.
// +k8s:deepcopy-gen=true
type StatusAccessor interface {
	GetStatus() interface{}
	SetStatus(interface{})
}

// NewReflectedStatusAccessor uses reflection to return a StatusAccessor
// to access the field called "Status".
func NewReflectedStatusAccessor(object interface{}) (StatusAccessor, error) {
	objectValue := reflect.Indirect(reflect.ValueOf(object))

	// If object is not a struct, don't even try to use it.
	if objectValue.Kind() != reflect.Struct {
		return nil, errors.New("object is not a struct")
	}

	statusField := objectValue.FieldByName("Status")

	if statusField.IsValid() && statusField.CanInterface() && statusField.CanSet() {
		if _, ok := statusField.Interface().(interface{}); ok {
			return &reflectedStatusAccessor{
				status: statusField,
			}, nil
		}
	}
	return nil, fmt.Errorf("status was not an interface: %v", statusField.Kind())
}

// reflectedConditionsAccessor is an internal wrapper object to act as the
// ConditionsAccessor for status objects that do not implement ConditionsAccessor
// directly, but do expose the field using the "Conditions" field name.
type reflectedStatusAccessor struct {
	status reflect.Value
}

// GetConditions uses reflection to return Conditions from the held status object.
func (r *reflectedStatusAccessor) GetStatus() interface{} {
	if r != nil && r.status.IsValid() && r.status.CanInterface() {
		if status, ok := r.status.Interface().(interface{}); ok {
			return status
		}
	}
	return nil
}

// SetConditions uses reflection to set Conditions on the held status object.
func (r *reflectedStatusAccessor) SetStatus(status interface{}) {
	if r != nil && r.status.IsValid() && r.status.CanSet() {
		r.status.Set(reflect.ValueOf(status))
	}
}
