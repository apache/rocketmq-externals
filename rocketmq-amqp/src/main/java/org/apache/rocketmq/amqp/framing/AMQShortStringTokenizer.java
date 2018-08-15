package org.apache.rocketmq.amqp.framing;

public interface AMQShortStringTokenizer {
    public int countTokens();

    public AMQShortString nextToken();

    boolean hasMoreTokens();
}
