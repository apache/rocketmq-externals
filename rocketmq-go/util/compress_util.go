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

package util

import (
	"bytes"
	"compress/zlib"
	"github.com/golang/glog"
	"io/ioutil"
)

//UnCompress un compress byte array
func UnCompress(body []byte) (unCompressBody []byte, err error) {
	b := bytes.NewReader(body)
	z, err := zlib.NewReader(b)
	if err != nil {
		glog.Error(err)
		return nil, err
	}
	unCompressBody, err = ioutil.ReadAll(z)
	z.Close()
	return unCompressBody, nil
}

//Compress compress byte array
func Compress(body []byte) (compressBody []byte, err error) {
	var in bytes.Buffer
	w := zlib.NewWriter(&in)
	_, err = w.Write(body)
	w.Close()
	compressBody = in.Bytes()
	return
}

//CompressWithLevel compress byte array with level
func CompressWithLevel(body []byte, level int) (compressBody []byte, err error) {
	var (
		in bytes.Buffer
		w  *zlib.Writer
	)
	//w := zlib.NewWriter(&in)
	w, err = zlib.NewWriterLevel(&in, level)
	if err != nil {
		return
	}
	_, err = w.Write(body)
	w.Close()
	compressBody = in.Bytes()
	return
}
