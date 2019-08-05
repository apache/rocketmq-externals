package org.apache.connect.mongo;

import io.openmessaging.KeyValue;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class MongoReplicatorConfig {

    private String mongoAddr;
    private int mongoPort;
    private String mongoUserName;
    private String mongoPassWord;
    private String interestDB;
    private String interestCollection;
    private long positionTimeStamp;
    private int positionInc;
    private String dataSync;
    private int copyThread = Runtime.getRuntime().availableProcessors();


    public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add("mongoAddr");
            add("mongoPort");
        }
    };

    public int getPositionInc() {
        return positionInc;
    }

    public void setPositionInc(int positionInc) {
        this.positionInc = positionInc;
    }

    public int getCopyThread() {
        return copyThread;
    }

    public void setCopyThread(int copyThread) {
        this.copyThread = copyThread;
    }

    public long getPositionTimeStamp() {
        return positionTimeStamp;
    }

    public void setPositionTimeStamp(long positionTimeStamp) {
        this.positionTimeStamp = positionTimeStamp;
    }

    public String getInterestDB() {
        return interestDB;
    }

    public void setInterestDB(String interestDB) {
        this.interestDB = interestDB;
    }

    public String getInterestCollection() {
        return interestCollection;
    }

    public void setInterestCollection(String interestCollection) {
        this.interestCollection = interestCollection;
    }

    public String getMongoAddr() {
        return mongoAddr;
    }

    public void setMongoAddr(String mongoAddr) {
        this.mongoAddr = mongoAddr;
    }

    public int getMongoPort() {
        return mongoPort;
    }

    public void setMongoPort(int mongoPort) {
        this.mongoPort = mongoPort;
    }

    public String getMongoUserName() {
        return mongoUserName;
    }

    public void setMongoUserName(String mongoUserName) {
        this.mongoUserName = mongoUserName;
    }

    public String getMongoPassWord() {
        return mongoPassWord;
    }

    public void setMongoPassWord(String mongoPassWord) {
        this.mongoPassWord = mongoPassWord;
    }


    public String getDataSync() {
        return dataSync;
    }

    public void setDataSync(String dataSync) {
        this.dataSync = dataSync;
    }


    public void load(KeyValue props) {

        properties2Object(props, this);
    }

    private void properties2Object(final KeyValue p, final Object object) {

        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                    String tmp = mn.substring(4);
                    String first = mn.substring(3, 4);

                    String key = first.toLowerCase() + tmp;
                    String property = p.getString(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg;
                            if (cn.equals("int") || cn.equals("Integer")) {
                                arg = Integer.parseInt(property);
                            } else if (cn.equals("long") || cn.equals("Long")) {
                                arg = Long.parseLong(property);
                            } else if (cn.equals("double") || cn.equals("Double")) {
                                arg = Double.parseDouble(property);
                            } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                                arg = Boolean.parseBoolean(property);
                            } else if (cn.equals("float") || cn.equals("Float")) {
                                arg = Float.parseFloat(property);
                            } else if (cn.equals("String")) {
                                arg = property;
                            } else {
                                continue;
                            }
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
