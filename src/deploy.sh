#!/bin/bash

# Load destinations
PROVIDER_NAME=federico
PROVIDER_HOST=192.168.43.91

STATION_NAME=pi
STATION_HOST=192.168.43.107

CLIENT_NAME=fede
CLIENT_HOST=192.168.43.61

# Shortcuts
PROVIDER=$PROVIDER_NAME@$PROVIDER_HOST
STATION=$STATION_NAME@$STATION_HOST
CLIENT=$CLIENT_NAME@$CLIENT_HOST



# Provider needs these resources:
# - http
# - provider
# - sensor/Sensor.class
# - sensor/SensorState.class
# - sensor/SensorState$State.class
# - rmi.policy
# - dependency

BASE_DIR=sensorlibraryprovider
echo ""
echo "Copying provider files to $PROVIDER/$BASE_DIR"
ssh $PROVIDER "rm -r $BASE_DIR"
ssh $PROVIDER "mkdir $BASE_DIR $BASE_DIR/sensor"
scp -rp http $PROVIDER:$BASE_DIR
scp -rp provider $PROVIDER:$BASE_DIR
scp -p sensor/Sensor.class $PROVIDER:$BASE_DIR/sensor
scp -p sensor/SensorState.class $PROVIDER:$BASE_DIR/sensor
scp -p sensor/SensorState$State.class $PROVIDER:$BASE_DIR/sensor
scp -p rmi.policy $PROVIDER:$BASE_DIR
scp -rp ../dependency $PROVIDER:$BASE_DIR



# Stations need these resources:
# - http
# - provider/Provider.class
# - sensor
# - sensor.implementations
# - station
# - station.properties
# - rmi.policy
# - dependency

BASE_DIR=sensorlibrarystation
echo ""
echo "Copying sensors files to $SENSOR/$BASE_DIR"
ssh $STATION rm -r $BASE_DIR
ssh $STATION mkdir $BASE_DIR $BASE_DIR/provider
scp -rp http $STATION:$BASE_DIR
scp -p provider/Provider.class $STATION:$BASE_DIR/provider
scp -rp sensor $STATION:$BASE_DIR
scp -rp sensor.implementations $STATION:$BASE_DIR
scp -rp station $STATION:$BASE_DIR
scp -p station.properties $STATION:$BASE_DIR
scp -p TempAndHumiditySensor.properties $STATION:$BASE_DIR
scp -p Temp4000.properties $STATION:$BASE_DIR
scp -p Temp2000.properties $STATION:$BASE_DIR
scp -p Rfid_SL030.properties $STATION:$BASE_DIR
scp -p rmi.policy $STATION:$BASE_DIR
scp -rp ../dependency $STATION:$BASE_DIR

# Clients need these resources:
# - http
# - client
# - provider/Provider.class
# - sensor
# - rmi.policy
# - dependency

BASE_DIR=sensorlibraryclient
echo ""
echo "Copying client files to $CLIENT/$BASE_DIR"
ssh $CLIENT rm -r $BASE_DIR
ssh $CLIENT mkdir $BASE_DIR $BASE_DIR/provider
scp -rp http $CLIENT:$BASE_DIR
scp -rp client $CLIENT:$BASE_DIR
scp -p provider/Provider.class $CLIENT:$BASE_DIR/provider
scp -rp sensor $CLIENT:$BASE_DIR
scp -p rmi.policy $CLIENT:$BASE_DIR
scp -rp ../dependency $CLIENT:$BASE_DIR
