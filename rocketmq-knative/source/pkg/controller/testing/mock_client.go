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

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type MockHandled int

const (
	// This mock has handled the function call, no further mocks nor the real client should be
	// called.
	Handled MockHandled = iota
	// This mock has not handled the function call, subsequent mocks or the real client should be
	// called.
	Unhandled
)

// All of the funcions in client.Client get mocked equivalents. For the function
// client.Client.Foo(), the mocked equivalent will be:
// func(innerClient client.Client[, arguments to Foo()]) (MockHandled[, returns from Foo()])

type MockGet func(innerClient client.Client, ctx context.Context, key client.ObjectKey, obj runtime.Object) (MockHandled, error)
type MockList func(innerClient client.Client, ctx context.Context, opts *client.ListOptions, list runtime.Object) (MockHandled, error)
type MockCreate func(innerClient client.Client, ctx context.Context, obj runtime.Object) (MockHandled, error)
type MockDelete func(innerClient client.Client, ctx context.Context, obj runtime.Object) (MockHandled, error)
type MockUpdate func(innerClient client.Client, ctx context.Context, obj runtime.Object) (MockHandled, error)

var _ client.Client = (*MockClient)(nil)

// mockClient is a client.Client that allows mock responses to be returned, instead of calling the
// inner client.Client.
type MockClient struct {
	innerClient client.Client
	mocks       Mocks
}

// The mocks to run on each function type. Each function will run through the mocks in its list
// until one responds with 'Handled'. If there is more than one mock in the list, then the one that
// responds 'Handled' will be removed and not run on subsequent calls to the function. If no mocks
// respond 'Handled', then the real underlying client is called.
type Mocks struct {
	MockGets    []MockGet
	MockLists   []MockList
	MockCreates []MockCreate
	MockDeletes []MockDelete
	MockUpdates []MockUpdate
}

func NewMockClient(innerClient client.Client, mocks Mocks) *MockClient {
	return &MockClient{
		innerClient: innerClient,
		mocks:       mocks,
	}
}

func (m *MockClient) stopMocking() {
	m.mocks = Mocks{}
}

// All of the functions are handled almost identically:
// 1. Run through the mocks in order:
//   a. If the mock handled the request, then:
//      i. If there is at least one other mock in the list, remove this mock.
//      ii. Return the response from the mock.
// 2. No mock handled the request, so call the inner client.

func (m *MockClient) Get(ctx context.Context, key client.ObjectKey, obj runtime.Object) error {
	for i, mockGet := range m.mocks.MockGets {
		handled, err := mockGet(m.innerClient, ctx, key, obj)
		if handled == Handled {
			if len(m.mocks.MockGets) > 1 {
				m.mocks.MockGets = append(m.mocks.MockGets[:i], m.mocks.MockGets[i+1:]...)
			}
			return err
		}
	}
	return m.innerClient.Get(ctx, key, obj)
}

func (m *MockClient) List(ctx context.Context, opts *client.ListOptions, list runtime.Object) error {
	for i, mockList := range m.mocks.MockLists {
		handled, err := mockList(m.innerClient, ctx, opts, list)
		if handled == Handled {
			if len(m.mocks.MockLists) > 1 {
				m.mocks.MockLists = append(m.mocks.MockLists[:i], m.mocks.MockLists[i+1:]...)
			}
			return err
		}
	}
	return m.innerClient.List(ctx, opts, list)
}

func (m *MockClient) Create(ctx context.Context, obj runtime.Object) error {
	for i, mockCreate := range m.mocks.MockCreates {
		handled, err := mockCreate(m.innerClient, ctx, obj)
		if handled == Handled {
			if len(m.mocks.MockCreates) > 1 {
				m.mocks.MockCreates = append(m.mocks.MockCreates[:i], m.mocks.MockCreates[i+1:]...)
			}
			return err
		}
	}
	return m.innerClient.Create(ctx, obj)
}

func (m *MockClient) Delete(ctx context.Context, obj runtime.Object, opts ...client.DeleteOptionFunc) error {
	for i, mockDelete := range m.mocks.MockDeletes {
		handled, err := mockDelete(m.innerClient, ctx, obj)
		if handled == Handled {
			if len(m.mocks.MockDeletes) > 1 {
				m.mocks.MockDeletes = append(m.mocks.MockDeletes[:i], m.mocks.MockDeletes[i+1:]...)
			}
			return err
		}
	}
	return m.innerClient.Delete(ctx, obj, opts...)
}

func (m *MockClient) Update(ctx context.Context, obj runtime.Object) error {
	for i, mockUpdate := range m.mocks.MockUpdates {
		handled, err := mockUpdate(m.innerClient, ctx, obj)
		if handled == Handled {
			if len(m.mocks.MockUpdates) > 1 {
				m.mocks.MockUpdates = append(m.mocks.MockUpdates[:i], m.mocks.MockUpdates[i+1:]...)
			}
			return err
		}
	}
	return m.innerClient.Update(ctx, obj)
}

func (m *MockClient) Status() client.StatusWriter {
	return m.innerClient.Status()
}
