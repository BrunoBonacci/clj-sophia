#!/bin/bash
LOC=`pwd`

# clean checkout
cd $(dirname $0)
rm -fr ./sophia
git clone https://github.com/pmwkaa/sophia.git
cd $(dirname $0)/sophia
echo "Checking out v2.2 - 669d57b"
#git checkout -b remotes/origin/v2.2
git checkout 669d57b

# build
make
if [ `uname` == "Darwin" ] ; then
    g++ -fpic -shared -Wl,-all_load libsophia.a -o libsophia.dylib
fi

echo "All done!"
ls -1 libsophia*
