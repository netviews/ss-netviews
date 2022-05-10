package org.onosproject.nifwd;

/*import org.apache.felix.scr.annotations.Activate;
     import org.apache.felix.scr.annotations.Component;
     import org.apache.felix.scr.annotations.Deactivate;
     import org.apache.felix.scr.annotations.Reference;
     import org.apache.felix.scr.annotations.ReferenceCardinality;
 */
import org.onlab.packet.IpAddress;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onlab.packet.IPacket;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.TpPort;

import java.io.FileNotFoundException;
import java.io.IOException;
import gov.nist.csd.pm.exceptions.PMException;
import java.util.Arrays;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

//import org.onosproject.net.NetviewsService;

import java.util.EnumSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.np.impl.PolicyEngine;
import org.onosproject.np.impl.IdentityMap;

// For the extension from VPLS.
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/**
 * Direction for Intent installation.
 */
enum Direction {
	ADD,
	REMOVE
}

/**
 * WORK-IN-PROGRESS: Sample reactive forwarding application using intent framework.
 */
@Component(immediate = true)
public class IntentReactiveForwarding {

	//String filePath = "/tmp/ifwd.log";

	private final Logger log = getLogger(getClass());

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected TopologyService topologyService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected PacketService packetService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected IntentService intentService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected HostService hostService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowObjectiveService flowObjectiveService;

//	@Reference(cardinality = ReferenceCardinality.MANDATORY)
//	protected NetviewsPolicy netViews;
	private PolicyEngine netViews = new PolicyEngine();

	private ReactivePacketProcessor processor = new ReactivePacketProcessor();
	private ApplicationId appId;

	private IdentityMap identityMap;
	private PolicyEngine policyEngine;

	private static final int DROP_RULE_TIMEOUT = 300;

	private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
																			IntentState.WITHDRAWING,
																			IntentState.WITHDRAW_REQ);

	private Exception error;

	private static final int OPERATION_TIMEOUT = 1;

	@Activate
	public void activate() throws Exception {
		appId = coreService.registerApplication("org.onosproject.nifwd");

		packetService.addProcessor(processor, PacketProcessor.director(2));

		// FIXME: Hard coded paths
		identityMap = new IdentityMap();
		//identityMap.createMapping("/home/dkostecki/Documents/net-views/netviews-code/ONOS Apps/nifwd/src/ref_topo.json");
		identityMap.createMapping("/home/dan/netviews-code/ONOS Apps/nifwd/src/ref_topo.json");

		policyEngine = new PolicyEngine();
		//policyEngine.createPolicyGraph("/home/dkostecki/Documents/net-views/netviews-code/ONOS Apps/nifwd/src/ref_policy.json");
		policyEngine.createPolicyGraph("/home/dan/netviews-code/ONOS Apps/nifwd/src/ref_policy.json");

		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

		selector.matchEthType(Ethernet.TYPE_IPV4);

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
		log.info("$$$$$$$$$$$$$$$$ Started");
	}

	@Deactivate
	public void deactivate() {
		packetService.removeProcessor(processor);
		processor = null;
		log.info("Stopped");
	}

	/**
	 * Packet processor responsible for forwarding packets along their paths.
	 */
	private class ReactivePacketProcessor implements PacketProcessor  {
		@Override
		public void process(PacketContext context)  {
			// Stop processing if the packet has been handled, since we
			// can't do any more to it.
			//log.info("$$$$$$$ We started handling $$$$$$$$$");
			if (context.isHandled()) {
				return;
			}

			//long inTime = System.nanoTime();

			InboundPacket pkt = context.inPacket();
			Ethernet ethPkt = pkt.parsed();

			if (ethPkt == null) {
				return;
			}

			HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
			HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

			// Do we know who this is for? If not, flood and bail.
			Host dst = hostService.getHost(dstId);
			if (dst == null) {
				flood(context);
				return;
			}

			IPacket ethPayload = ethPkt.getPayload();

			String sourceIP = "";
			String destIP = "";
			String sourceMAC = (ethPkt.getSourceMAC()).toString();
			String destMAC = (ethPkt.getDestinationMAC()).toString();
			String ingressPort = ((pkt.receivedFrom()).port()).toString();
			String egressPort = dst.location().port().toString(); // CURRENTLY UNUSED

			String protocol = "";
			String sourcePort = "";
			String destPort = "";

			String code = "";
			String type = "";

			TrafficSelector.Builder outSelector = DefaultTrafficSelector.builder()
					.matchEthSrc(ethPkt.getSourceMAC())
					.matchEthDst(ethPkt.getDestinationMAC())
					.matchEthType(ethPkt.getEtherType());
			TrafficSelector.Builder inSelector = DefaultTrafficSelector.builder()
					.matchEthSrc(ethPkt.getDestinationMAC())
					.matchEthDst(ethPkt.getSourceMAC())
					.matchEthType(ethPkt.getEtherType());

			//ARP packet: Allow
			if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
				String pseudoKey =  (srcId.toString() + dstId.toString() + "arp");
				String inPseudoKey = (dstId.toString() + srcId.toString() + "arp");
				setUpConnectivity(context, srcId, dstId, outSelector.build(), pseudoKey);//, inTime);
				setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey);//, inTime);
				forwardPacketToDst(context, dst);
				return;
			}
			else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
				IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
				byte ipv4Protocol = ipv4Packet.getProtocol();
				int sourceAddress = ipv4Packet.getSourceAddress();
				int destinationAddress = ipv4Packet.getDestinationAddress();
				sourceIP = IpAddress.valueOf(sourceAddress).toString();
				destIP = IpAddress.valueOf(destinationAddress).toString();

				if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
					TCP tcpPacket = (TCP) ipv4Packet.getPayload();
					protocol = "TCP";
					sourcePort = Integer.toString(tcpPacket.getSourcePort());
					destPort = Integer.toString(tcpPacket.getDestinationPort());

					outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//outSelector = outSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
							outSelector = outSelector.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
					//inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
							inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()));
				}
				if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
					UDP udpPacket = (UDP) ipv4Packet.getPayload();
					protocol = "UDP";
					/*sourcePort = Integer.toString(udpPacket.getSourcePort());
					destPort = Integer.toString(udpPacket.getDestinationPort());
					outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					outSelector = outSelector.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()));*/
					sourcePort = Integer.toString(udpPacket.getSourcePort());
					destPort = Integer.toString(udpPacket.getDestinationPort());

					outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//outSelector = outSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
							outSelector = outSelector.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					//inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
							inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()));
				}
				if (ipv4Protocol == IPv4.PROTOCOL_ICMP) {
					ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
					protocol = "ICMP";
					code = String.valueOf(icmpPacket.getIcmpCode());
					type = String.valueOf(icmpPacket.getIcmpType());
					//selector = selector.matchIcmpCode(icmpPacket.getIcmpCode())
					//		.matchIcmpType(icmpPacket.getIcmpType());
				}
			}
			else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
				IPv6 ipv6Packet = (IPv6) ethPkt.getPayload();
				byte ipv6Protocol = ipv6Packet.getNextHeader();
				sourceIP = Byte.toString(ipv6Packet.getSourceAddress()[0]);
				destIP = Byte.toString(ipv6Packet.getDestinationAddress()[0]);
				if (ipv6Protocol == IPv6.PROTOCOL_TCP) {
					TCP tcpPacket = (TCP) ipv6Packet.getPayload();
					protocol = "TCP";
					sourcePort = Integer.toString(tcpPacket.getSourcePort());
					destPort = Integer.toString(tcpPacket.getDestinationPort());
					outSelector = outSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
							.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
					inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
							.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
				}
				if (ipv6Protocol == IPv6.PROTOCOL_UDP) {
					UDP udpPacket = (UDP) ipv6Packet.getPayload();
					protocol = "UDP";
					sourcePort = Integer.toString(udpPacket.getSourcePort());
					destPort = Integer.toString(udpPacket.getDestinationPort());
					outSelector = outSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
							.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()))
							.matchUdpDst(TpPort.tpPort(udpPacket.getSourcePort()));
				}
				if (ipv6Protocol == IPv6.PROTOCOL_ICMP6) {
					ICMP icmpPacket = (ICMP) ipv6Packet.getPayload();
					protocol = "ICMP";
					code = String.valueOf(icmpPacket.getIcmpCode());
					type = String.valueOf(icmpPacket.getIcmpType());
					//selector = selector.matchIcmpCode(icmpPacket.getIcmpCode())
					//		.matchIcmpType(icmpPacket.getIcmpType());
				}
			}

			//log.info("\n###"+sourceIP + " "+ sourceMAC+" "+ ingressPort+" "+ destIP+" "+ destMAC+" "+ egressPort+" "+ protocol+" "+ sourcePort+" "+ destPort+"###\n");

			// Otherwise forward and be done with it

			boolean addIntent = false;
			try {
				//log.info("\n Request Intent");
				addIntent = netViewsPolicyPlaceholder(sourceIP, sourceMAC, ingressPort, destIP, destMAC, egressPort, protocol, sourcePort, destPort);
				//log.info("\nReceived "+addIntent);
			} catch (Exception e) {
				log.info("\nexception:\n");
				log.info(e.getLocalizedMessage());
			}

			String outPseudoKey = "";
			String inPseudoKey = "";

			outPseudoKey += (srcId.toString() + dstId.toString());
			inPseudoKey += (dstId.toString() + srcId.toString());

			//outPseudoKey += (sourceIP + sourceMAC + ingressPort + destIP + destMAC + egressPort + protocol + sourcePort + destPort + code + type);
			//inPseudoKey += (destIP + destMAC + egressPort + sourceIP + sourceMAC + ingressPort + protocol + destPort + sourcePort + code + type);

			outPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + destPort);//sourcePort + destPort + code + type);
			inPseudoKey += (destIP + destMAC + sourceIP + sourceMAC + protocol + destPort + "RETURN"); //+ sourcePort + code + type);

			Key key;
			key = Key.of(outPseudoKey, appId);
			//log.info("\nChecking Key: " + key.toString() + "\n");

			if (addIntent || intentService.getIntentState(key) == IntentState.INSTALLED) {
				setUpConnectivity(context, srcId, dstId, outSelector.build(), outPseudoKey);//, inTime);
				setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey);//, inTime);
				forwardPacketToDst(context, dst);
			} else {
				// The intent should not be installed.
				return;
			}
		}
	}

	private boolean netViewsPolicyPlaceholder(
			String sourceIP, String sourceMAC, String ingressPort,
			String destIP, String destMAC, String egressPort,
			String protocol, String sourcePort, String destPort) throws IOException, PMException {

		/*String logging = "\n";
		logging += ("sourceIP: " + sourceIP + "\n");
		logging += ("sourceMAC: " + sourceMAC + "\n");
		logging += ("ingressPort: " + ingressPort + "\n");
		logging += ("destIP: " + destIP + "\n");
		logging += ("destMAC: " + destMAC + "\n");
		logging += ("egressPort: " + egressPort + "\n");
		logging += ("protocol: " + protocol + "\n");
		logging += ("sourcePort: " + sourcePort + "\n");
		logging += ("destPort: " + destPort + "\n");

		log.info(logging);
		*/
		long inTime = System.nanoTime();
		
		boolean permission = false;
		/*
		log.info("\n Request Permission");
		String subjectName = identityMap.getHostIdentity(sourceIP).getName();
		String objectName = identityMap.getHostIdentity(destIP).getName();
		String actionName = protocol.toLowerCase()+"/"+destPort;

		log.info("\n @@@@@ Got 	Name: " + subjectName+" "+objectName+" "+actionName);
		*/
		//permission = policyEngine.getPermission(subjectName, objectName, actionName);
		
		permission = policyEngine.getPermission();

		long outTime = System.nanoTime();
		log.info("\n\nPOLICY FETCH TIME: " + String.valueOf(outTime - inTime) + "\n\n");
		log.info("\n@@@@@ " + permission + " @@@@@@");
		
		return permission;
		//return true;
	}

	// Floods the specified packet if permissible.
	private void flood(PacketContext context) {
		if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
											 context.inPacket().receivedFrom())) {
			packetOut(context, PortNumber.FLOOD);
		} else {
			context.block();
		}
	}

	// Sends a packet out the specified port.
	private void packetOut(PacketContext context, PortNumber portNumber) {
		context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}

	private void forwardPacketToDst(PacketContext context, Host dst) {
		TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
		OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
														  treatment, context.inPacket().unparsed());
		packetService.emit(packet);
		log.info("sending packet: {}", packet);
	}

	// Install a rule forwarding the packet to the specified port.
	private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId, TrafficSelector selector, String pseudoKey) {//, Long inTime) {
		//TrafficSelector selector = DefaultTrafficSelector.emptySelector();
		TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

		Key key;
	  /*if (srcId.toString().compareTo(dstId.toString()) < 0) {
	    key = Key.of(srcId.toString() + dstId.toString(), appId);
	  } else {
	    key = Key.of(dstId.toString() + srcId.toString(), appId);
	  }*/
		key = Key.of(pseudoKey, appId);

		// UNCOMMENT FOR JUST THE TIME TO INSTALL AN INTENT, WITHOUT NETVIEWS OVERHEAD.
		//inTime = System.nanoTime();

		HostToHostIntent intent = (HostToHostIntent) intentService.getIntent(key);
		// TODO handle the FAILED state
		if (intent != null) {
			if (WITHDRAWN_STATES.contains(intentService.getIntentState(key))) {
				HostToHostIntent hostIntent = HostToHostIntent.builder()
						.appId(appId)
						.key(key)
						.one(srcId)
						.two(dstId)
						.selector(selector)
						.treatment(treatment)
						.build();

				intentService.submit(hostIntent);
				//applyIntentSync(hostIntent, Direction.ADD);
			} else if (intentService.getIntentState(key) == IntentState.FAILED) {

				TrafficSelector objectiveSelector = DefaultTrafficSelector.builder()
						.matchEthSrc(srcId.mac()).matchEthDst(dstId.mac()).build();

				TrafficTreatment dropTreatment = DefaultTrafficTreatment.builder()
						.drop().build();

				ForwardingObjective objective = DefaultForwardingObjective.builder()
						.withSelector(objectiveSelector)
						.withTreatment(dropTreatment)
						.fromApp(appId)
						.withPriority(intent.priority() - 1)
						.makeTemporary(DROP_RULE_TIMEOUT)
						.withFlag(ForwardingObjective.Flag.VERSATILE)
						.add();

				flowObjectiveService.forward(context.outPacket().sendThrough(), objective);
			}

		} else if (intent == null) {
			HostToHostIntent hostIntent = HostToHostIntent.builder()
					.appId(appId)
					.key(key)
					.one(srcId)
					.two(dstId)
					.selector(selector)
					.treatment(treatment)
					.build();

			intentService.submit(hostIntent);
			//applyIntentSync(hostIntent, Direction.ADD);
		}

		//ComputeTime computeTime = new ComputeTime(inTime, key);
		//Thread t = new Thread(computeTime);
		//t.start();
	}

	public class ComputeTime implements Runnable {
		Long inTime;
		Key key;
		public ComputeTime(Long inTime, Key key) {
			this.inTime = inTime;
			this.key = key;
		}

		public void run() {
			while(intentService.getIntentState(key) != IntentState.INSTALLED) {
				//do nothing
			}
			long outTime = System.nanoTime();
			log.info("Intent " + (intentService.getIntent(key).id()).toString() + ": " + key.toString() + " Installed Time: " + String.valueOf(outTime - inTime) + "\n\n");
		}
	}

	/**
	 * Applies Intents synchronously with a specific direction.
	 *
	 * @param intents the Intents
	 * @param direction the direction
	 */
	 /*private void applyIntentSync(Intent intent, Direction direction) {
		 //Set<Key> pendingIntentKeys = intents.stream().map(Intent::key).collect(Collectors.toSet());
		 Key pendingIntentKey = intent.key();
		 IntentCompleter completer;

		 switch (direction) {
			 case ADD:
				 completer = new IntentCompleter(pendingIntentKey, IntentEvent.Type.INSTALLED);
				 intentService.addListener(completer);
				 intentService.submit(intent);
				 //intents.forEach(intentService::submit);
				 break;
			 case REMOVE:
				 completer = new IntentCompleter(pendingIntentKey, IntentEvent.Type.WITHDRAWN);
				 //intentService.addListener(completer);
				 intentService.withdraw(intent);
				 //intents.forEach(intentService::withdraw);
				 break;
			 default:
				 log.warn("Unknown Intent Direction within applyIntentSync\n");
				 return;
		 }

		 try {
			 // Wait until Intent operation is completed.
			 completer.complete();
		 } catch (Exception e) {
			 this.error = e;
		 } finally {
			 intentService.removeListener(completer);
		 }
	 }
	 */
	 /**
		* Helper class which monitors if all Intent operations are completed.
		*/
	 /*class IntentCompleter implements IntentListener {
		 private static final String INTENT_COMPILE_ERR = "Got {} from intent completer";
		 private CompletableFuture<Void> completableFuture;
		 private Key pendingIntentKey;
		 private IntentEvent.Type expectedEventType;
		 */
		 /**
			* Initialize completer with given Intent keys and expected Intent event type.
			*
			* @param pendingIntentKey the Intent key to wait for
			* @param expectedEventType the expected Intent event type
			*/
			 /*public IntentCompleter(Key pendingIntentKey, IntentEvent.Type expectedEventType) {
				 this.completableFuture = new CompletableFuture<>();
				 this.pendingIntentKey = pendingIntentKey;
				 this.expectedEventType = expectedEventType;
			 }

			 @Override
			 public void event(IntentEvent event) {
				 Intent intent = event.subject();
				 Key key = intent.key();
				 if (!(pendingIntentKey == key)) {
					 // Ignore Intent events from other.
					 return;
				 }
				 // Intent failed, throw an exception to the completable future.
				 if (event.type() == IntentEvent.Type.CORRUPT ||
						 event.type() == IntentEvent.Type.FAILED) {
					 completableFuture.completeExceptionally(new IntentException(intent.toString()));
					 return;
				 }
				 // If the event type matched the expected type, no longer pending.
				 if (event.type() == expectedEventType) {
					 //log.info("\n\nPending Key before null: " + pendingIntentKey.toString() + "\n\n");
					 pendingIntentKey = null;
					 //log.info("\n\nPending Key after null: " + pendingIntentKey.toString() + "\n\n");
					 completableFuture.complete(null);
				 }
			 }*/

			 /**
				* Waits until pending Intent is completed to timeout.
				*/
			 /*public void complete() {
				 //log.info("\n\nPending Key: " + pendingIntentKey.toString() + "\n\n");
				 // If no pending Intent keys, complete directly
				 if (pendingIntentKey == null) {
					 return;
				 }
				 try {
					 completableFuture.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
				 } catch (TimeoutException | InterruptedException |
									ExecutionException | IntentException e) {
						 // TODO: handle errors more carefully
						 log.warn(INTENT_COMPILE_ERR, e.toString());
						 //throw new Exception(e.toString());
				 }
			 }
	 }*/
}
