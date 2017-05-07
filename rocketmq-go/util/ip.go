/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package util

import (
	"net"
	"strings"
)

func GetIp4Bytes() (ret []byte) {
	ip := getIp()
	ret = ip[len(ip)-4:]
	return
}

func GetLocalIp4() (ip4 string) {
	ip := getIp()
	if ip.To4() != nil {
		currIp := ip.String()
		if !strings.Contains(currIp, ":") && currIp != "127.0.0.1" && isIntranetIpv4(currIp) {
			ip4 = currIp
		}
	}
	return
}
func getIp() (ip net.IP) {
	interfaces, err := net.Interfaces()
	if err != nil {
		return
	}

	for _, face := range interfaces {
		if strings.Contains(face.Name, "lo") {
			continue
		}
		addrs, err := face.Addrs()
		if err != nil {
			return
		}

		for _, addr := range addrs {
			if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
				if ipnet.IP.To4() != nil {
					currIp := ipnet.IP.String()
					if !strings.Contains(currIp, ":") && currIp != "127.0.0.1" && isIntranetIpv4(currIp) {
						ip = ipnet.IP
					}
				}
			}
		}
	}
	return
}
func isIntranetIpv4(ip string) bool {
	if strings.HasPrefix(ip, "192.168.") || strings.HasPrefix(ip, "169.254.") {
		return true
	}
	return false
}
