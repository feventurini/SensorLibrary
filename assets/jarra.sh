#!/bin/bash

cd .. # dentro la cartella classes
rm -r "../jars"
mkdir "../jars" # accanto alla cartella classes

# nota: nel jar finisce anche la cartella assets
# che però non usiamo perchè usiamo una cartella esterna

echo ""
echo "Creating provider jar into jars/provider"
mkdir "../jars/provider"
mkdir "../jars/provider/assets"
jar cfe "SN_Provider.jar" provider.ProviderRMI @assets/provider.txt
mv "SN_Provider.jar" "../jars/provider/"
cp "assets/rmi.policy" "../jars/provider/assets/"

echo ""
echo "Creating client jar into jars/client"
mkdir "../jars/client"
mkdir "../jars/client/assets"
cp "../dependency/reflections-0.9.10.jar" "../jars/client/"
cp "../dependency/guava-15.0.jar" "../jars/client/"
cp "../dependency/javassist-3.19.0-GA.jar" "../jars/client/"
cp "../dependency/annotations-2.0.1.jar" "../jars/client/"
jar cfe "SN_Client.jar" client.TestUser @assets/client.txt
mv "SN_Client.jar" "../jars/client/"
cp "assets/rmi.policy" "../jars/client/assets"

echo ""
echo "Creating station jar into jars/station"
mkdir "../jars/station"
mkdir "../jars/station/assets"
cp "../dependency/pi4j-core-1.0.jar" "../jars/station/"
cp "../dependency/pi4j-1.0.jar" "../jars/station/"
cp "../../lib/grovepi-pi4j.jar" "../jars/station/"
jar cfe "SN_Station.jar" station.StationImpl @assets/station.txt
mv "SN_Station.jar" "../jars/station/"
cp "assets/rmi.policy" "../jars/station/assets"
cp assets/*.properties "../jars/station/assets"
cp assets/*.xml "../jars/station/assets"

echo ""
echo "-------------------"
echo "  JARring SUCCESS"
echo "-------------------"
