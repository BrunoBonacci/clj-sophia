#!/bin/bash
LOC=`pwd`
git clone git@github.com:pmwkaa/sophia.git
cd $(dirname $0)/sophia
echo "Checking out v2.2"
git checkout -b remotes/origin/v2.2
make
mkdir darwin
g++ -fpic -shared -Wl,-all_load libsophia.a -o darwin/libsophia.dylib
