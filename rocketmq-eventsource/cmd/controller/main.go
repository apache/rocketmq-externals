
package main

import (
	"log"
	"github.com/apache/rocketmq-externals/rocketmq-eventsource/pkg/apis"
	controller "github.com/apache/rocketmq-externals/rocketmq-eventsource/pkg/reconciler"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"knative.dev/pkg/logging/logkey"
	"sigs.k8s.io/controller-runtime/pkg/client/config"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/runtime/signals"
)

func main() {
	logCfg := zap.NewProductionConfig()
	logCfg.EncoderConfig.EncodeTime = zapcore.ISO8601TimeEncoder
	logger, err := logCfg.Build()
	if err != nil {
		log.Fatal(err)
	}
	logger = logger.With(zap.String(logkey.ControllerType, "alitablestore-controller"))
	if err != nil {
		log.Fatal(err)
	}

	cfg, err := config.GetConfig()
	if err != nil {
		log.Fatal(err)
	}

	// Create a new Cmd to provide shared dependencies and start components
	mgr, err := manager.New(cfg, manager.Options{})
	if err != nil {
		log.Fatal(err)
	}

	log.Printf("Registering Components.")

	// Setup Scheme for all resources
	if err := apis.AddToScheme(mgr.GetScheme()); err != nil {
		log.Fatal(err)
	}

	log.Printf("Setting up Controller.")
	// Setup MNS Controller
	if err := controller.Add(mgr, logger.Sugar()); err != nil {
		log.Fatal(err)
	}

	log.Printf("Starting alitablestore controller.")

	// Start the Cmd
	log.Fatal(mgr.Start(signals.SetupSignalHandler()))
}
