#!/bin/bash

provisioner --scale 30
provisioner --restart

coordinator --memberWorkerCount 6 \
	--clientWorkerCount 150 \
	--duration 60m \
	--workerClassPath   '../target/*.jar' \
	--clientHzFile      ../conf/client-hazelcast.xml \
    --hzFile            ../conf/hazelcast.xml \
	--workerVmOptions "-XX:+HeapDumpOnOutOfMemoryError" \
	--parallel \
	../conf/test.properties

provisioner --download

#provisioner --terminate