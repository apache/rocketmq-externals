package org.apache.rocketmq.amqp.connection;

import org.wso2.andes.transport.ConnectionException;

/**
 * ConnectionListener
 *
 */

public interface ConnectionListener
{

    void opened(Connection connection);

    void exception(Connection connection, ConnectionException exception);

    void closed(Connection connection);

}
