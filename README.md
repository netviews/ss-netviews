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

## Attack Graph Generation
Attack graph visualizing possible reconnaissance and lateral movement from a compromised host. [Here](https://github.com/netviews/ss-netviews), we use similar technique as Lippmann et al.
```
Richard Lippmann, Kyle Ingols, Chris Scott, Keith Piwowarski, Kendra Kratkiewicz, Mike Artz, and Robert Cunningham. 2006. Validating and Restor- ing Defense in Depth Using Attack Graphs. In Proceedings of the IEEE Military Communications conference (MILCOM).
```

## Network Environment Setup
### Mininet Installation
```
git clone https://github.com/mininet/mininet
cd mininet
git tag (optional)
git checkout -b 2.3.0d6
./util/install.sh -fnv
sudo apt install net-tools
( -fnv: OpenFLow, Dependencies/Core files, OVS Switch )
```
### ONOS Installation
```
git clone https://gerrit.onosproject.org/onos
cd onos
git checkout onos-2.3
bazel build onos
```
Add the following 2 lines to ~/.bashrc:
```
export ONOS_ROOT=~/onos
source $ONOS_ROOT/tools/dev/bash_profile
```
Run ONOS using following command and wait till log stops to ensure proper compilation and startup:
```
bazel run onos-local -- clean debug
```
From another terminal, run the ONOS CLI and activate the desired application:
```
$ONOS_ROOT/tools/test/bin/onos localhost
```
For activating Netviews (nifwd) application:
```
app deactivate [other existing forwarding application]
app activate nifwd_combined
```
For activating ONOS Intent (ifwd) application:

```
app deactivate [other existing forwarding application]
app activate ifwd
```
For activating ONOS Reactive Forwarding application:
```
app deactivate [other existing forwarding application]
app activate fwd
```

## Policy, Topology and Identity Files
[Here](https://github.com/netviews/ss-netviews/tree/master/input-files), we have differnet types of topology, sample policies and intentity files used in system experiments.
| Topology | Folder Name | 
| ----------- | ----------- | 
| Reference Topology | topo-ref | 
| Cisco | topo-cisco | 
| Ministanford | topo-ministanford-[]host[]server|

## Getting Netviews Application Ready in ONOS:

1. If no modifications to the onos application code are needed, skip to step 5.
2. Go to the netviews-code/ONOS_Apps directory
3. Change the paths of policy and identity files in the IntentReactiveForwarding [Line 167 and 168], netviews-code/ONOS_Apps/nifwd_combined/app/src/main/java/org/onosproject/nifwd_combined/IntentReactiveForwarding.java, to your local paths
4. Run onos_setup to move files to the $ONOS_ROOT directory (see [README](https://github.com/netviews/ss-netviews/blob/master/ONOS_Apps/README)).
5. Build ONOS, activate cooresponding forwarding application


