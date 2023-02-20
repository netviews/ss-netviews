# ss-netviews
This project is implementation of our effort in securing enterprise netwrok environment without depending on perimeter, which is published in SACMAT 2022. Please find the published version of the paper here, [NetViews](https://enck.org/pubs/anjum-sacmat22.pdf).

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

## Running Network Environment
### STEP 01: Getting Netviews Application Ready in ONOS:

1. If no modifications to the onos application code are needed, skip to STEP 2.
2. Go to the netviews-code/ONOS_Apps directory, to chnage and update coorespoding forwarding applications.
3. Change the paths of policy and identity files in the IntentReactiveForwarding [Line 167 and 168], netviews-code/ONOS_Apps/nifwd_combined/app/src/main/java/org/onosproject/nifwd_combined/IntentReactiveForwarding.java, to your local paths
4. Run onos_setup to move files to the $ONOS_ROOT directory (see [README](https://github.com/netviews/ss-netviews/blob/master/ONOS_Apps/README)).
```
cd netviews-code/ONOS_Apps
./onos_setup
```
### STEP 02: Get ONOS Ready
1. Build and run ONOS as normal to ensure proper compilation and startup:
```
cd onos
bazel run onos-local -- clean debug
```
2. From another terminal, run the ONOS CLI and activate the desired application (assuming STEP 01 is completed successfully):
```
$ONOS_ROOT/tools/test/bin/onos localhost
app activate nifwd_combined [for nifwd application, assuming ]
app activate ifwd [for ifwd application]
app activate fwd [for reactive forwarding application]
```

### STEP 03: Get Mininet Ready
1. There are multiple versions of the Mininet. [This]{https://github.com/netviews/ss-netviews/blob/master/experiments/basic_mininet_script} version reads given topology and deploys the network environment.

2. This command will run the mininet sript:
```
sudo ./basic_mininet_script -t [path to specific topology input file] -c [run/generate] -d [where to store the results of experiment] -e [things you want your hosts to do] -a [the type of forwarding application you are using]
```
Here, "generate" command is used for generating the info type inputfile needed for identity mapping.

A example run:
```
sudo ./basic_mininet_script -t ../topology-json/demo-topo-ref/topo-ref.json -c run  -d ./testing -e run_without_experiment -a org.onosproject.nifwd_combined
```

### STEP 04: Working with the Environment
At this point you have access to the ONOS server and application logs on terminal 1, the ONOS CLI for debugging on terminal 2, and the application output on terminal 3.
Use tcpdump, Wireshark, or tshark to capture and analyze packets and network behavior.

### Running Experiments

1. Build and run ONOS as normal to ensure proper compilation and startup:
```	
bazel run onos-local -- clean debug
```
2. Stop ONOS and kill ONOS processes:
```
pkill -f onos
```
3. In netviews-code/experiments define experiment files and an experiment list, then call run_experiment_set to begin (see examples in the experiments directory).
4. After experiments are complete, use iperf3_to_csv (with wildcard) and mtr_to_csv (with separate flags per file) to create parsed files.
5. Compile parsed files based on desired figures (reference some of the “combined” csv files in the repo for formatting examples).
6. Run box_plot_throughput and bar_plot_latency on the compiled files to create figures. 

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
