#!/bin/bash

provisioner --scale 4

coordinator     --workerVmOptions "-ea -server -Xms2G -Xmx2G -XX:+PrintGC -XX:+HeapDumpOnOutOfMemoryError" \
                --clientHzFile      ../conf/client-hazelcast.xml \
                --hzFile            ../conf/hazelcast.xml \
                --clientWorkerCount 4 \
                --memberWorkerCount 4 \
                --workerClassPath   '../target/*.jar' \
                --duration          2m \
                ../conf/test.properties

provisioner --download

#provisioner --terminate