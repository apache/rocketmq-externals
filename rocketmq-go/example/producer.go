package example

import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/producer"
	"os"
)

func main() {
	pproducer := producer.NewDefaultProducer("pro")
	pproducer.SetNameServerAddress("test")

	err := pproducer.Start()

	if err != nil {
		fmt.Println(err)
		os.Exit(-1)
	}

	for i := 1; i < 10; i++ {
		message := msg.NewDefultMessage("test", []byte("test"))
		res, _ := pproducer.Send(message)
		fmt.Println(res)
	}
}
