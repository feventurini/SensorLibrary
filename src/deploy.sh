#!/bin/bash

./destinations.sh

# Provider
BASE_DIR=sensorlibraryprovider
echo "Copying provider files to $PROVIDER/$BASE_DIR"
ssh $PROVIDER rm -r $BASE_DIR
ssh $PROVIDER mkdir $BASE_DIR $BASE_DIR/sensor
scp -p rmi.policy $PROVIDER:$BASE_DIR
scp -rp provider $PROVIDER:$BASE_DIR
scp -rp http $PROVIDER:$BASE_DIR
scp -p sensor/Sensor.class $PROVIDER:$BASE_DIR/sensor

# Sensors
BASE_DIR=sensorlibrarysensor
echo "Copying sensors files to $SENSOR/$BASE_DIR"
ssh $SENSOR rm -r $BASE_DIR
ssh $SENSOR mkdir $BASE_DIR $BASE_DIR/provider
scp -p rmi.policy $SENSOR:$BASE_DIR
scp -rp sensor $SENSOR:$BASE_DIR
scp -rp http $SENSOR:$BASE_DIR
scp -rp implementations $SENSOR:$BASE_DIR
scp -p provider/Provider.class $SENSOR:$BASE_DIR/provider

# Client
BASE_DIR=sensorlibraryclient
echo "Copying client files to $CLIENT/$BASE_DIR"
ssh $CLIENT rm -r $BASE_DIR
ssh $CLIENT mkdir $BASE_DIR $BASE_DIR/provider
scp -p rmi.policy $CLIENT:$BASE_DIR
scp -rp sensor $CLIENT:$BASE_DIR
scp -rp client $CLIENT:$BASE_DIR
scp -p provider/Provider.class $CLIENT:$BASE_DIR/provider