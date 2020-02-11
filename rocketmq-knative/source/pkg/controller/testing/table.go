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

package testing

import (
	"context"
	"fmt"
	"strings"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	"github.com/knative/eventing-contrib/pkg/controller/sdk"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/dynamic"
	dynamicfake "k8s.io/client-go/dynamic/fake"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/cache"
	"knative.dev/pkg/apis"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

// TestCase holds a single row of our table test.
type TestCase struct {
	// Name is a descriptive name for this test suitable as a first argument to t.Run()
	Name string

	// InitialState is the list of objects that already exists when reconciliation
	// starts.
	InitialState []runtime.Object

	Reconciles runtime.Object

	// ReconcileKey is the key of the object to reconcile in namespace/name form.
	ReconcileKey string

	// WantErr is true when we expect the Reconcile function to return an error.
	WantErr bool

	// WantErrMsg contains the pattern to match the returned error message.
	// Implies WantErr = true.
	WantErrMsg string

	// WantResult is the reconcile result we expect to be returned from the
	// Reconcile function.
	WantResult reconcile.Result

	// WantResultObject is the reconcile result we expect to be returned from the
	// Reconcile function.
	WantResultObject runtime.Object

	// WantPresent holds the non-exclusive set of objects we expect to exist
	// after reconciliation completes.
	WantPresent []runtime.Object

	// WantAbsent holds the list of objects expected to not exist
	// after reconciliation completes.
	WantAbsent []runtime.Object

	// Mocks that tamper with the client's responses.
	Mocks Mocks

	// Scheme for the dynamic client
	Scheme *runtime.Scheme

	// Fake dynamic objects
	Objects []runtime.Object

	// OtherTestData is arbitrary data needed for the test. It is not used directly by the table
	// testing framework. Instead it is used in the test method. E.g. setting up the responses for a
	// fake GCP PubSub client can go in here, as no other field makes sense for it.
	OtherTestData map[string]interface{}

	// IgnoreTimes causes comparisons to ignore fields of type apis.VolatileTime.
	IgnoreTimes bool
}

// Runner returns a testing func that can be passed to t.Run.
func (tc *TestCase) Runner(t *testing.T, r sdk.KnativeReconciler, c *MockClient) func(t *testing.T) {
	return func(t *testing.T) {
		result, recErr := tc.Reconcile(c, r)

		if err := tc.VerifyErr(recErr); err != nil {
			t.Error(err)
		}

		// Push back the reconciled changes into the client.
		if result != nil {
			c.Update(context.TODO(), result)
		}

		// Verifying should be done against the innerClient, never against mocks.
		c.stopMocking()

		if err := tc.VerifyWantPresent(c); err != nil {
			t.Error(err)
		}

		if err := tc.VerifyWantAbsent(c); err != nil {
			t.Error(err)
		}
	}
}

// GetDynamicClient returns the mockDynamicClient to use for this test case.
func (tc *TestCase) GetDynamicClient() dynamic.Interface {
	if tc.Scheme == nil {
		return dynamicfake.NewSimpleDynamicClient(runtime.NewScheme(), tc.Objects...)
	}
	return dynamicfake.NewSimpleDynamicClient(tc.Scheme, tc.Objects...)
}

// GetClient returns the mockClient to use for this test case.
func (tc *TestCase) GetClient() *MockClient {
	innerClient := fake.NewFakeClient(tc.InitialState...)
	return NewMockClient(innerClient, tc.Mocks)
}

// Reconcile calls the given reconciler's Reconcile() function with the test
// case's reconcile request.
func (tc *TestCase) Reconcile(c client.Client, r sdk.KnativeReconciler) (runtime.Object, error) {
	if tc.ReconcileKey == "" {
		return nil, fmt.Errorf("test did not set ReconcileKey")
	}
	ns, n, err := cache.SplitMetaNamespaceKey(tc.ReconcileKey)
	if err != nil {
		return nil, err
	}

	obj := tc.Reconciles.DeepCopyObject()
	err = c.Get(context.TODO(), client.ObjectKey{Namespace: ns, Name: n}, obj)
	if err != nil {
		// Not found is not an error.
		return nil, nil
	}

	return obj, r.Reconcile(context.TODO(), obj)
}

// VerifyErr verifies that the given error returned from Reconcile is the error
// expected by the test case.
func (tc *TestCase) VerifyErr(err error) error {
	// A non-empty WantErrMsg implies that an error is wanted.
	wantErr := tc.WantErr || tc.WantErrMsg != ""

	if wantErr && err == nil {
		return fmt.Errorf("want error, got nil")
	}

	if !wantErr && err != nil {
		return fmt.Errorf("want no error, got %v", err)
	}

	if err != nil {
		if diff := cmp.Diff(tc.WantErrMsg, err.Error()); diff != "" {
			return fmt.Errorf("incorrect error (-want, +got): %v", diff)
		}
	}
	return nil
}

// VerifyResult verifies that the given result returned from Reconcile is the
// result expected by the test case.
func (tc *TestCase) VerifyResult(result reconcile.Result) error {
	if diff := cmp.Diff(tc.WantResult, result); diff != "" {
		return fmt.Errorf("unexpected reconcile Result (-want +got) %v", diff)
	}
	return nil
}

// VerifyResult verifies that the given result returned from Reconcile is the
// result expected by the test case.
func (tc *TestCase) VerifyResultSDK(result runtime.Object) error {
	if diff := cmp.Diff(tc.WantResultObject, result); diff != "" {
		return fmt.Errorf("unexpected reconcile Result Object (-want +got) %v", diff)
	}
	return nil
}

type stateErrors struct {
	errors []error
}

func (se stateErrors) Error() string {
	msgs := make([]string, 0)
	for _, err := range se.errors {
		msgs = append(msgs, err.Error())
	}
	return strings.Join(msgs, "\n")
}

// VerifyWantPresent verifies that the client contains all the objects expected
// to be present after reconciliation.
func (tc *TestCase) VerifyWantPresent(c client.Client) error {
	var errs stateErrors
	for _, wp := range tc.WantPresent {
		o, err := scheme.Scheme.New(wp.GetObjectKind().GroupVersionKind())
		if err != nil {
			errs.errors = append(errs.errors, fmt.Errorf("error creating a copy of %T: %v", wp, err))
		}
		acc, err := meta.Accessor(wp)
		if err != nil {
			errs.errors = append(errs.errors, fmt.Errorf("error getting accessor for %#v %v", wp, err))
		}
		err = c.Get(context.TODO(), client.ObjectKey{Namespace: acc.GetNamespace(), Name: acc.GetName()}, o)
		if err != nil {
			if apierrors.IsNotFound(err) {
				errs.errors = append(errs.errors, fmt.Errorf("want present %T %s/%s, got absent", wp, acc.GetNamespace(), acc.GetName()))
			} else {
				errs.errors = append(errs.errors, fmt.Errorf("error getting %T %s/%s: %v", wp, acc.GetNamespace(), acc.GetName(), err))
			}
		}

		diffOpts := cmp.Options{
			// Ignore TypeMeta, since the objects created by the controller won't have
			// it
			cmpopts.IgnoreTypes(metav1.TypeMeta{}),
		}

		if tc.IgnoreTimes {
			// Ignore VolatileTime fields, since they rarely compare correctly.
			diffOpts = append(diffOpts, cmpopts.IgnoreTypes(apis.VolatileTime{}))
		}

		if diff := cmp.Diff(wp, o, diffOpts...); diff != "" {
			errs.errors = append(errs.errors, fmt.Errorf("Unexpected present %T %s/%s (-want +got):\n%v", wp, acc.GetNamespace(), acc.GetName(), diff))
		}
	}
	if len(errs.errors) > 0 {
		return errs
	}
	return nil
}

// VerifyWantAbsent verifies that the client does not contain any of the objects
// expected to be absent after reconciliation.
func (tc *TestCase) VerifyWantAbsent(c client.Client) error {
	var errs stateErrors
	for _, wa := range tc.WantAbsent {
		acc, err := meta.Accessor(wa)
		if err != nil {
			errs.errors = append(errs.errors, fmt.Errorf("error getting accessor for %#v %v", wa, err))
		}
		err = c.Get(context.TODO(), client.ObjectKey{Namespace: acc.GetNamespace(), Name: acc.GetName()}, wa)
		if err == nil {
			errs.errors = append(errs.errors, fmt.Errorf("want absent, got present %T %s/%s", wa, acc.GetNamespace(), acc.GetName()))
		}
		if !apierrors.IsNotFound(err) {
			errs.errors = append(errs.errors, fmt.Errorf("error getting %T %s/%s: %v", wa, acc.GetNamespace(), acc.GetName(), err))
		}
	}
	if len(errs.errors) > 0 {
		return errs
	}
	return nil
}
