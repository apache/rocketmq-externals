package org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl;

import org.apache.rocketmq.connect.runtime.service.PositionManagementService;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class TestPositionManageServiceImpl implements PositionManagementService {

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void persist() {

    }

    @Override
    public void synchronize() {

    }

    @Override
    public Map<ByteBuffer, ByteBuffer> getPositionTable() {
        return null;
    }

    @Override
    public ByteBuffer getPosition(ByteBuffer partition) {
        return null;
    }

    @Override
    public void putPosition(Map<ByteBuffer, ByteBuffer> positions) {

    }

    @Override
    public void putPosition(ByteBuffer partition, ByteBuffer position) {

    }

    @Override
    public void removePosition(List<ByteBuffer> partitions) {

    }

    @Override
    public void registerListener(PositionUpdateListener listener) {

    }
}
