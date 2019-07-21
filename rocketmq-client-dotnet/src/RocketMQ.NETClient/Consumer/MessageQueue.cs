using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace RocketMQ.NETClient.Consumer
{
    public class MessageQueue
    {
        public string topic { get; set; }

        public string brokeName { get; set; }

        public int queueId { get; set; }
    }
}
