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
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/equality"
)

func (s *Subscription) Validate(ctx context.Context) *apis.FieldError {
	return s.Spec.Validate(ctx).ViaField("spec")
}

func (ss *SubscriptionSpec) Validate(ctx context.Context) *apis.FieldError {
	// We require always Channel.
	// Also at least one of 'subscriber' and 'reply' must be defined (non-nil and non-empty).

	var errs *apis.FieldError
	if isChannelEmpty(ss.Channel) {
		fe := apis.ErrMissingField("channel")
		fe.Details = "the Subscription must reference a channel"
		return fe
	} else if fe := isValidChannel(ss.Channel); fe != nil {
		errs = errs.Also(fe.ViaField("channel"))
	}

	missingSubscriber := isSubscriberSpecNilOrEmpty(ss.Subscriber)
	missingReply := isReplyStrategyNilOrEmpty(ss.Reply)
	if missingSubscriber && missingReply {
		fe := apis.ErrMissingField("reply", "subscriber")
		fe.Details = "the Subscription must reference at least one of (reply or a subscriber)"
		errs = errs.Also(fe)
	}

	if !missingSubscriber {
		if fe := IsValidSubscriberSpec(*ss.Subscriber); fe != nil {
			errs = errs.Also(fe.ViaField("subscriber"))
		}
	}

	if !missingReply {
		if fe := isValidReply(*ss.Reply); fe != nil {
			errs = errs.Also(fe.ViaField("reply"))
		}
	}

	return errs
}

func isSubscriberSpecNilOrEmpty(s *SubscriberSpec) bool {
	if s == nil || equality.Semantic.DeepEqual(s, &SubscriberSpec{}) {
		return true
	}
	if equality.Semantic.DeepEqual(s.Ref, &corev1.ObjectReference{}) &&
		s.DeprecatedDNSName == nil &&
		s.URI == nil {
		return true
	}
	return false
}

func IsValidSubscriberSpec(s SubscriberSpec) *apis.FieldError {
	var errs *apis.FieldError

	fieldsSet := make([]string, 0, 0)
	if s.Ref != nil && !equality.Semantic.DeepEqual(s.Ref, &corev1.ObjectReference{}) {
		fieldsSet = append(fieldsSet, "ref")
	}
	if s.DeprecatedDNSName != nil && *s.DeprecatedDNSName != "" {
		fieldsSet = append(fieldsSet, "dnsName")
	}
	if s.URI != nil && *s.URI != "" {
		fieldsSet = append(fieldsSet, "uri")
	}
	if len(fieldsSet) == 0 {
		errs = errs.Also(apis.ErrMissingOneOf("ref", "dnsName", "uri"))
	} else if len(fieldsSet) > 1 {
		errs = errs.Also(apis.ErrMultipleOneOf(fieldsSet...))
	}

	// If Ref given, check the fields.
	if s.Ref != nil && !equality.Semantic.DeepEqual(s.Ref, &corev1.ObjectReference{}) {
		fe := IsValidObjectReference(*s.Ref)
		if fe != nil {
			errs = errs.Also(fe.ViaField("ref"))
		}
	}
	return errs
}

func isReplyStrategyNilOrEmpty(r *ReplyStrategy) bool {
	return r == nil || equality.Semantic.DeepEqual(r, &ReplyStrategy{}) || equality.Semantic.DeepEqual(r.Channel, &corev1.ObjectReference{})
}

func isValidReply(r ReplyStrategy) *apis.FieldError {
	if fe := IsValidObjectReference(*r.Channel); fe != nil {
		return fe.ViaField("channel")
	}
	return nil
}

func (s *Subscription) CheckImmutableFields(ctx context.Context, og apis.Immutable) *apis.FieldError {
	original, ok := og.(*Subscription)
	if !ok {
		return &apis.FieldError{Message: "The provided original was not a Subscription"}
	}
	if original == nil {
		return nil
	}

	// Only Subscriber and Reply are mutable.
	ignoreArguments := cmpopts.IgnoreFields(SubscriptionSpec{}, "Subscriber", "Reply")
	if diff, err := kmp.ShortDiff(original.Spec, s.Spec, ignoreArguments); err != nil {
		return &apis.FieldError{
			Message: "Failed to diff Subscription",
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
