#!/bin/bash

# Parameters
PROVIDER_NAME=pi
SENSOR_NAME=pi
CLIENT_NAME=federico
PROVIDER_HOST=192.168.0.18
SENSOR_HOST=192.168.0.18
CLIENT_HOST=localhost

PROVIDER=$PROVIDER_NAME@$PROVIDER_HOST
SENSOR=$SENSOR_NAME@$SENSOR_HOST
CLIENT=$CLIENT_NAME@$CLIENT_HOST


# Provider
BASE_DIR=sensorlibraryprovider
ssh $PROVIDER rm -r $BASE_DIR
ssh $PROVIDER mkdir $BASE_DIR $BASE_DIR/sensor
scp -p rmi.policy $PROVIDER:$BASE_DIR
scp -rp provider $PROVIDER:$BASE_DIR
scp -p sensor/Sensor.class $PROVIDER:$BASE_DIR/sensor
ssh $PROVIDER "cd $BASE_DIR && rmiregistry" &
ssh $PROVIDER "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy provider.ProviderRMI" &
sleep 10

# Sensors
BASE_DIR=sensorlibrarysensor
ssh $SENSOR rm -r $BASE_DIR
ssh $SENSOR mkdir $BASE_DIR $BASE_DIR/provider
scp -p rmi.policy $SENSOR:$BASE_DIR
scp -rp sensor $SENSOR:$BASE_DIR
scp -rp http $SENSOR:$BASE_DIR
scp -rp implementations $SENSOR:$BASE_DIR
scp -p provider/Provider.class $SENSOR:$BASE_DIR/provider
ssh $SENSOR "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy implementations.Temp2000 $PROVIDER_HOST" & 
# forse ci vogliono  -Djava.rmi.server.codebase=http://$SENSOR_HOST/ -Djava.rmi.server.hostname=$SENSOR_HOST ma dovrebbero essere dentro http server
sleep 10

# Client
BASE_DIR=sensorlibraryclient
ssh $CLIENT rm -r $BASE_DIR
ssh $CLIENT mkdir $BASE_DIR $BASE_DIR/provider
scp -p rmi.policy $CLIENT:$BASE_DIR
scp -rp sensor $CLIENT:$BASE_DIR
scp -rp client $CLIENT:$BASE_DIR
scp -p provider/Provider.class $CLIENT:$BASE_DIR/provider
ssh $CLIENT "cd $BASE_DIR && java -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=rmi.policy client.RmiTemp2000Test $PROVIDER_HOST" &