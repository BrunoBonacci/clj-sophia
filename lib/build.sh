#!/bin/bash
LOC=`pwd`
cd $(dirname $0)
rm -fr ./sophia
git clone git@github.com:pmwkaa/sophia.git
cd $(dirname $0)/sophia
echo "Checking out v2.2"
git checkout -b remotes/origin/v2.2
make
if [ `uname` == "Darwin" ] ; then
    g++ -fpic -shared -Wl,-all_load libsophia.a -o libsophia.dylib
fi
echo "All done!"
ls -1 libsophia*
