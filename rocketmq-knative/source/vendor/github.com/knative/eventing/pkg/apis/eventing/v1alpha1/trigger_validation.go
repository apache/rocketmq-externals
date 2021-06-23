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
	"context"

	"knative.dev/pkg/apis"
	"knative.dev/pkg/kmp"
)

func (t *Trigger) Validate(ctx context.Context) *apis.FieldError {
	return t.Spec.Validate(ctx).ViaField("spec")
}

func (ts *TriggerSpec) Validate(ctx context.Context) *apis.FieldError {
	var errs *apis.FieldError
	if ts.Broker == "" {
		fe := apis.ErrMissingField("broker")
		errs = errs.Also(fe)
	}

	if ts.Filter == nil {
		fe := apis.ErrMissingField("filter")
		errs = errs.Also(fe)
	}

	if ts.Filter != nil && ts.Filter.SourceAndType == nil {
		fe := apis.ErrMissingField("filter.sourceAndType")
		errs = errs.Also(fe)
	}

	if isSubscriberSpecNilOrEmpty(ts.Subscriber) {
		fe := apis.ErrMissingField("subscriber")
		errs = errs.Also(fe)
	} else if fe := IsValidSubscriberSpec(*ts.Subscriber); fe != nil {
		errs = errs.Also(fe.ViaField("subscriber"))
	}

	return errs
}

func (t *Trigger) CheckImmutableFields(ctx context.Context, og apis.Immutable) *apis.FieldError {
	if og == nil {
		return nil
	}

	original, ok := og.(*Trigger)
	if !ok {
		return &apis.FieldError{Message: "The provided original was not a Trigger"}
	}

	if diff, err := kmp.ShortDiff(original.Spec.Broker, t.Spec.Broker); err != nil {
		return &apis.FieldError{
			Message: "Failed to diff Trigger",
			Paths:   []string{"spec"},
			Details: err.Error(),
		}
	} else if diff != "" {
		return &apis.FieldError{
			Message: "Immutable fields changed (-old +new)",
			Paths:   []string{"spec", "broker"},
			Details: diff,
		}
	}
	return nil
}
