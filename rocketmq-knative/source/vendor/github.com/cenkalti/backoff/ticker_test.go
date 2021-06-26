package backoff

import (
	"context"
	"errors"
	"fmt"
	"log"
	"testing"
)

func TestTicker(t *testing.T) {
	const successOn = 3
	var i = 0

	// This function is successful on "successOn" calls.
	f := func() error {
		i++
		log.Printf("function is called %d. time\n", i)

		if i == successOn {
			log.Println("OK")
			return nil
		}

		log.Println("error")
		return errors.New("error")
	}

	b := NewExponentialBackOff()
	ticker := NewTickerWithTimer(b, &testTimer{})

	var err error
	for range ticker.C {
		if err = f(); err != nil {
			t.Log(err)
			continue
		}

		break
	}
	if err != nil {
		t.Errorf("unexpected error: %s", err.Error())
	}
	if i != successOn {
		t.Errorf("invalid number of retries: %d", i)
	}
}

func TestTickerContext(t *testing.T) {
	var i = 0

	ctx, cancel := context.WithCancel(context.Background())

	// Cancel context as soon as it is created.
	// Ticker must stop after first tick.
	cancel()

	// This function cancels context on "cancelOn" calls.
	f := func() error {
		i++
		log.Printf("function is called %d. time\n", i)
		log.Println("error")
		return fmt.Errorf("error (%d)", i)
	}

	b := WithContext(NewConstantBackOff(0), ctx)
	ticker := NewTickerWithTimer(b, &testTimer{})

	var err error
	for range ticker.C {
		if err = f(); err != nil {
			t.Log(err)
			continue
		}

		ticker.Stop()
		break
	}
	// Ticker is guaranteed to tick at least once.
	if err == nil {
		t.Errorf("error is unexpectedly nil")
	}
	if err.Error() != "error (1)" {
		t.Errorf("unexpected error: %s", err)
	}
	if i != 1 {
		t.Errorf("invalid number of retries: %d", i)
	}
}
