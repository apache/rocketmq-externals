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
	"context"

	"github.com/google/go-cmp/cmp/cmpopts"

	"knative.dev/pkg/apis"
	"knative.dev/pkg/kmp"
)

func (et *EventType) Validate(ctx context.Context) *apis.FieldError {
	return et.Spec.Validate(ctx).ViaField("spec")
}

func (ets *EventTypeSpec) Validate(ctx context.Context) *apis.FieldError {
	var errs *apis.FieldError
	if ets.Type == "" {
		fe := apis.ErrMissingField("type")
		errs = errs.Also(fe)
	}
	if ets.Source == "" {
		// TODO validate is a valid URI.
		fe := apis.ErrMissingField("source")
		errs = errs.Also(fe)
	}
	if ets.Broker == "" {
		fe := apis.ErrMissingField("broker")
		errs = errs.Also(fe)
	}
	// TODO validate Schema is a valid URI.
	return errs
}

func (et *EventType) CheckImmutableFields(ctx context.Context, og apis.Immutable) *apis.FieldError {
	if og == nil {
		return nil
	}

	original, ok := og.(*EventType)
	if !ok {
		return &apis.FieldError{Message: "The provided original was not an EventType"}
	}

	// All but Description field immutable.
	ignoreArguments := cmpopts.IgnoreFields(EventTypeSpec{}, "Description")
	if diff, err := kmp.ShortDiff(original.Spec, et.Spec, ignoreArguments); err != nil {
		return &apis.FieldError{
			Message: "Failed to diff EventType",
			Paths:   []string{"spec"},
			Details: err.Error(),
		}
	} else if diff != "" {
		return &apis.FieldError{
			Message: "Immutable fields changed (-old +new)",
			Paths:   []string{"spec"},
			Details: diff,
		}
	}
	return nil
}
