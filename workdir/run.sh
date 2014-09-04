#!/bin/bash

provisioner --scale 25
provisioner --restart

coordinator     --workerVmOptions "-XX:+HeapDumpOnOutOfMemoryError" \
                --clientHzFile      ../conf/client-hazelcast.xml \
                --hzFile            ../conf/hazelcast.xml \
                --clientWorkerCount 100 \
                --memberWorkerCount 25 \
                --workerClassPath   '../target/*.jar' \
                --duration          20m \
                ../conf/test.properties

provisioner --download

#provisioner --terminate