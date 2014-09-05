#!/bin/bash

provisioner --scale 25
provisioner --restart

coordinator --memberWorkerCount 10 \
	--clientWorkerCount 100 \
	--duration 10m \
	--workerClassPath   '../target/*.jar' \
	--clientHzFile      ../conf/client-hazelcast.xml \
    --hzFile            ../conf/hazelcast.xml \
	--workerVmOptions "-XX:+HeapDumpOnOutOfMemoryError" \
	--parallel \
	../conf/test.properties

provisioner --download

#provisioner --terminate