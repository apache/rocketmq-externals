package org.apache.rocketmq.redis.replicator.cmd.parser;

import org.apache.rocketmq.redis.replicator.cmd.impl.RPopLPushCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class RPopLPushParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        RPopLPushParser parser = new RPopLPushParser();
        RPopLPushCommand cmd = parser.parse(toObjectArray("RPOPLPUSH mylist myotherlist".split(" ")));
        assertEquals("mylist", cmd.getSource());
        assertEquals("myotherlist", cmd.getDestination());
        System.out.println(cmd);
    }

}