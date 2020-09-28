#!/bin/bash

rm ../../lib/tfmteleco-1.0.jar 
cp ../../build/libs/tfmteleco-1.0.jar ../../lib/
java -cp "../../lib/*" DriveQuickstart