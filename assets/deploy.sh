#!/bin/bash

# Load destinations
PROVIDER_NAME=pi
PROVIDER_HOST=192.168.1.111

STATION_NAME=pi
STATION_HOST=192.168.1.113

CLIENT_NAME=federico
CLIENT_HOST=localhost

# Shortcuts
PROVIDER=$PROVIDER_NAME@$PROVIDER_HOST
STATION=$STATION_NAME@$STATION_HOST
CLIENT=$CLIENT_NAME@$CLIENT_HOST

tar -cf dependencies.tar ../dependency

# Provider needs these resources:
# - http
# - provider
# - sensor/Sensor.class
# - sensor/SensorState.class
# - rmi.policy
# - dependency

BASE_DIR=sensorlibraryprovider
echo ""
echo "Copying provider files to $PROVIDER/$BASE_DIR"
tar -cf provider.tar --files-from provider.txt
ssh $PROVIDER "rm -r $BASE_DIR || mkdir $BASE_DIR"
scp -p provider.tar $PROVIDER:$BASE_DIR
ssh $PROVIDER "cd $BASE_DIR && tar -xf provider.tar"
scp -p dependencies.tar $PROVIDER:$BASE_DIR
ssh $PROVIDER "cd $BASE_DIR && tar -xf dependencies.tar"
rm provider.tar

# Stations need these resources:
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
tar -cf station.tar --files-from station.txt
ssh $STATION "rm -r $BASE_DIR || mkdir $BASE_DIR"
scp -p station.tar $STATION:$BASE_DIR
ssh $STATION "cd $BASE_DIR && tar -xf station.tar"
scp -p dependencies.tar $STATION:$BASE_DIR
ssh $STATION "cd $BASE_DIR && tar -xf dependencies.tar"
rm station.tar

# Clients need these resources:
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
tar -cf client.tar --files-from client.txt
ssh $CLIENT "rm -r $BASE_DIR || mkdir $BASE_DIR"
scp -p client.tar $CLIENT:$BASE_DIR
ssh $CLIENT "cd $BASE_DIR && tar -xf client.tar"
scp -p dependencies.tar $CLIENT:$BASE_DIR
ssh $CLIENT "cd $BASE_DIR && tar -xf dependencies.tar"
rm client.tar
