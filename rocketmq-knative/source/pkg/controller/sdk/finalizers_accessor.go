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
	"k8s.io/apimachinery/pkg/util/sets"
)

// FinalizersAccessor is the interface for a Resource that implements the getter and setting for
// accessing its Finalizer set.
// +k8s:deepcopy-gen=true
type FinalizersAccessor interface {
	GetFinalizers() sets.String
	SetFinalizers(finalizers sets.String)
}

// NewReflectedFinalizersAccessor uses reflection to return a FinalizersAccessor to access the field
// called "Finalizers".
func NewReflectedFinalizersAccessor(object interface{}) (FinalizersAccessor, error) {
	objectValue := reflect.Indirect(reflect.ValueOf(object))

	// If object is not a struct, don't even try to use it.
	if objectValue.Kind() != reflect.Struct {
		return nil, errors.New("object is not a struct")
	}

	finalizersField := objectValue.FieldByName("Finalizers")
	if finalizersField.IsValid() && finalizersField.CanSet() && finalizersField.Kind() == reflect.Slice {
		finalizers := sets.NewString()
		for i := 0; i < finalizersField.Len(); i++ {
			finalizer := finalizersField.Index(i)
			if finalizer.IsValid() && finalizer.Kind() == reflect.String {
				finalizers.Insert(finalizer.String())
			} else {
				return nil, fmt.Errorf("element in the Finalizer slice was not a string: %v", finalizer.Kind())
			}
		}
		return &reflectedFinalizersAccessor{
			finalizersField: finalizersField,
			finalizersSet:   finalizers,
		}, nil
	}

	return nil, fmt.Errorf("finalizer was not a slice: %v", finalizersField.Kind())
}

// reflectedFinalizersAccessor is an internal wrapper object to act as the FinalizersAccessor for
// objects that do not implement FinalizersAccessor directly, but do expose the field using the
// name "Finalizers".
type reflectedFinalizersAccessor struct {
	finalizersField reflect.Value
	finalizersSet   sets.String
}

// GetFinalizers uses reflection to return the Finalizers set from the held object.
func (r *reflectedFinalizersAccessor) GetFinalizers() sets.String {
	return r.finalizersSet
}

// SetFinalizers uses reflection to set Finalizers on the held object.
func (r *reflectedFinalizersAccessor) SetFinalizers(finalizers sets.String) {
	r.finalizersSet = finalizers
	r.finalizersField.Set(reflect.ValueOf(finalizers.List()))
}
