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

package v1alpha1

import (
	"fmt"

	yaml2 "gopkg.in/yaml.v2"
)

func (in *Artifact) String() string {
	return in.ID
}

func (spec ConfigurationSpec) String() string {
	return fmt.Sprintf("%s=%s", spec.Type, spec.Value)
}

// Serialize serializes a Flow
func (flows Flows) Serialize() (string, error) {
	res, err := yaml2.Marshal(flows)
	if err != nil {
		return "", err
	}
	return string(res), nil
}
