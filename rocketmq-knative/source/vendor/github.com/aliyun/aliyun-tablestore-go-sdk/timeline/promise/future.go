package promise

import (
	"errors"
	"sync"
)

type Future struct {
	done   chan struct{}
	result interface{}
	err    error
	once   sync.Once
}

func NewFuture() *Future {
	return &Future{
		done: make(chan struct{}),
	}
}

func (f *Future) Get() (interface{}, error) {
	<-f.done
	return f.result, f.err
}

func (f *Future) Set(v interface{}, err error) {
	f.once.Do(func() {
		defer close(f.done)
		f.result = v
		f.err = err
	})
}

func (f *Future) FanInGet() ([]*FanResult, error) {
	ret, err := f.Get()
	if err != nil {
		return nil, err
	}
	if fanRet, ok := ret.([]*FanResult); ok {
		return fanRet, nil
	}
	return nil, errors.New("not a fan in future")
}

type FanResult struct {
	Result interface{}
	Err    error
}

func FanIn(futures ...*Future) *Future {
	f := NewFuture()
	go func() {
		fanResults := make([]*FanResult, len(futures))
		wg := new(sync.WaitGroup)
		wg.Add(len(futures))
		for i, f := range futures {
			go func(idx int) {
				defer wg.Done()
				ret, err := f.Get()
				fanResults[idx] = &FanResult{Result: ret, Err: err}
			}(i)
		}
		wg.Wait()
		f.Set(fanResults, nil)
	}()
	return f
}
