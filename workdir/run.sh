#!/bin/bash

provisioner --scale 6

coordinator     --workerVmOptions "-ea -server -Xms2G -Xmx2G -XX:+PrintGC -XX:+HeapDumpOnOutOfMemoryError" \
                --clientHzFile      ../conf/client-hazelcast.xml \
                --hzFile            ../conf/hazelcast.xml \
                --clientWorkerCount 24 \
                --memberWorkerCount 6 \
                --workerClassPath   '../target/*.jar' \
                --duration          10m \
                ../conf/test.properties

provisioner --download

#provisioner --terminatem