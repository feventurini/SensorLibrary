#!/bin/bash

cd .. # dentro la cartella classes
rm -r "../jars"
mkdir "../jars" # accanto alla cartella classes

echo ""
echo "Creating provider jar into jars/provider"
mkdir "../jars/provider"
jar cf "SN_Provider.jar" @assets/provider.txt -e provider.ProviderRMI

echo ""
echo "Creating client jar into jars/client"
mkdir "../jars/client"
cp "../dependency/reflections-0.9.10.jar" "jars/client"
cp "../dependency/guava-15.0.jar" "jars/client"
cp "../dependency/javassist-3.19.0-GA.jar" "jars/client"
cp "../dependency/annotations-2.0.1.jar" "jars/client"
jar cf "SN_Provider.jar" @assets/provider.txt -e client.Tclient

echo ""
echo "Creating station jar into jars/station"
mkdir "jars/station"
cp "../dependency/pi4j-core-1.0.jar" "jars/station"
cp "../dependency/grovepi-pi4j.jar" "jars/station"
jar cf "SN_Provider.jar" @assets/provider.txt -e client.TestUser

echo ""
echo "-------------------"
echo "  JARring SUCCESS"
echo "-------------------"
