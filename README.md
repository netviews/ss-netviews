# ss-netviews
## Random Policy Graph Generation
To understand the impact of the Policy Engine on the overall NetViews overhead, we analyzed the response time of policy-machine-core using random policy graphs.


## Environment Setup
### Mininet Installation
```
$  git clone https://github.com/mininet/mininet
$  cd mininet
~/mininet$    git tag (optional)
~/mininet$    git checkout -b 2.3.0d6
~/mininet$    ./util/install.sh -fnv
~/mininet$    sudo apt install net-tools
( -fnv: OpenFLow, Dependencies/Core files, OVS Switch )

```
### ONOS Installation
```
$    git clone https://gerrit.onosproject.org/onos
$    cd onos
~/onos$    git checkout onos-2.3
~/onos$    bazel build onos
```
Add the following 2 lines to ~/.bashrc:
```
export ONOS_ROOT=~/onos
source $ONOS_ROOT/tools/dev/bash_profile
```
Run ONOS using following command and wait till log stops
```
~/onos$    bazel run onos-local -- clean debug
```
