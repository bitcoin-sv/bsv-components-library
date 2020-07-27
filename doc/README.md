# JCL: Java Component Library for Bitcoin

# Introduction

*JCL* is a light Java Library that provides "Connection" and "Streaming" functionalities to the Blockchain. 
An application can use *JCL* to make a Connection to the blockchain and receive notifications (streaming) of any 
Event that occurs in the network and it might be interesting. All the *Bitcoin Protocol* details are hidden and dealt 
with by *JCL* transparentely


# High-Level View

## Architecture:

*JCL* runs as a separate Service in a different Thread than your main application. You start it (after setting some 
basic configuration, but default configuration is also provided), and it will connect automatically to the Blockchain
P2P Network. The number of Peers *JCL* connects to is configurable within a range, and the service will make sure that 
the connection remains stable, looking for new Peers if some connections are broken), or just standing by if we 
already have enough.


The 3 main features provided by *JCL* are:

* Connection to the Blockchain Network: This includes not only the connection, but also all the *Bitcoin Protocol* 
  internals that are needed to main those connections alive (*Handshake* protocol, *timeout* (Ping/Pong), *Node-Discovery* 
  algorithm, Blacklist, etc)
  
* Streaming: *JCYL* can notify about any Event that might happen during your connection, so your application can 
  react to it. The application only needs to *subscribe* to the events it's interested int, and it wil get notified as soon 
  as they occur. 

* Requests: *JCYL* also provides a way for your application to make *Requests* to the Blockchain Network. Since
  *JCTYL* is a low-level library, these Requests are always related to different aspects of the protocol, like sendign a message, 
  connecting/disconnecting from a Peer, rquesting a TX or a Block, etc.
  
![high level architecture](jclBasic.png)

Other non-functional aspects of the Library:

* Consumption of resources limited: *JCL* has been tested to connect to hundreds or thosands of Peers using modest Hardware.

* Extensible: The *JCYL* Service is just an *aggregation* of different *Components/Handlers*, each one of them takes care of a 
  different aspect of the Protocol. These architecture is flexible, so new Handlers can be developed and added at runtime.
  For example, *JCYL* provides a set of Default Handlers, which implements the *Bitcoin protocol*. but also provides other
  higher-level Handlers that implement other functionalities, like download of full blocks on-demand. New 
  Handlers that implements other capabilities can be added without modifying the core,



# How to import *JCL* into your project

If you are using *gradle*, you need to configure the access to the *nChain* maven repository, and add the dependency 
in your project in your *build.gradle*

````
...
repositories {
  // nchan repository:
  maven { url "https://repo.spring.io/snapshot" }
  ...  
}
...

dependencies {
...
    implementation 'com.nchain.jcl:0.0.1'
...
}
````

If you are using *maven*, you need to configure the access to the *nChain* maven repository, and add the dependency 
in your project in your *pom.xml*

````
asda
````

*JCLY* uses *logbnack* as loging system. In order to log the outut of the library, you need to add a *logback.xml* file 
into your classpath:

````
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level  - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.nchain.jcl" additivity="false">
        <level value="INFO" />
        <appender-ref ref="STDOUT" />
    </logger>
        
</configuration>
````

## Examples:

### Simple connection:

The following example is just ane xample of how the connection is performed. We get a reference to the "P2P"
service from the *P2PBuilder* (specifying an identifier that is mostly used for logging), and then we start it. 
The service wil run in a different Thread, so all the Streaming services will be working in the background while 
our app performs other actions. In the example below though, the service will automaticallu stop after starting.

````
P2P p2p = new P2PBuilder("testing").build();
p2p.start(); // asynchronous...
// Do something useful here...
p2p.stop();
````

The output of the previous example:

````
2020-07-23 12:19:24.844 INFO   - testing :: P2P-Handler :: Starting...
2020-07-23 12:19:24.850 INFO   - testing :: P2P-Handler :: Configuration:
2020-07-23 12:19:24.854 INFO   - testing :: P2P-Handler ::  - BSV [main Net] configuration
2020-07-23 12:19:24.857 INFO   - testing :: P2P-Handler ::  - working dir: /var/folders/5z/nz8z4wp14fj6fmvmfrxp1cc40000gn/T/jcl
2020-07-23 12:19:24.885 INFO   - testing :: P2P-Handler ::  - peers range: [10 - 15]
2020-07-23 12:19:24.965 INFO   - testing :: P2P-Handler :: Stop.

````

We can see the Configuration used: *ProtocolBSVMain* by default, so the service will connect to the BSV Man network if
nothing else is specified. We also see the *working directory*, which is a temporary folder automatically picked up by the 
service to store some internal information. The Service has started in *Client Mode*, so it can connect to other Peers but it 
does Not allow incoming connections. The number of Peers connected will always remain in the range [10 -15].

If you want to allow incoming connections from Remote Peer, you just need to start in server mode:

````
...
p2p.startServer();
...
````

In this case, the Service will accept connections in the local IP address, using the port specified in the 
Configuration (in this specific example, the port number for BSV [main Net] is 8333)


### Connection and basic streaming

In the following example, we are streaming some events: The *P2P* Services contains a *EVENTS* reference, which itself 
contains different references to different types of Events (Events related to Peers, to Messages, etc). 
We select the type of Event we are interested in, and then we add a callback by using the method *forEach* (in a similar 
way as the Java Streams work).

````
P2P p2p = new P2PBuilder("testing").build();

p2p.EVENTS.PEERS.CONNECTED.forEach(System.out::println);
p2p.EVENTS.PEERS.DISCONNECTED.forEach(System.out::println);
p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
p2p.EVENTS.MSGS.ALL.forEach(System.out::println);

p2p.start();
// We will be notified for 5 seconds...
Thread.sleep(5_000);
p2p.stop();
````

The parameter to the "forEach" methods is just an Event Object, which contains different information depending on the 
event itself. In this example we are just printing its content to the console, so the output for the previous code is 
as follows:

````
INFO   - testing :: P2P-Handler :: Starting...
INFO   - testing :: P2P-Handler :: Configuration:
INFO   - testing :: P2P-Handler ::  - com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig@614ca7df configuration
INFO   - testing :: P2P-Handler ::  - working dir: /var/folders/5z/nz8z4wp14fj6fmvmfrxp1cc40000gn/T/jcl
Event[Peer Connected]: 206.189.104.98/206.189.104.98:8333
Event[Peer Connected]: 174.138.5.253/174.138.5.253:8333
Event[Peer Connected]: 68.183.42.63/68.183.42.63:8333
Event[Peer Connected]: 104.248.30.60/104.248.30.60:8333
Event[Peer Connected]: 167.99.92.186/167.99.92.186:8333
Event[Peer Connected]: 104.248.245.82/104.248.245.82:8333
Event[Msg Received]: VERSION : from 104.248.30.60/104.248.30.60:8333
Event[Msg Received]: VERACK : from 167.99.92.186/167.99.92.186:8333
Event[Msg Received]: VERSION : from 167.99.92.186/167.99.92.186:8333
Event[Msg Received]: VERACK : from 206.189.104.98/206.189.104.98:8333
Event[Msg Received]: VERSION : from 206.189.104.98/206.189.104.98:8333
Event[Msg Received]: VERACK : from 104.248.30.60/104.248.30.60:8333
Event[Peer Handshaked]: 174.138.5.253/174.138.5.253:8333 : /Bitcoin SV:1.0.4/
Event[Peer Handshaked]: 68.183.42.63/68.183.42.63:8333 : /Bitcoin SV:1.0.4/
Event[Peer Handshaked]: 206.189.104.98/206.189.104.98:8333 : /Bitcoin SV:1.0.4/
Event[Peer Handshaked]: 104.248.30.60/104.248.30.60:8333 : /Bitcoin SV:1.0.4/
Event[Peer Disconnected]: 206.189.81.233/206.189.81.233:8333: DISCONNECTED_BY_LOCAL
Event[Peer Disconnected]: 167.99.92.186/167.99.92.186:8333: DISCONNECTED_BY_LOCAL
Event[Peer Disconnected]: 159.65.152.200/159.65.152.200:8333: DISCONNECTED_BY_LOCAL
````

### Basic Customization and fine-tuning

The Configuration we use in the *P2P* Service can be changed in several ways, in this chapter we'll explain how to do a
very basic configuration changes, modifying some values:

* The network we connect to
* The range of Peers we want to be always connected to
* The *port* number we'll be listening at (if started in Server Mode)
* other network parameters...


In the next example, we are connecting to a different network and changing the range of peers [10,20].

````
ProtocolConfig config = new ProtocolBSVStnConfig(); // Different Network!!!
P2P p2p = new P2PBuilder("testing")
              .config(config)
              .minPeers(10)
              .maxPeers(15)
              .build();
...
````
In the previous exampe, we'v e used a diffrent Class (*ProtocolBSVStnConfig*), which is a built-in class that already 
contains the configuration values to connect to the *Stress Net* in BSV. But you don´´ have to use a new class, you can 
still use the original class for *BSV [main Net]* and change the parameters on the fly:

````
...
ProtocolConfig config = new ProtocolBSVMainConfig().toBuilder()
                    .magicPackage(0xe8f3e1e3L)
                    .port(8333)
                    .protocolVersion(ProtocolVersion.CURRENT.getBitcoinProtocolVersion())
                    .services(ProtocolServices.NODE_BLOOM.getProtocolServices())
                    .build();
...

````

### Events Handling

Each Event is streamed through a callback that we define in the *forEach* method. In previous examples we only printed 
its content to the console, but we can implement any kind of logic. In the following example, we print an specific message 
if any peer we connect to has an specific protocol version number:

````
...
p2p.EVENTS.PEERS.HANDSHAKED.forEach(this::onPeerHandshaked);
...
void onPeerHandshaked(PeerHandshakedEvent event) {
 if (event.getVersionMsg().getVersion() < 70013)
     System.out.println("Version too low!!!");
}
...
````

### Status Streaming

Just by listening to the right events, the application can keep track of the number of Peers connected, messages 
exchanges, etc. Most of the time, this is useful information that can be used to trigger other functions or flows 
in our system. 

Other times, we just need to monitor the system, for example to print out the information about the number of Msgs 
exchanged per second, or the number of new Connections vs number of connections lost per hour, etc. Even though these 
"status" information can also be obtained by listening to the right Events and putting all the info together, *JCYL* 
provides specific mechanisms to notify about the system *State*. These *State* Notifications are like any other 
Event, with the difference that they include some aggregate information that might help make a whole picture on the 
system. These *State* Events are triggered on a frequency basis, which must be specified before starting the service.

Here is an example:

````
P2P p2p = new P2PBuilder("testing")
              .publishStates(Duration.ofSeconds(5)) // We get notified every 5 seconds
              .build();
p2p.EVENTS.STATE.ALL.forEach(System.out::println);  // we print the State
p2p.start();
Thread.sleep(10_000); // we get notified for 10 seconds
p2p.stop();

````

And the output:


````
Event[State]: Network Handler State: Connections: 12 active, 0 pending to Open, 0 pending to Close: Running in Client Mode: connecting
Event[State]: Message Handler State: 63 incoming Msgs, 48 outcoming Msgs
Event[State]: Handshake Handler State: 12 current Handshakes, 0 failed.  More Connections requested
Event[State]: PingPong-Handler State: 0 Pings in progress
Event[State]: Discovery State:  Pool size: 0 [ 0 handshaked, 1000 added, 0 removed, 1045 rejected ] : 12 GET_ADDR sent, 4 ADDR received
Event[State]: Blacklist Status: [ 0 Addresses blacklisted ] count: {}
````

Since *JCL* is internally made of a composition of different *handlers*, each one taking care of an specific part of the 
*Bitcoin Protocol*, we have then different status, each one related to one specific Handler.

Like with a regular Event, we can define the "forEach" callback in a separate method, and inspect the different State
Event Objects to get all the information we nee dfrom them.

> All the *State* Events, along with the rest of Events, are described in the **Reference** Section 




# Detailed View and advance-configuration:

*JCL* follows a modular architecture, composed of multiple components called *Handlers*, each 
one of them implementing a different functionality. Some of these *Handlers* are designed as *Default 
handlers* and are mandatory, since critical parts of the *Bitcoin protocol* would be missing 
without them.

Others are optional, and provide other extra-functionalities. And more *Handlers* can be developed and 
*added* the the main *JCL* Service in runtime.


![detail level architecture](jclDetail.png)


The Default Handlers are the following:

 * *Network/Connection Handler*: This handler implements the physical connection between *JCL* and the remote Peers, and 
 also manages all the communications with them (in raw format, byte array structure)
 
 * *Message Handler*: This Handler implements the Serialization/Deserialization process: It converts the raw data 
 exchanged over the network into a *Message* format which is better handled by the Application.
 
 * *Handshake Handler*: This handler performs the Handshake Protocol: Every time *JCL* connects to a remote Peer, the 
 hnadshake needs to be performed beween *JCL* and that Peer, before any exchange of information can be done. The *Handshake* 
 protocol is defined as an exchange of a series of messages between 2 parties until they either agree they can "talk" to each other, or
 are discarded.
 
 * *PingPong Handler*: This handler checks that a Remote Peer is still "alive", by sending them *ping* messages and waiting for
 the reply.
 
 * *Discovery Handler*: This handler is responsible for keeping an "alive" pool of Peer Addresses, so we can use it to connect 
 to more Peers if we need more connections. The way new Addresses are discovered is described as the "Node Discovery Algorithm", and 
 involves a rquest/response mechanism between different Peers, asking for new addresses and replying to those requests.
 
 * *Blacklist Handler*: This handler detects any situation when a Peer might be blacklisted, and it does so. It0's also responsible
 for whitelisting those Peer which have been blacklisted but which "fault" has expired already.


# Advance Configuration:

> PENDING...


# Reference

# Events
The following is a list of the most relevant Events that *JCYL* can stream. Each *Event* is represented by a JAva 
Object and it contains different types of information, depending on the event itself.

## Events related to Peers

### PeerConnectedEvent
``com.nchain.jcl.network.events.PeerConnectedEvent``

(*EVENTS.PEERS.CONNECTED*)

An Event triggered when a Peer is Connected. This is a physical connection (Socket Connection),
so the real communication with this Peer has not even started yet. Most probably you will be interested in the
*PeerHandshakedEvent*, which is triggered when a Peer is connected and the handshake is done, so real
communication can be performed.

### PeerDisconnectedEvent
``com.nchain.jcl.network.events.PeerDisconnectedEvent``

(*EVENTS.PEERS.DISCONNECTED*)

An Event triggered when a Peer is disconnected.

### PeerHandshakedEvent
``com.nchain.jcl.protocol.events.PeerHandshakedEvent``

(*EVENTS.PEERS.HANDSHAKED*)

An Event triggered when A Peer has been handshaked and it's ready to communicate with.

### PeerHandshakeRejectedEvent
``com.nchain.jcl.protocol.events.PeerHandshakeRejectedEvent``

(*EVENTS.PEERS.HANDSHAKED_REJECTED*)

An Event triggered when the Handshake with a Remote Peer has been rejected.

### PeerHandshakedDisconnectedEvent
``com.nchain.jcl.protocol.events.PeerHandshakedDisconnectedEvent``

(*EVENTS.PEERS.HANDSHAKED_DISCONNECTED*)

An Event triggered when a Peer that was currently handshaked, disconnects. This is a *convenience* event, since the
same information can be achieved by listening to the events *PeerHandshakedEvent* and *PeerDisconnectedEvent*

### MinHandshakedPeersReachedEvent
``com.nchain.jcl.protocol.events.MinHandshakedPeersReachedEvent``

(*EVENTS.PEERS.HANDSHAKED_MIN_REACHED*)

An Event triggered when the minimun number of Peers Handshaked has been reached, as specified in the P2P Configuration.
For example, if the peers range is specified to [10 - 12], this event will be notified when the service manages to connect 
to 10 Peers. This Event wil NOT be notified again UNTIL the number of Peers falls below the lower range (10) nad later on we manage to connect to 12 Peers again 

### MinHandshakedPeersLostEvent
``com.nchain.jcl.protocol.events.MinHandshakedPeersLostEvent``

(*EVENTS.PEERS.HANDSHAKED_MIN_Lost*)

An Event triggered when the Number of Peers Handshakes has dropped below the threshold specified in the P2P Configuration
For example, if the peers range is specified to [10 - 12], this event will be notified when the service is already 
connected to 10 or more Peers, and the number of connections drops below 10. 

### PingPongFailedEvent
``com.nchain.jcl.protocol.events.PingPongFailedEvent``

(*EVENTS.PEERS.PINGPONG_FAILED*)

An Event triggered when a Peer has failed to perform the Ping/Pong Protocol, which means that a PING message has been sent
 to this Peer but it has not reply with a PONG message within the time frame specified in the Configuration.
 
> Note: This event only notified the fact. If you want to check if some action has been taken on this Peer due to this, you 
should listen to the *PeerDisconnectedEvent* or "PeerBlacklistedEvent" Events. 


### PeersBlacklistedEvent
``com.nchain.jcl.protocol.events.PeersBlacklistedEvent``

(*EVENTS.PEERS.BLACKLISTED*)

An Event triggered when a set of Nodes is blacklisted. (Ahole IP address is blacklisted, no mater the port number)
It also provides information about the REASON why it's been blacklisted, which also contains the expirationTime (the date after which this Peer is whitelisted and can be used again).

### PeersWhitelistedEvent
``com.nchain.jcl.protocol.events.PeersWhitelistedEvent``

(*EVENTS.PEERS.WHITELISTED*)

An event triggered when a set of IP Addresses has been whitelisted (back to business again)

## Events related to Bitcoin Messages

### MsgReceivedEvent
``com.nchain.jcl.protocol.events.MsgReceivedEvent``

(*EVENTS.MSGS.ALL*)

An Event triggered when a Message is received from a Remote Peer. If you are interested only in a subset of all possible 
messages, you can either implement your own logic into the callback to filter them out, or you can also use the different 
and more specific Events provided:

 * (*EVENTS.MSGS.ADDR*)
 * (*EVENTS.MSGS.BLOCK*)
 * (*EVENTS.MSGS.FEE*)
 * (*EVENTS.MSGS.GET_ADDR*)
 * (*EVENTS.MSGS.GET_DATA*)
 * (*EVENTS.MSGS.INV*)
 * (*EVENTS.MSGS.NOT_FOUND*)
 * (*EVENTS.MSGS.PING*)
 * (*EVENTS.MSGS.REJECT*)
 * (*EVENTS.MSGS.TX*)
 * (*EVENTS.MSGS.VERSION*)
 * (*EVENTS.MSGS.VERSION_ACK*)

> MORE MESSAGES COMING UP...

### MsgSentEvent
``com.nchain.jcl.protocol.events.MsgSentEvent``

(*EVENTS.MSGS.ALL_SENT*)

An Event triggered when a Message is sent to a Remote Peer. If you are interested only in a subset of all possible 
messages, you can either implement your own logic into the callback to filter them out, or you can also use the different 
and more specific Events provided:

 * (*EVENTS.MSGS.ADDR_SENT*)
 * (*EVENTS.MSGS.BLOCK_SENT*)
 * (*EVENTS.MSGS.FEE_SENT*)
 * (*EVENTS.MSGS.GET_ADDR_SENT*)
 * (*EVENTS.MSGS.GET_DATA_SENT*)
 * (*EVENTS.MSGS.INV_SENT*)
 * (*EVENTS.MSGS.NOT_FOUND_SENT*)
 * (*EVENTS.MSGS.PING_SENT*)
 * (*EVENTS.MSGS.REJECT_SENT*)
 * (*EVENTS.MSGS.TX_SENT*)
 * (*EVENTS.MSGS.VERSION_SENT*)
 * (*EVENTS.MSGS.VERSION_ACK_SENT*)
 
 > MORE MESSAGES COMING UP...
 
## Events related to *States*:
These Events, unlike the rest of Events in *JCL*, are triggered always on a frequency basis, which has to be specified *before* 
running the service.

### NetworkHandlerState
``com.nchain.jcl.network.events.HandlerStateEvent``

(*EVENTS.STATE.NETWORK*)

This Event stores the current State of the Connection/Network Handler. The Network Handler implements the
physical and low-level connection to a remote Peers, and handles all the incoming/outcoming data between
the 2 parties.

You must cast the Handler State:

````
...
public void onState(HandlerStateEvent event) {
    NetworkHandlerState state = (NetworkHandlerState) event.getState();
    ...
}
...
````
 
### MessageHandlerState
``com.nchain.jcl.network.events.HandlerStateEvent``

(*EVENTS.STATE.MESSAGES*)

This event stores the state of the Handshake Handler at a point in time.
The Message Handler takes care of the Serialization/Deserialization of the information coming
from/to the Blockchain P2P Network, converting Bitcoin Messages into bytes (raw data) and the
other way around.

This events keeps track of the number of bitcoin messages sent to and received from the network.

You must cast the Handler State:

````
...
public void onState(HandlerStateEvent event) {
    MessageHandlerState state = (MessageHandlerState) event.getState();
    ...
}
...
````

### HandshakeHandlerState
``com.nchain.jcl.network.events.HandlerStateEvent``

(*EVENTS.STATE.HANDSHAKE*)

This event stores the state of the Handshake Handler at a point in time.
the Handshake Handler takes care of implementing the Handshake Protocol, which takes place
right after connecting to a new Peer. It consists of a exchange of messages between the 2
parties to verify that they are protocol-compatible.
 
This event stores the number of Peers currently handshaked or failed, and also some flags
that indicate if the Service has been requested to look for more Peers or to stop new connections
instead (these request to resume/stop connections are always triggered when the nuymber of
Peer handshakes go above or below some thresholds).

You must cast the Handler State:

````
...
public void onState(HandlerStateEvent event) {
    HandshakeHandlerState state = (HandshakeHandlerState) event.getState();
    ...
}
...
````


### PingPongHandlerState
``com.nchain.jcl.network.events.HandlerStateEvent``

(*EVENTS.STATE.PINGPONG*)

 This event stores the state of the PingPong Handler at a point in time.
 The PingPong Handler takes care of checking that the Remote Peers we are connected to are still
 "alive". On a frequency basis, it sends a PING message to them, expecting a PONG message back. If
 the response does not come or comes out of time, the Peer has then broken the timeout and will most
 probably be blacklisted.
 
 You must cast the Handler State:
 
 ````
 ...
 public void onState(HandlerStateEvent event) {
     PingPongHandlerState state = (PingPongHandlerState) event.getState();
     ...
 }
 ...
 ````
 

### DiscoveryHandlerState
``com.nchain.jcl.network.events.HandlerStateEvent``

(*EVENTS.STATE.DISCOVERY*)

 This event stores the state of the Discovery Handler at a point in time.
 The Discovery Handler takes care of feeding the Service with enough addresses of Remote Peers, so
 we have a "pool" of addresses we can use when we need to connect to more Peers.
 
 You must cast the Handler State:
 
  ````
  ...
  public void onState(HandlerStateEvent event) {
      DiscoveryHandlerState state = (DiscoveryHandlerState) event.getState();
      ...
  }
  ...
  ````
  
 
### BlacklistHandlerState
``com.nchain.jcl.network.events.HandlerStateEvent``

(*EVENTS.STATE.BLACKLIST*)

 This event stores the state of the Blacklist Handler at a point in time.
 The Blacklist Handler takes care of Blacklisting (and whitelisting) Peers when some conditions are
 met: they failed during the handshake, or broken the timeout specified by the PingPong Handler, etc.
 
 You must cast the Handler State:
 
   ````
   ...
   public void onState(HandlerStateEvent event) {
       BlacklistHandlerState state = (BlacklistHandlerState) event.getState();
       ...
   }
   ...
   ````