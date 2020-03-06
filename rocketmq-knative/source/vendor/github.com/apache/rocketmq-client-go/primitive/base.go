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

package primitive

import (
	"regexp"
	"strings"
)

var (
	ipRegex, _ = regexp.Compile(`^((25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))`)
)

type NamesrvAddr []string

func NewNamesrvAddr(s ...string) (NamesrvAddr, error) {
	if len(s) == 0 {
		return nil, ErrNoNameserver
	}

	ss := s
	if len(ss) == 1 {
		// compatible with multi server env string: "a;b;c"
		ss = strings.Split(s[0], ";")
	}

	for _, srv := range ss {
		if err := verifyIP(srv); err != nil {
			return nil, err
		}
	}

	addrs := make(NamesrvAddr, 0)
	addrs = append(addrs, ss...)
	return addrs, nil
}

func (addr NamesrvAddr) Check() error {
	for _, srv := range addr {
		if err := verifyIP(srv); err != nil {
			return err
		}
	}
	return nil
}

var (
	httpPrefixRegex, _ = regexp.Compile("^(http|https)://")
)

func verifyIP(ip string) error {
	if httpPrefixRegex.MatchString(ip) {
		return nil
	}
	if strings.Contains(ip, ";") {
		return ErrMultiIP
	}
	ips := ipRegex.FindAllString(ip, -1)
	if len(ips) == 0 {
		return ErrIllegalIP
	}

	if len(ips) > 1 {
		return ErrMultiIP
	}
	return nil
}

var PanicHandler func(interface{})

func WithRecover(fn func()) {
	defer func() {
		handler := PanicHandler
		if handler != nil {
			if err := recover(); err != nil {
				handler(err)
			}
		}
	}()

	fn()
}
