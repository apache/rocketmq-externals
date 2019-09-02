# centos 下 rocketmq-client-mq 安装

[toc]

## 安装总览

1. 安装 `rocketmq-client-cpp`
2. 安装 `PHP-CPP`
3. 安装  `rocketmq-client-php`


## 安装 rocketmq-client-cpp

### 安装依赖


```bash

yum -y install gcc gcc-c++

yum -y install cmake

yum -y install automake 

yum -y install autoconf

yum -y install libtool

yum -y install bzip-devel

yum -y install libzip

yum -y install libzip-devel


```



### 下载源码和依赖包

```

git clone  https://github.com/apache/rocketmq-client-cpp.git

```

下载必要的包到 **rocketmq-client-cpp** 根目录（可以自动下，但自动下载可能时间长）

* [libevent 2.0.22](https://github.com/libevent/libevent/archive/release-2.0.22-stable.zip)
* [jsoncpp 0.10.6](https://github.com/open-source-parsers/jsoncpp/archive/0.10.6.zip) 
* [boost 1.58.0](http://sourceforge.net/projects/boost/files/boost/1.58.0/boost_1_58_0.tar.gz)



### build

build 脚本会自动下载上述依赖包，如果下载时间较长，建议自己下完，放在**rocketmq-client-cpp**根目录下

```

cd rocketmq-client-cpp

sh build.sh

```

### install

```

mkdir /usr/include/rocketmq

cp -rf include/* /usr/include/rocketmq

cd bin

cp -f librocketmq.a /usr/lib

cp -f librocketmq.so /usr/lib

cp -f librocketmq.a /usr/lib64

cp -f librocketmq.so /usr/lib64

cp -rf include/* /usr/include/

cp -rf lib/* /usr/lib/

cp -rf lib64/* /usr/lib64/


```

## 安装PHPCPP

### 安装依赖

#### 安装php

可以使用源码或者yum安装,这里使用yum安装

```
 yum -y install php

```
查看php版本

```

php -v

```

#### 安装 php-devel

```
 yum -y install php-devel

```
检查安装

```

php-config

```

出现如下代码表示成功

```

Usage: /usr/bin/php-config [OPTION]
Options:
  --prefix            [/usr]
  --includes          [-I/usr/include/php -I/usr/include/php/main -I/usr/include/php/TSRM -I/usr/include/php/Zend -I/usr/include/php/ext -I/usr/include/php/ext/date/lib]
  --ldflags           []
  --libs              [-lcrypt   -lresolv -lcrypt -ledit -lncurses -lstdc++ -lgmp -lbz2 -lz -lpcre -lrt -lm -ldl -lnsl  -lxml2 -lz -lm -ldl -lgssapi_krb5 -lkrb5 -lk5crypto -lcom_err -lssl -lcrypto -lssl -lcrypto -lxml2 -lz -lm -ldl -lcrypt -lxml2 -lz -lm -ldl -lcrypt ]
  --extension-dir     [/usr/lib64/php/modules]
  --include-dir       [/usr/include/php]
  --man-dir           [/usr/share/man]
  --php-binary        [/usr/bin/php]
  --php-sapis         [ cli cgi]
  --configure-options [--build=x86_64-redhat-linux-gnu --host=x86_64-redhat-linux-gnu --program-prefix= --disable-dependency-tracking --prefix=/usr --exec-prefix=/usr --bindir=/usr/bin --sbindir=/usr/sbin --sysconfdir=/etc --datadir=/usr/share --includedir=/usr/include --libdir=/usr/lib64 --libexecdir=/usr/libexec --localstatedir=/var --sharedstatedir=/var/lib --mandir=/usr/share/man --infodir=/usr/share/info --cache-file=../config.cache --with-libdir=lib64 --with-config-file-path=/etc --with-config-file-scan-dir=/etc/php.d --disable-debug --with-pic --disable-rpath --without-pear --with-bz2 --with-exec-dir=/usr/bin --with-freetype-dir=/usr --with-png-dir=/usr --with-xpm-dir=/usr --enable-gd-native-ttf --with-t1lib=/usr --without-gdbm --with-gettext --with-gmp --with-iconv --with-jpeg-dir=/usr --with-openssl --with-pcre-regex=/usr --with-zlib --with-layout=GNU --enable-exif --enable-ftp --enable-sockets --with-kerberos --enable-shmop --enable-calendar --with-libxml-dir=/usr --enable-xml --with-system-tzdata --with-mhash --libdir=/usr/lib64/php --enable-pcntl --enable-mbstring=shared --enable-mbregex --with-gd=shared --enable-bcmath=shared --enable-dba=shared --with-db4=/usr --with-tcadb=/usr --with-xmlrpc=shared --with-ldap=shared --with-ldap-sasl --enable-mysqlnd=shared --with-mysql=shared,mysqlnd --with-mysqli=shared,mysqlnd --with-mysql-sock=/var/lib/mysql/mysql.sock --enable-dom=shared --with-pgsql=shared --enable-wddx=shared --with-snmp=shared,/usr --enable-soap=shared --with-xsl=shared,/usr --enable-xmlreader=shared --enable-xmlwriter=shared --with-curl=shared,/usr --enable-pdo=shared --with-pdo-odbc=shared,unixODBC,/usr --with-pdo-mysql=shared,mysqlnd --with-pdo-pgsql=shared,/usr --with-pdo-sqlite=shared,/usr --with-sqlite3=shared,/usr --enable-json=shared --enable-zip=shared --with-libzip --without-readline --with-libedit --with-pspell=shared --enable-phar=shared --enable-sysvmsg=shared --enable-sysvshm=shared --enable-sysvsem=shared --enable-posix=shared --with-unixODBC=shared,/usr --enable-fileinfo=shared --enable-intl=shared --with-icu-dir=/usr --with-enchant=shared,/usr --with-recode=shared,/usr]
  --version           [5.4.16]
  --vernum            [50416]

```

从上面的信息可以看出 php的版本是5.4

### 下载php-cpp

* php>=7.0

[PHP-CPP](https://github.com/CopernicaMarketingSoftware/PHP-CPP.git)

* php >=5.* && php < 7.0

[PHP-CPP-LEGAY](https://github.com/CopernicaMarketingSoftware/PHP-CPP-LEGACY.git)

#### 下载对应版本，安装过程相同

```

git clone https://github.com/CopernicaMarketingSoftware/PHP-CPP-LEGACY.git

```

### build & install

```

cd PHP-CPP-LEGAY

make && make install

```

## 安装 rocketmq-client-php

### 下载 rocketmq-client-php

```

git clone  https://github.com/apache/rocketmq-externals.git


```
### build & install

```

cd rocketmq-externals/rocketmq-clietn-php

make && make install

```

## 开启rocketmq扩展

打开php.ini

* 注意 不同的安装可能导致php.ini的位置不同，请注意自己安装的位置

1. 打开php.ini

```

vim /etc/php.ini

```

2. 找到`extension=`的模块，增加一行

```

extension=rocketmq.so

```

具体效果如下图

```vim

....

;
; ... or under UNIX:
;
;   extension=msql.so
;
; ... or with a path:
;
;   extension=/path/to/extension/msql.so
;
; If you only provide the name of the extension, PHP will look for it in its
; default extension directory.

extension=rocketmq.so

;;;;
; Note: packaged extension modules are now loaded via the .ini files
; found in the directory /etc/php.d; these are loaded by default.
;;;;

...


```

3. 查看

运行命令

```
php -v

```

出现以下信息时，并且没有报错时说明扩展安装成功

```bash

PHP 5.4.16 (cli) (built: Oct 30 2018 19:30:51) 
Copyright (c) 1997-2013 The PHP Group
Zend Engine v2.4.0, Copyright (c) 1998-2013 Zend Technologies

```

4. 查看扩展 

运行命令

```

php -m |grep rocketmq

```

查看到如下结果扩展完全运行

```
rocketmq
```

## 使用rocketmq-client-php

参考[example](https://github.com/apache/rocketmq-externals/blob/master/rocketmq-client-php/example)使用

* Producer示例

```php

namespace RocketMQ;
$instanceName = "MessageQueue";
$producer = new Producer($instanceName);
$producer->setInstanceName($instanceName);
$producer->setNamesrvAddr("127.0.0.1:9876");
$producer->setTcpTransportPullThreadNum(40);
$producer->getTcpTransportConnectTimeout(100);
$producer->setTcpTransportTryLockTimeout(1);
$producer->start();
$queues = $producer->getTopicMessageQueueInfo("TopicTest");
	echo "-------------------------------------------------------------------------\n";
foreach($queues as $queue){
	printf("|%-30s|%-40s|\n", "topic", $queue->getTopic());
	printf("|%-30s|%-40s|\n", "brokerName", $queue->getBrokerName());
	printf("|%-30s|%-40s|\n", "queueId", $queue->getQueueId());
	echo "-------------------------------------------------------------------------\n";
}
for ($i = 0; $i < 10000; $i ++){
	$message = new Message("TopicTest", "*", "hello world $i");
	$sendResult = $producer->send($message, $queues[3]);
	printf("|%-30s|%-40s|\n", "msgId", $sendResult->getMsgId());
	printf("|%-30s|%-40s|\n", "offsetMsgId", $sendResult->getOffsetMsgId());
	printf("|%-30s|%-40s|\n", "sendStatus", $sendResult->getSendStatus());
	printf("|%-30s|%-40s|\n", "queueOffset", $sendResult->getQueueOffset());
    printf("|%-30s|%-40s|\n", "body", $message->getBody());
	echo "-------------------------------------------------------------------------\n";
}

```


