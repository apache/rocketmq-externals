package writer

import (
	"math/rand"
	"time"
)

var defaultBackoff = backoffConfig{
	maxDelay:  time.Second,
	baseDelay: 10 * time.Millisecond,
	factor:    1.6,
	jitter:    0.2,
}

type backoffConfig struct {
	maxDelay  time.Duration
	baseDelay time.Duration
	factor    float64
	jitter    float64
}

func (bc backoffConfig) backoff(retries int) time.Duration {
	if retries == 0 {
		return bc.baseDelay
	}
	backoff, max := float64(bc.baseDelay), float64(bc.maxDelay)
	for backoff < max && retries > 0 {
		backoff *= bc.factor
		retries--
	}
	if backoff > max {
		backoff = max
	}
	backoff *= 1 + bc.jitter*(rand.Float64()*2-1)
	if backoff < 0 {
		return 0
	}
	return time.Duration(backoff)
}
