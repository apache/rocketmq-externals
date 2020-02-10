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
	"context"

	"go.uber.org/zap"

	"k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/record"
	"knative.dev/pkg/logging"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/runtime/inject"
)

type Reconciler struct {
	client   client.Client
	recorder record.EventRecorder
	scheme   *runtime.Scheme
	logger   zap.SugaredLogger

	provider Provider
}

// Verify the struct implements reconcile.Reconciler
var _ reconcile.Reconciler = &Reconciler{}

// Reconcile compares the actual state with the desired, and attempts to
// converge the two.
func (r *Reconciler) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	ctx := logging.WithLogger(context.TODO(), r.logger.With(zap.Any("request", request)))
	logger := logging.FromContext(ctx)

	logger.Infof("Reconciling %s %v", r.provider.Parent.GetObjectKind(), request)

	original := r.provider.Parent.DeepCopyObject()

	err := r.client.Get(context.TODO(), request.NamespacedName, original)

	if errors.IsNotFound(err) {
		logger.Errorf("could not find %s %v\n", r.provider.Parent.GetObjectKind(), request)
		return reconcile.Result{}, nil
	}

	if err != nil {
		logger.Errorf("could not fetch %s %v for %+v\n", r.provider.Parent.GetObjectKind(), err, request)
		return reconcile.Result{}, err
	}

	// Don't modify the cache's copy
	obj := original.DeepCopyObject()

	// Reconcile this copy of the Source and then write back any status
	// updates regardless of whether the reconcile error out.
	reconcileErr := r.provider.Reconciler.Reconcile(ctx, obj)
	if reconcileErr != nil {
		logger.Warnf("Failed to reconcile %s: %v", r.provider.Parent.GetObjectKind(), reconcileErr)
	}

	if needsUpdate, err := r.needsUpdate(ctx, original, obj); err != nil {
		logger.Desugar().Error("Unable to determine if an update is needed", zap.Error(err), zap.Any("original", original), zap.Any("obj", obj))
		return reconcile.Result{}, err
	} else if needsUpdate {
		if _, err := r.update(ctx, request, obj); err != nil {
			logger.Desugar().Error("Failed to update", zap.Error(err), zap.Any("objectKind", r.provider.Parent.GetObjectKind()))
			return reconcile.Result{}, err
		}
	}

	// Requeue if the resource is not ready:
	return reconcile.Result{}, reconcileErr
}

func (r *Reconciler) InjectClient(c client.Client) error {
	r.client = c
	_, err := inject.ClientInto(c, r.provider.Reconciler)
	return err
}

func (r *Reconciler) InjectConfig(c *rest.Config) error {
	_, err := inject.ConfigInto(c, r.provider.Reconciler)
	return err
}

func (r *Reconciler) needsUpdate(ctx context.Context, old, new runtime.Object) (bool, error) {
	if old == nil {
		return true, nil
	}

	// Check Status.
	os, err := NewReflectedStatusAccessor(old)
	if err != nil {
		return false, err
	}
	ns, err := NewReflectedStatusAccessor(new)
	if err != nil {
		return false, err
	}
	oStatus := os.GetStatus()
	nStatus := ns.GetStatus()

	if !equality.Semantic.DeepEqual(oStatus, nStatus) {
		return true, nil
	}

	// Check finalizers.
	of, err := NewReflectedFinalizersAccessor(old)
	if err != nil {
		return false, err
	}
	nf, err := NewReflectedFinalizersAccessor(new)
	if err != nil {
		return false, err
	}
	oFinalizers := of.GetFinalizers()
	nFinalizers := nf.GetFinalizers()

	if !equality.Semantic.DeepEqual(oFinalizers, nFinalizers) {
		return true, nil
	}

	return false, nil
}

func (r *Reconciler) update(ctx context.Context, request reconcile.Request, object runtime.Object) (runtime.Object, error) {
	freshObj := r.provider.Parent.DeepCopyObject()
	if err := r.client.Get(ctx, request.NamespacedName, freshObj); err != nil {
		return nil, err
	}

	// Finalizers
	freshFinalizers, err := NewReflectedFinalizersAccessor(freshObj)
	if err != nil {
		return nil, err
	}
	orgFinalizers, err := NewReflectedFinalizersAccessor(object)
	if err != nil {
		return nil, err
	}
	freshFinalizers.SetFinalizers(orgFinalizers.GetFinalizers())

	if err := r.client.Update(ctx, freshObj); err != nil {
		return nil, err
	}

	// Refetch
	freshObj = r.provider.Parent.DeepCopyObject()
	if err := r.client.Get(ctx, request.NamespacedName, freshObj); err != nil {
		return nil, err
	}

	// Status
	freshStatus, err := NewReflectedStatusAccessor(freshObj)
	if err != nil {
		return nil, err
	}
	orgStatus, err := NewReflectedStatusAccessor(object)
	if err != nil {
		return nil, err
	}
	freshStatus.SetStatus(orgStatus.GetStatus())

	if err := r.client.Status().Update(ctx, freshObj); err != nil {
		return nil, err
	}

	return freshObj, nil
}
