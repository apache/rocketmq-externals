package util

import (
	"bytes"
	"compress/zlib"
	"github.com/golang/glog"
	"io/ioutil"
)

func UnCompress(body []byte) (unCompressBody []byte, err error) {
	b := bytes.NewReader(body)
	z, err := zlib.NewReader(b)
	if err != nil {
		glog.Error(err)
		return
	}
	defer z.Close()
	unCompressBody, err = ioutil.ReadAll(z)
	if err != nil {
		glog.Error(err)
	}
	return
}
func Compress(body []byte) (compressBody []byte, err error) {
	var in bytes.Buffer
	w := zlib.NewWriter(&in)
	_, err = w.Write(body)
	w.Close()
	compressBody = in.Bytes()
	return
}

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
