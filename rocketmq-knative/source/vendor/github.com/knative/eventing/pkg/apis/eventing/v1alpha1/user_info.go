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

	"github.com/knative/eventing/pkg/apis/eventing"
	"knative.dev/pkg/apis"
	"k8s.io/apimachinery/pkg/api/equality"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type HasSpec interface {
	// GetSpec returns the spec of the resource.
	GetSpec() interface{}
}

// setUserInfoAnnotations sets creator and updater annotations on a resource.
func setUserInfoAnnotations(resource HasSpec, ctx context.Context) {
	if ui := apis.GetUserInfo(ctx); ui != nil {
		objectMetaAccessor, ok := resource.(metav1.ObjectMetaAccessor)
		if !ok {
			return
		}

		annotations := objectMetaAccessor.GetObjectMeta().GetAnnotations()
		if annotations == nil {
			annotations = map[string]string{}
			defer objectMetaAccessor.GetObjectMeta().SetAnnotations(annotations)
		}

		if apis.IsInUpdate(ctx) {
			old := apis.GetBaseline(ctx).(HasSpec)
			if equality.Semantic.DeepEqual(old.GetSpec(), resource.GetSpec()) {
				return
			}
			annotations[eventing.UpdaterAnnotation] = ui.Username
		} else {
			annotations[eventing.CreatorAnnotation] = ui.Username
			annotations[eventing.UpdaterAnnotation] = ui.Username
		}
	}
}
