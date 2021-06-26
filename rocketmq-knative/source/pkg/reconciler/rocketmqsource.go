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
package rocketmq

import (
	"context"

	"fmt"
	"log"
	"os"

	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/apis/sources/v1alpha1"
	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/reconciler/resources"
	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/controller/sdk"
	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/controller/sinks"
	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/reconciler/eventtype"
	eventingv1alpha1 "github.com/knative/eventing/pkg/apis/eventing/v1alpha1"
	"go.uber.org/zap"
	appsv1 "k8s.io/api/apps/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/util/sets"
	"knative.dev/pkg/logging"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/manager"
)

const (
	// controllerAgentName is the string used by this controller to identify
	// itself when creating events.
	controllerAgentName = "source-controller"

	// raImageEnvVar is the name of the environment variable that contains the receive adapter's
	// image. It must be defined.
	raImageEnvVar = "RocketMQ_RA_IMAGE"

	finalizerName = controllerAgentName
)

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new RocketMQSource Controller and adds it to the Manager with default RBAC. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager, logger *zap.SugaredLogger) error {
	raImage, defined := os.LookupEnv(raImageEnvVar)
	if !defined {
		return fmt.Errorf("required environment variable '%s' not defined", raImageEnvVar)
	}
	log.Println("Adding the RocketMQ Source controller.")
	p := &sdk.Provider{
		AgentName: controllerAgentName,
		Parent:    &v1alpha1.RocketMQSource{},
		Owns:      []runtime.Object{&appsv1.Deployment{}, &eventingv1alpha1.EventType{}},
		Reconciler: &reconciler{
			scheme:              mgr.GetScheme(),
			receiveAdapterImage: raImage,
			eventTypeReconciler: eventtype.Reconciler{
				Scheme: mgr.GetScheme(),
			},
		},
	}

	return p.Add(mgr, logger)
}

type reconciler struct {
	client              client.Client
	scheme              *runtime.Scheme
	receiveAdapterImage string
	eventTypeReconciler eventtype.Reconciler
}

func (r *reconciler) InjectClient(c client.Client) error {
	r.client = c
	r.eventTypeReconciler.Client = c
	return nil
}

func (r *reconciler) Reconcile(ctx context.Context, object runtime.Object) error {
	logger := logging.FromContext(ctx).Desugar()

	src, ok := object.(*v1alpha1.RocketMQSource)
	if !ok {
		logger.Error("could not find RocketMQ source", zap.Any("object", object))
		return nil
	}

	deletionTimestamp := src.DeletionTimestamp
	if deletionTimestamp != nil {
		r.removeFinalizer(src)
		return nil
	}

	r.addFinalizer(src)

	src.Status.InitializeConditions()

	sinkURI, err := sinks.GetSinkURI(ctx, r.client, src.Spec.Sink, src.Namespace)
	if err != nil {
		src.Status.MarkNoSink("NotFound", "")
		return err
	}
	src.Status.MarkSink(sinkURI)

	_, err = r.createReceiveAdapter(ctx, src, "knative-eventing-default", sinkURI)
	if err != nil {
		logger.Error("Unable to create the receive adapter", zap.Error(err))
		return err
	}
	src.Status.MarkDeployed()

	err = r.reconcileEventTypes(ctx, src)
	if err != nil {
		logger.Error("Unable to reconcile the event types", zap.Error(err))
		return err
	}
	src.Status.MarkEventTypes()

	return nil
}

func (r *reconciler) addFinalizer(s *v1alpha1.RocketMQSource) {
	finalizers := sets.NewString(s.Finalizers...)
	finalizers.Insert(finalizerName)
	s.Finalizers = finalizers.List()
}

func (r *reconciler) removeFinalizer(s *v1alpha1.RocketMQSource) {
	finalizers := sets.NewString(s.Finalizers...)
	finalizers.Delete(finalizerName)
	s.Finalizers = finalizers.List()
}

func (r *reconciler) createReceiveAdapter(ctx context.Context, src *v1alpha1.RocketMQSource, subscriptionID, sinkURI string ) (*appsv1.Deployment, error) {
	ra, err := r.getReceiveAdapter(ctx, src)
	if err != nil && !apierrors.IsNotFound(err) {
		logging.FromContext(ctx).Error("Unable to get an existing receive adapter", zap.Error(err))
		return nil, err
	}
	if ra != nil {
		logging.FromContext(ctx).Desugar().Info("Reusing existing receive adapter", zap.Any("receiveAdapter", ra))
		return ra, nil
	}
	svc := resources.MakeReceiveAdapter(&resources.ReceiveAdapterArgs{
		Image:          r.receiveAdapterImage,
		Source:         src,
		Labels:         getLabels(src),
		SubscriptionID: subscriptionID,
		SinkURI:        sinkURI,
	})
	if err := controllerutil.SetControllerReference(src, svc, r.scheme); err != nil {
		return nil, err
	}
	err = r.client.Create(ctx, svc)
	logging.FromContext(ctx).Desugar().Info("Receive Adapter created.", zap.Error(err), zap.Any("receiveAdapter", svc))
	return svc, err
}

func (r *reconciler) getReceiveAdapter(ctx context.Context, src *v1alpha1.RocketMQSource) (*appsv1.Deployment, error) {
	dl := &appsv1.DeploymentList{}
	err := r.client.List(ctx, &client.ListOptions{
		Namespace:     src.Namespace,
		LabelSelector: r.getLabelSelector(src),
		// TODO this is only needed by the fake client. Real K8s does not need it. Remove it once
		// the fake is fixed.
		Raw: &metav1.ListOptions{
			TypeMeta: metav1.TypeMeta{
				APIVersion: appsv1.SchemeGroupVersion.String(),
				Kind:       "Deployment",
			},
		},
	},
		dl)

	if err != nil {
		logging.FromContext(ctx).Desugar().Error("Unable to list deployments: %v", zap.Error(err))
		return nil, err
	}
	for _, dep := range dl.Items {
		if metav1.IsControlledBy(&dep, src) {
			return &dep, nil
		}
	}
	return nil, apierrors.NewNotFound(schema.GroupResource{}, "")
}

func (r *reconciler) getLabelSelector(src *v1alpha1.RocketMQSource) labels.Selector {
	return labels.SelectorFromSet(getLabels(src))
}

func getLabels(src *v1alpha1.RocketMQSource) map[string]string {
	return map[string]string{
		"knative-eventing-source":      controllerAgentName,
		"knative-eventing-source-name": src.Name,
	}
}

func (r *reconciler) reconcileEventTypes(ctx context.Context, src *v1alpha1.RocketMQSource) error {
	args := r.newEventTypeReconcilerArgs(src)
	return r.eventTypeReconciler.Reconcile(ctx, src, args)
}

func (r *reconciler) newEventTypeReconcilerArgs(src *v1alpha1.RocketMQSource) *eventtype.ReconcilerArgs {
	specs := make([]eventingv1alpha1.EventTypeSpec, 0)
	spec := eventingv1alpha1.EventTypeSpec{
		Type:   v1alpha1.RocketMQSourceEventType,
		Source: v1alpha1.RocketMQEventSource(src.Spec.Topic),
		Broker: src.Spec.Sink.Name,
	}
	specs = append(specs, spec)
	return &eventtype.ReconcilerArgs{
		Specs:     specs,
		Namespace: src.Namespace,
		Labels:    getLabels(src),
		Kind:      src.Spec.Sink.Kind,
	}
}

func generateSubName(src *v1alpha1.RocketMQSource) string {
	return fmt.Sprintf("knative-eventing-%s-%s-%s", src.Namespace, src.Name, src.UID)
}
