using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace RocketMQ.NETClient.Consumer
{
    public enum CPullStatus
    {
        E_FOUND = 1,
        E_NO_NEW_MSG =2,
        E_NO_MATCHED_MSG =3,
        E_OFFSET_ILLEGAL = 4,
        E_BROKER_TIMEOUT = 5
    }
}
