package org.onosproject.nifwd_combined.impl;

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
import org.onlab.packet.ARP;

import java.io.*;
import gov.nist.csd.pm.exceptions.PMException;
import java.util.Arrays;

//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Scanner;

import gov.nist.csd.pm.exceptions.PMException;

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

//import org.onosproject.np.impl.PolicyEngine;
//import org.onosproject.np.impl.IdentityMap;

// For the extension from VPLS.
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.ExecutionException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// INSTALLED, WITHDRAWN, FAILED, etc.
import static org.onosproject.net.intent.IntentState.*;

import java.util.WeakHashMap;


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

	// A hashmap to cache the intents returning from the server.
	WeakHashMap<String, Boolean> return_key_cache = new WeakHashMap<>();

	private static final long TIMEOUT = 30;
	
	int PRIORITY = (1 << 16) - 1;

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
	private PolicyEngine netViews = PolicyEngine.getInstance();

	private ReactivePacketProcessor processor = new ReactivePacketProcessor();
	private ApplicationId appId;

	private IdentityMap identityMap;
	private PolicyEngine policyEngine;
		

	private static final int DROP_RULE_TIMEOUT = 300;

	private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
																			IntentState.WITHDRAWING,
																			IntentState.WITHDRAW_REQ);

	private Exception error;
	
	private Thread serverThread;

	private static final int OPERATION_TIMEOUT = 1;
	
	@Activate
	public void activate() throws Exception {
		appId = coreService.registerApplication("org.onosproject.nifwd_combined");

		packetService.addProcessor(processor, PacketProcessor.director(2));

		// FIXME: Hard coded paths
		identityMap = new IdentityMap();
		policyEngine = PolicyEngine.getInstance();

		
		identityMap.createMapping("/home/noah/netviews/ss-netviews/input-files/med-topo-ref/med-topo-info.json");
		policyEngine.createPolicyGraph("/home/noah/netviews/ss-netviews/input-files/med-topo-ref/med-topo-policy.json");
                

		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

		selector.matchEthType(Ethernet.TYPE_IPV4);

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
		log.info("$$$$$$$$$$$$$$$$ Started");
		
		//Create a thread to host a socket server and start it.
		ServerThread thread = new ServerThread();
	    	serverThread = new Thread(thread);
	    	serverThread.start();
		log.info("$$$$$$$$$$$$$$$$ Starting SocketServer on port 9191");
		
//		Endpoint.publish("http://localhost:8080/policyenginecontroller", 
//		          new PolicyEngineController());
//		log.info("SOAP api endpoint has been published.");
	}

	@Deactivate
	public void deactivate() {
		packetService.removeProcessor(processor);
		processor = null;
		log.info("Stopped");
		
		try {
			serverThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("$$$$$$$$$$$$$$$$ Stopping SocketServer on port 9191");
	}

	/**
	 * Packet processor responsible for forwarding packets along their paths.
	 */
	private class ReactivePacketProcessor implements PacketProcessor  {
		@Override
		public void process(PacketContext context)  {
			// Stop processing if the packet has been handled, since we
			// can't do any more to it.
			if (context.isHandled()) {
				return;
			}
			//log.info("\nPacket_in\n");
			//log.info("\nThreadname: " + Thread.currentThread().getName());
			//log.info("\nSwitch: " + context.inPacket().receivedFrom());
			long inTime = System.nanoTime();

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
				protocol = "ARP";
				/*outSelector = DefaultTrafficSelector.builder()
					//.matchArpOp(((ARP) ethPkt.getPayload()).getOpCode())
					.matchArpSha(ethPkt.getSourceMAC())
					.matchArpTha(ethPkt.getDestinationMAC());
				inSelector = DefaultTrafficSelector.builder()
                                        //.matchArpOp(((ARP) ethPkt.getPayload()).getOpCode())
					.matchArpSha(ethPkt.getDestinationMAC())
                                        .matchArpTha(ethPkt.getSourceMAC());*/
				// WAIT FOR THE INSTALL
				/*
				setUpConnectivity(context, srcId, dstId, outSelector.build(), pseudoKey, inTime, true);
				setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey, inTime, true);
				*/
				// DO NOT WAIT
				
				setUpConnectivity(context, srcId, dstId, outSelector.build(), pseudoKey, inTime, false);
                                setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey, inTime, false);
				
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
					// OPTIMIZED
					
					outSelector = outSelector.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
					inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()));
					
					// NOT OPTIMIZED
					/*
					outSelector = outSelector.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()))
								 .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort())); // NOT OPTIMIZED
					inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
								 .matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
					*/
					//inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()));
									       //.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort())); // NOT OPTIMIZED
				}
				if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
					UDP udpPacket = (UDP) ipv4Packet.getPayload();
					protocol = "UDP";
					//sourcePort = Integer.toString(udpPacket.getSourcePort());
					//destPort = Integer.toString(udpPacket.getDestinationPort());
					//outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//outSelector = outSelector.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					//inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()));
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
				log.info("\n Request Intent");
				addIntent = netViewsPolicyPlaceholder(sourceIP, sourceMAC, ingressPort, destIP, destMAC, egressPort, protocol, sourcePort, destPort, context);
				log.info("\nReceived "+addIntent);
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

			// OPTIMIZED
			
			outPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + destPort);//sourcePort + destPort + code + type);
			inPseudoKey += (destIP + destMAC + sourceIP + sourceMAC + protocol + destPort + "RETURN"); //+ sourcePort + code + type);
			
			return_key_cache.put(inPseudoKey, true);

			// NOT OPTIMIZED
			/*
			outPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + destPort + sourcePort);//sourcePort + destPort + code + type);
                        inPseudoKey += (destIP + destMAC + sourceIP + sourceMAC + protocol + sourcePort + destPort); //+ sourcePort + code + type);
			*/
			Key key;
			key = Key.of(outPseudoKey, appId);
			log.info("\nChecking Key: " + key.toString() + "\n" + Key.of(inPseudoKey, appId) + "\n");

			if (addIntent) {
				// WAIT UNTIL INSTALL
				/*
				setUpConnectivity(context, srcId, dstId, outSelector.build(), outPseudoKey, inTime, true);
				setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey, inTime, true);
				*/
				// DO NOT WAIT UNTIL INSTALL
				
				setUpConnectivity(context, srcId, dstId, outSelector.build(), outPseudoKey, inTime, false);
                                setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey, inTime, false);
				
				forwardPacketToDst(context, dst);
			} else {
				// This is a fix for dropped return packet ins.
                        	String lostPseudoKey = "";
                        	lostPseudoKey += (srcId.toString() + dstId.toString());
                        	lostPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + sourcePort + "RETURN");
				
				if (return_key_cache.containsKey(lostPseudoKey)) {
					log.info("\n LOST PACKET \n");
					forwardPacketToDst(context, dst);
					return;
				} else {
					// The intent should not be installed.
					log.info("\nFalse Policy, dropping: " + outPseudoKey + "\n" + inPseudoKey);
					return;
				}
			}
		}
	}

	private boolean netViewsPolicyPlaceholder(
			String sourceIP, String sourceMAC, String ingressPort,
			String destIP, String destMAC, String egressPort,
			String protocol, String sourcePort, String destPort, PacketContext context) throws IOException, PMException {

		//long inTime = System.nanoTime();	
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

		log.info(logging);*/
		boolean permission = false;

		log.info("\n Request Permission");
		String subjectName = identityMap.getHostIdentity(sourceIP).getName();
		String objectName = identityMap.getHostIdentity(destIP).getName();
		//String subjectName = identityMap.getHostIdentity(sourceIP);
		//String objectName = identityMap.getHostIdentity(destIP);
		
		String actionName = protocol.toLowerCase()+"/"+destPort;

		log.info("\n @@@@@ Got 	Name: " + subjectName+" "+objectName+" "+actionName + " From: " + context.inPacket().receivedFrom());

		permission = policyEngine.getPermission(subjectName, objectName, actionName);

		//log.info("\n@@@@@ " + permission + " @@@@@@");
		//long outTime = System.nanoTime();
                //log.info("\n\nPolicy Time:" + String.valueOf(outTime - inTime) + "\n\n");
		
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
	private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId, TrafficSelector selector, String pseudoKey, Long inTime, boolean waitInstalled) {
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
			//log.info("\nNon-null intent state: " + intentService.getIntentState(key));
			if (WITHDRAWN_STATES.contains(intentService.getIntentState(key))) {
				HostToHostIntent hostIntent = HostToHostIntent.builder()
						.appId(appId)
						.key(key)
						.one(srcId)
						.two(dstId)
						.selector(selector)
						.treatment(treatment)
						.priority(PRIORITY)
						.build();

				if (waitInstalled) {
					submitIntent(hostIntent, intentService);
				} else {
					intentService.submit(hostIntent);
				}
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
					.priority(PRIORITY)
					.build();

			if (waitInstalled) {
                                submitIntent(hostIntent, intentService);
                        } else {
                                intentService.submit(hostIntent);
                        }
			//intentService.submit(hostIntent);
			//submitIntent(hostIntent, intentService);
			//applyIntentSync(hostIntent, Direction.ADD);
		}

		PRIORITY -= 1;

		//ComputeTime computeTime = new ComputeTime(inTime, key);
		//Thread t = new Thread(computeTime);
		//t.start();
	}

	private boolean submitIntent(HostToHostIntent intent, 
				     IntentService intentService) {
		Key key = intent.key();
		intentService.submit(intent);
		IntentState state = intentService.getIntentState(key);
		log.info("Intent State: {}\n", state);

		while (state != INSTALLED && state != INSTALLING) {

			try {
                                Thread.sleep(2);
                        } catch (Exception e) {
                                System.out.println(e);
                        }
			
			state = intentService.getIntentState(key);
		
			try {
                                Thread.sleep(2);
                        } catch (Exception e) {
                                System.out.println(e);
                        }

			log.info("Intent State: {}\n", state);

		}
		//log.info("\n\nIntent State: {}\n\n", listener.getState());
		return false;
	}
	
	private final class InternalIntentListener implements IntentListener {
		private final HostToHostIntent intent;
		private final IntentService intentService;
		private final CountDownLatch latch;
		private IntentState state;

		private InternalIntentListener(HostToHostIntent intent, 
					       IntentService intentService,
					       CountDownLatch latch) {
			this.intent = intent;
			this.intentService = intentService;
			this.latch = latch;
		}

		@Override
		public void event(IntentEvent event) {
			if (event.subject().equals(intent)) {
				state = intentService.getIntentState(intent.key());
				if (state == INSTALLED || state == INSTALL_REQ) {
					latch.countDown();
					intentService.removeListener(this);
				}
			}
		}

		public IntentState getState() {
			return state;
		}
	}

	public void waitInstall(Key key) {
                while(intentService.getIntentState(key) != IntentState.INSTALLED) {
 	        	//do nothing
                        try {
                                Thread.sleep(1);
                        } catch (Exception e) {
                                System.out.println(e);
                        }
       
                }
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
				try {
                                        Thread.sleep(1);
                                } catch (Exception e) {
                                        System.out.println(e);
                                }
			}
			long outTime = System.nanoTime();
			log.info("\nIntent " + (intentService.getIntent(key).id()).toString() + ": " + key.toString() + " Installed Time: " + String.valueOf(outTime - inTime) + "\n\n");
		}
	}
}
