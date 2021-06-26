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

	eventingv1alpha1 "github.com/knative/eventing/pkg/apis/eventing/v1alpha1"
	"knative.dev/pkg/apis"
	"k8s.io/apimachinery/pkg/api/equality"
)

func (p *Sequence) Validate(ctx context.Context) *apis.FieldError {
	return p.Spec.Validate(ctx).ViaField("spec")
}

func (ps *SequenceSpec) Validate(ctx context.Context) *apis.FieldError {
	var errs *apis.FieldError

	if len(ps.Steps) == 0 {
		errs = errs.Also(apis.ErrMissingField("steps"))
	}

	for i, s := range ps.Steps {
		if e := eventingv1alpha1.IsValidSubscriberSpec(s); e != nil {
			errs = errs.Also(apis.ErrInvalidArrayValue(s, "steps", i))
		}
	}

	if equality.Semantic.DeepEqual(ps.ChannelTemplate, ChannelTemplateSpec{}) {
		errs = errs.Also(apis.ErrMissingField("channelTemplate"))
		return errs
	}

	if len(ps.ChannelTemplate.APIVersion) == 0 {
		errs = errs.Also(apis.ErrMissingField("channelTemplate.apiVersion"))
	}

	if len(ps.ChannelTemplate.Kind) == 0 {
		errs = errs.Also(apis.ErrMissingField("channelTemplate.kind"))
	}
	if ps.Reply != nil {
		if err := eventingv1alpha1.IsValidObjectReference(*ps.Reply); err != nil {
			errs = errs.Also(err.ViaField("reply"))
		}
	}
	return errs
}
