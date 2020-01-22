#!/bin/bash
if [ -f "instances" ]
then
    inst=$(cat instances)
else
    export inst=1
fi

java -cp "lib/*" rice.tests.bootstrap.Node $((9002+inst)) dht-master 9001 sssss > output${inst}.log  2>&1 &
echo $! > node${inst}.pid
inst=$((inst+1))
echo -ne $inst > instances
