# libdetect
[![](https://jitpack.io/v/RightMesh/librxbus.svg)](https://jitpack.io/#compscidr/libdetect)

Performs detection of open TCP ports on the local networks that a particular device is connected to.

You can use this library
in your project with gradle using jitpack:

```java
repositories {
    maven { url 'https://jitpack.io' }
}
```

```java
dependencies {
   implementation 'com.github.compscidr:libdetect:v1.0'
}
```

# Features
* Can monitor multiple TCP ports with subsequent calls to start(port) function.
* Supports callbacks for the following two events:
  * PeerReachable events generated when a peer becomes reachable on the local network with the specified TCP port.
  * PeerUnreachable events generated when a previously reachable peer is no longer reachable with the specified TCP port.

## Register for TCP peer discovery:
For example let's suppose we wish to have events generated for any port 80 tcp servers running on all local networks:
```
LibDetect test = new LibDetect();

test.start(80, new ActionListener() {
    @Override
    public void onPeerReachable(PeerReachable peer) {
        System.out.println("PEER REACHABLE on " + peer.address.getHostAddress() + " port: " + 80);
    }

    @Override
    public void onPeerUnreachable(PeerUnreachable peer) {
        System.out.println("PEER UNREACHABLE on " + peer.address.getHostAddress() + " port: " + 80);
    }
}, false);
```

## Cleanup (stop listening for peer discovery events)
```
test.stop(80);
```
