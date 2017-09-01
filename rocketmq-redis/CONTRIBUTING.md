## Before pull request  

* execute following commands and make sure you can pass all testcase.  

```
sudo wget https://github.com/antirez/redis/archive/3.2.3.tar.gz && tar -xvzf 3.2.3.tar.gz && cd redis-3.2.3 && make  
cd src && nohup ./redis-server --port 6380 --requirepass test &
sudo wget https://github.com/antirez/redis/archive/3.0.7.tar.gz && tar -xvzf 3.0.7.tar.gz && cd redis-3.0.7 && make 
cd src && nohup ./redis-server --port 6379 &
sudo wget -O stunnel.tar.gz ftp://ftp.stunnel.org/stunnel/archive/5.x/stunnel-5.29.tar.gz && tar -xvzf stunnel.tar.gz && cd stunnel-5.29 && ./configure && make && sudo make install && cd src && wget https://raw.githubusercontent.com/leonchen83/redis-replicator/master/src/test/resources/keystore/stunnel.conf && wget https://raw.githubusercontent.com/leonchen83/redis-replicator/master/src/test/resources/keystore/private.pem && ./stunnel stunnel.conf
sudo mvn clean package
```

