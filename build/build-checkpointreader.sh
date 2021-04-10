#!/bin/bash

# Building CheckpointReader

if [ ! -f lib/cdk-2.3.jar ]; then
    echo "Failed to create CheckpointReader.jar. Cannot locate cdk-2.3.jar in ../lib"
    exit -1
fi

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/CheckpointReader/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/jgrapht-core-1.4.0.jar:lib/DENOPTIM-GUI.jar:lib/gson-2.8.6.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create CheckpointReader.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: checkpointreader.CheckpointReader" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/jgrapht-core-1.4.0.jar lib/DENOPTIM-GUI.jar lib/gson-2.8.6.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm CheckpointReader.jar manifest.mf checkpointreader 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf checkpointreader
else
    echo "Failed to create CheckpointReader.jar."
    exit -1
fi

echo "--------------------- Done building CheckpointReader.jar ---------------------"
