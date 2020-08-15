package timeline

import "errors"

var (
	ErrMisuse     = errors.New("misuse")
	ErrUnexpected = errors.New("unexpected")
	ErrorDone     = errors.New("done")
)
