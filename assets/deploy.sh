#!/bin/bash

cd ..

# Load destinations
PROVIDER_NAME=federico
PROVIDER_HOST=localhost

STATION_NAME=federico
STATION_HOST=localhost

CLIENT_NAME=federico
CLIENT_HOST=localhost

# Shortcuts
PROVIDER=$PROVIDER_NAME@$PROVIDER_HOST
STATION=$STATION_NAME@$STATION_HOST
CLIENT=$CLIENT_NAME@$CLIENT_HOST

tar -cf dependencies.tar ../dependency

# Provider needs these resources: TO BE UPDATED
# - http
# - provider
# - sensor/Sensor.class
# - sensor/SensorState.class
# - rmi.policy
# - dependency

BASE_DIR=sensorlibraryprovider
echo ""
echo "Copying provider files to $PROVIDER/$BASE_DIR"
ssh $PROVIDER "rm -r $BASE_DIR"
ssh $PROVIDER "mkdir $BASE_DIR"
tar c --files-from assets/provider.txt | ssh $PROVIDER "tar x -C $BASE_DIR"
ssh $PROVIDER "tar -x -C $BASE_DIR" < dependencies.tar

# Stations need these resources: TO BE UPDATED
# - http
# - provider/Provider.class
# - sensor
# - station
# - station.xml
# - rmi.policy
# - dependency

BASE_DIR=sensorlibrarystation
echo ""
echo "Copying station files to $STATION/$BASE_DIR" 
ssh $STATION "rm -r $BASE_DIR" 
ssh $STATION "mkdir $BASE_DIR"
tar c --files-from assets/station.txt | ssh $STATION "tar x -C $BASE_DIR"
ssh $STATION "tar -x -C $BASE_DIR" < dependencies.tar

# Clients need these resources: TO BE UPDATED
# - http
# - client
# - provider/Provider.class
# - sensor/Sensor.class
# - sensor/interfaces
# - rmi.policy
# - dependency

BASE_DIR=sensorlibraryclient
echo ""
echo "Copying client files to $CLIENT/$BASE_DIR"
ssh $CLIENT "rm -r $BASE_DIR"
ssh $CLIENT "mkdir $BASE_DIR"
tar c --files-from assets/client.txt | ssh $CLIENT "tar x -C $BASE_DIR"
ssh $CLIENT "tar -x -C $BASE_DIR" < dependencies.tar

echo ""
echo "------------------"
echo "  DEPLOY SUCCESS"
echo "------------------"