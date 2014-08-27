#!/bin/bash



coordinator     --workerVmOptions "-ea -server -Xms2G -Xmx2G -XX:+PrintGC -XX:+HeapDumpOnOutOfMemoryError" \
                --clientHzFile      ../conf/client-hazelcast.xml \
                --hzFile            ../conf/hazelcast.xml \
                --clientWorkerCount 2 \
                --memberWorkerCount 2 \
                --workerClassPath   '../target/*.jar' \
                --duration          2m \
                ../conf/test.properties

provisioner --download



