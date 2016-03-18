#!/bin/bash

# See http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/enhancements-7.html
# for java.rmi.server.useCodebaseOnly=false
# When set to false, this property allows one side of an RMI connection to specify
# a network location (URL) from which the other side of the RMI connection should
# load Java classes. The typical use of this mechanism is for RMI clients and
# servers to be able to provide remote interfaces and data classes to each other,
# without the need for configuration.

./deploy.sh

# Provider
ssh $PROVIDER "cd $BASE_DIR && rmiregistry" &
ssh $PROVIDER "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy provider.ProviderRMI" &
sleep 10

# Sensors
ssh $SENSOR "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy sensor.implementations.Temp2000 $PROVIDER_HOST" &
sleep 10

# Client
ssh $CLIENT "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy client.RmiTemp2000Test $PROVIDER_HOST" &
