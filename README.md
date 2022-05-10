# ss-netviews
## Random Policy Graph Generation
To understand the impact of the Policy Engine on the overall NetViews overhead, we analyzed the response time of policy-machine-core using random policy graphs. We used similar techniques as following paper:
```
R. Basnet, S. Mukherjee, V. M. Pagadala, and I. Ray. 2018. An efficient implementa- tion of next generation access control for the mobile health cloud. In Proceedings of the International Conference on Fog and Mobile Edge Computing (FMEC).
```
The implementation is in [here](https://github.com/netviews/ss-netviews/tree/master/random-graph-generation)
To run and generate policy graph:
```
python randomPolicyGraph.py AlgorithmNo UserCount ObjectCount Height
```
To generate policy graph useing algorithm one, two user, one object and one height:
```
python randomPolicyGraph.py 1 2 1 1
```

## Environment Setup
### Mininet Installation
```
$    git clone https://github.com/mininet/mininet
$    cd mininet
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
