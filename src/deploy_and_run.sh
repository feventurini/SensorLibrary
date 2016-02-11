#!/bin/bash

./deploy.sh

# Provider
ssh $PROVIDER "cd $BASE_DIR && rmiregistry" &
ssh $PROVIDER "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy provider.ProviderRMI" &
sleep 10

# Sensors
ssh $SENSOR "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy implementations.Temp2000 $PROVIDER_HOST" & 
sleep 10

# Client
ssh $CLIENT "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy client.RmiTemp2000Test $PROVIDER_HOST" &