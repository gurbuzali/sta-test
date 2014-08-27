#!/bin/bash

#provisioner --scale 30

coordinator     --workerVmOptions "-ea -server -Xms2G -Xmx2G -XX:+PrintGC -XX:+HeapDumpOnOutOfMemoryError" \
                --clientHzFile      ../conf/client-hazelcast.xml \
                --hzFile            ../conf/hazelcast.xml \
                --clientWorkerCount 0 \
                --memberWorkerCount 30 \
                --mixedWorkerCount 0 \
                --workerClassPath   '../target/*.jar' \
                --duration          3m \
                ../conf/test.properties

provisioner --download

#provisioner --terminate