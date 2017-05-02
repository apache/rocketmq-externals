package model

type MQBrokerError struct {
	ResponseCode int
	errorMessage string
}

type MQClientError struct {
	ResponseCode int
	errorMessage string
}

func NewMQBrokerError(code int, msg string) MQBrokerError {
	return MQBrokerError{code, msg}
}

func (err MQBrokerError) Error() string { // TODO
	return err.errorMessage
}

func NewMQClientError(code int, msg string) MQClientError {
	return MQClientError{code, msg}
}

func (err MQClientError) Error() string {
	return err.errorMessage
}