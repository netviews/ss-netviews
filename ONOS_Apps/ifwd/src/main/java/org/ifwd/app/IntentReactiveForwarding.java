package org.ifwd.app;

/*import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
*/
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
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

import java.util.EnumSet;

import static org.slf4j.LoggerFactory.getLogger;

import static org.onosproject.net.intent.IntentState.*;

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


    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private ApplicationId appId;

    private static final int DROP_RULE_TIMEOUT = 300;

    private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
                                                                            IntentState.WITHDRAWING,
                                                                            IntentState.WITHDRAW_REQ);

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.ifwd");
	log.info("ifwd_1");
	packetService.addProcessor(processor, PacketProcessor.director(2));
	log.info("ifwd_2");
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        log.info("ifwd_3");
	selector.matchEthType(Ethernet.TYPE_IPV4);
        log.info("ifwd_4");
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
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            //log.info("$$$$$$$$$$$$$$$$ We started handling");
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

	    log.info(ethPkt.toString());

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(dstId);
            if (dst == null) {
                flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            setUpConnectivity(context, srcId, dstId);
            forwardPacketToDst(context, dst);

	    //long outTime = System.nanoTime();
            //log.info("\n\nSend Time (us): " + String.valueOf(outTime - inTime) + "\n\n");
        }
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
    private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId) {
        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        Key key;
        if (srcId.toString().compareTo(dstId.toString()) < 0) {
            key = Key.of(srcId.toString() + dstId.toString(), appId);
        } else {
            key = Key.of(dstId.toString() + srcId.toString(), appId);
        }

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
		// DO NOT VERIFY
                intentService.submit(hostIntent);
		// VERIFY
		//submitIntent(hostIntent, intentService);
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

	    // DO NOT VERIFY
            intentService.submit(hostIntent);
	    // VERIFY
	    //submitIntent(hostIntent, intentService);
        }

    }

    private boolean submitIntent(HostToHostIntent intent,
                                     IntentService intentService) {
                /*CountDownLatch latch = new CountDownLatch(1);
                InternalIntentListener listener =
                        new InternalIntentListener(intent, intentService, latch);
                intentService.addListener(listener);*/
                Key key = intent.key();
	        //long submitTime = System.nanoTime();
                //Key key = intent.key();
                intentService.submit(intent);
                //log.info("Submitted NetViews App intent and waiting: {}", intent);
                //Key key = intent.key();
                IntentState state = intentService.getIntentState(key);
                //log.info("Intent State: {}\n", state);
                //log.info(intentService.getIntentData().iterator().next().toString());

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
                //log.info(intentService.getIntentData().iterator().next().toString());
		//long installTime = System.nanoTime();

                //log.info("\nSubmit Time: " + String.valueOf(submitTime) + "\nCompile Time: " + String.valueOf(compileTime) + "\nInstall Time: " + String.valueOf(installTime));
                //log.info("\nCompile Time: " + String.valueOf(installTime - submitTime));// + "\nInstall Time: " + String.valueOf(installTime));
                /*try {
                        //latch.await();
                        //if (
                        if (latch.await(TIMEOUT, TimeUnit.MILLISECONDS) &&
                                listener.getState() == INSTALLED) {
                                log.info("\n\nIntent State: {}\n\n", listener.getState());
                                return true;
                        }
                } catch (Exception e) {
                        log.info("\n\nEXCEPTION: {}\n\n", e.toString());
                }*/
    	return false;
    }
}
