#!/bin/bash

provisioner --scale 25
provisioner --restart

coordinator     --workerVmOptions "-ea -server -Xms2G -Xmx2G -XX:+PrintGC -XX:+HeapDumpOnOutOfMemoryError" \
                --clientHzFile      ../conf/client-hazelcast.xml \
                --hzFile            ../conf/hazelcast.xml \
                --clientWorkerCount 100 \
                --memberWorkerCount 25 \
                --workerClassPath   '../target/*.jar' \
                --duration          4m \
                ../conf/test.properties

provisioner --download

#provisioner --terminate