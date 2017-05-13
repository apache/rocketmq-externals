### 2.1.1  
**api changes** :  

```java  
add new commands :  
swapdb,zremrangebylex,zremrangebyrank,zremrangebyscore,multi,exec  
RPushXCommand value -> values (redis 4.0 compatibility)
LPushXCommand value -> values (redis 4.0 compatibility)
```

### 2.1.0
**api changes** :

```java  
RdbVisitor interface -> abstract  
```

**command changes** :  

```java  
ZIncrByCommand.increment int -> double  
SetTypeOffsetValue.value int -> long  
SetRangeCommand.index int -> long  
SetBitCommand.offset int -> long  
LSetCommand.index int -> long  
LRemCommand.index int -> long  
IncrByTypeOffsetIncrement.increment int -> long  
IncrByCommand.value int -> long
HIncrByCommand.increment int -> long
DecrByCommand.value int -> long
```

### 2.0.0-rc3  
**api changes** :  

```java  
ReplicatorListener.addRdbRawByteListener -> ReplicatorListener.addRawByteListener
ReplicatorListener.removeRdbRawByteListener -> ReplicatorListener.removeRawByteListener
```

### 2.0.0-rc2  
no api changes  

### 2.0.0-rc1  
2.0.0 Initial commit  