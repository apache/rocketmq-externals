package org.apache.rocketmq.amqp.exchange;

import org.apache.rocketmq.amqp.framing.AMQShortString;

/**
 * Defines the names of the standard AMQP exchanges that every AMQP broker should provide. These exchange names
 * and type are given in the specification.
 *
 * <p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Defines the standard AMQP exchange names.
 * <tr><td> Defines the standard AMQP exchange types.
 * </table>
 *
 * @todo A type safe enum, might be more appropriate for the exchange types.
 */
public class ExchangeDefaults
{
    /** The default direct exchange, which is a special internal exchange that cannot be explicitly bound to. */
    public static final AMQShortString DEFAULT_EXCHANGE_NAME = new AMQShortString("<<default>>");

    /** The pre-defined topic exchange, the broker SHOULD provide this. */
    public static final AMQShortString TOPIC_EXCHANGE_NAME = new AMQShortString("amq.topic");

    /** Defines the identifying type name of topic exchanges. */
    public static final AMQShortString TOPIC_EXCHANGE_CLASS = new AMQShortString("topic");

    /** The pre-defined direct exchange, the broker MUST provide this. */
    public static final AMQShortString DIRECT_EXCHANGE_NAME = new AMQShortString("amq.direct");

    /** Defines the identifying type name of direct exchanges. */
    public static final AMQShortString DIRECT_EXCHANGE_CLASS = new AMQShortString("direct");

    /** The pre-defined headers exchange, the specification does not say this needs to be provided. */
    public static final AMQShortString HEADERS_EXCHANGE_NAME = new AMQShortString("amq.match");

    /** Defines the identifying type name of headers exchanges. */
    public static final AMQShortString HEADERS_EXCHANGE_CLASS = new AMQShortString("headers");

    /** The pre-defined fanout exchange, the boker MUST provide this. */
    public static final AMQShortString FANOUT_EXCHANGE_NAME = new AMQShortString("amq.fanout");

    /** Defines the identifying type name of fanout exchanges. */
    public static final AMQShortString FANOUT_EXCHANGE_CLASS = new AMQShortString("fanout");

    public static final AMQShortString WILDCARD_ANY = new AMQShortString("*");
}
