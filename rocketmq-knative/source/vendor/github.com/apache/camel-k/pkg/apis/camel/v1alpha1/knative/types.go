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

package knative

// CamelEnvironment is the top level configuration object expected by the Camel Knative component
type CamelEnvironment struct {
	Services []CamelServiceDefinition `json:"services"`
}

// NewCamelEnvironment creates a new env
func NewCamelEnvironment() CamelEnvironment {
	return CamelEnvironment{
		Services: make([]CamelServiceDefinition, 0),
	}
}

// CamelServiceDefinition defines the parameters to connect to Knative service. It's also used for exposed services
type CamelServiceDefinition struct {
	ServiceType CamelServiceType  `json:"type"`
	Protocol    CamelProtocol     `json:"protocol"`
	Name        string            `json:"name"`
	Host        string            `json:"host"`
	Port        int               `json:"port"`
	Metadata    map[string]string `json:"metadata"`
}

// CamelServiceType --
type CamelServiceType string

const (
	// CamelServiceTypeEndpoint is a callable endpoint
	CamelServiceTypeEndpoint CamelServiceType = "endpoint"
	// CamelServiceTypeChannel is a callable endpoint that will be also associated to a subscription
	CamelServiceTypeChannel CamelServiceType = "channel"
)

// CamelProtocol is the communication protocol to use for the service
type CamelProtocol string

// Knative protocols
const (
	CamelProtocolHTTP  CamelProtocol = "http"
	CamelProtocolHTTPS CamelProtocol = "https"
)

// Meta Options
const (
	CamelMetaServicePath       = "service.path"
	CamelMetaServiceID         = "service.id"
	CamelMetaServiceName       = "service.name"
	CamelMetaServiceHost       = "service.host"
	CamelMetaServicePort       = "service.port"
	CamelMetaServiceZone       = "service.zone"
	CamelMetaServiceProtocol   = "service.protocol"
	CamelMetaFilterHeaderName  = "filter.header.name"
	CamelMetaFilterHeaderValue = "filter.header.value"
)
