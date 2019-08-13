package org.apache.connect.mongo.replicator;

import org.bson.BsonTimestamp;

import java.util.Objects;

public class ReplicaSetConfig {


    private String shardName;
    private String replicaSetName;
    private String host;
    private Position position;


    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getShardName() {
        return shardName;
    }

    public void setShardName(String shardName) {
        this.shardName = shardName;
    }

    public String getReplicaSetName() {
        return replicaSetName;
    }

    public void setReplicaSetName(String replicaSetName) {
        this.replicaSetName = replicaSetName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public ReplicaSetConfig(String shardName, String replicaSetName, String host) {
        this.shardName = shardName;
        this.replicaSetName = replicaSetName;
        this.host = host;
    }

    public Position emptyPosition() {
        return new Position(0, 0, true);
    }


    public class Position {
        private int timeStamp;
        private int inc;
        private boolean initSync;


        public int getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(int timeStamp) {
            this.timeStamp = timeStamp;
        }

        public int getInc() {
            return inc;
        }

        public void setInc(int inc) {
            this.inc = inc;
        }

        public boolean isInitSync() {
            return initSync;
        }

        public void setInitSync(boolean initSync) {
            this.initSync = initSync;
        }


        public Position(int timeStamp, int inc, boolean initSync) {
            this.timeStamp = timeStamp;
            this.inc = inc;
            this.initSync = initSync;
        }

        public boolean isValid() {
            return timeStamp > 0;
        }

        public BsonTimestamp converBsonTimeStamp() {
            return new BsonTimestamp(timeStamp, inc);
        }

        @Override
        public String toString() {
            return "Position{" +
                    "timeStamp=" + timeStamp +
                    ", inc=" + inc +
                    ", initSync=" + initSync +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return timeStamp == position.timeStamp &&
                    inc == position.inc &&
                    initSync == position.initSync;
        }

        @Override
        public int hashCode() {
            return Objects.hash(timeStamp, inc, initSync);
        }
    }


    @Override
    public String toString() {
        return "ReplicaSetConfig{" +
                "shardName='" + shardName + '\'' +
                ", replicaSetName='" + replicaSetName + '\'' +
                ", host='" + host + '\'' +
                ", position=" + position +
                '}';
    }
}
