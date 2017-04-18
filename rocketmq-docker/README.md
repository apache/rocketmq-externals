# Apache RocketMQ Docker module

Apache RocketMQ Docker module provides Dockerfile and bash scripts for building docker image.


cd 4.0.0-incubating/broker/ or cd 4.0.0-incubating/namesrv/ 

## Prequirement

* Docker Engine
* Linux / windows 

## Build:

### For Linux
```bash
sh docker_build.sh
```
### For Windows
```
.\docker_build.cmd
```

## Run:

### For Linux
```
sh docker_run.sh
```
### For Windows
```
.\docker_run.cmd
```

## Play:

Build broker and namesrv images, then run them at local. The name server address of broker is automatically configured.

```
cd 4.0.0-incubating
```

### For Windows
```
.\play.cmd
```

### For Linux
```
sh play.sh
```