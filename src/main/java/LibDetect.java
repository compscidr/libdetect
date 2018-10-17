import com.google.common.collect.Sets;

import java.net.*;
import java.util.*;

/*
 * Detects other devices on the same subnet also using LibDetect on the same port.
 * Provides events for devices being reachable and unreachable
 * Jason Ernst, 2018, RightMesh
 *
 * This lib works by brute forcing a TCP connection on the specified port for all IPs on all subnet ranges for which
 * a device has a local ipv4 address on. For instance if the device has local ip 192.168.1.45, it will search every ip
 * from 192.168.1.1 to 192.168.1.254 excluding itself (if skipOurself is set). If a connection is made it calls the
 * PeerReachaable callback. If this TCP connection subsequently breaks, it will call the PeerUnreachable callback.
 * In order to detect the breakage a 1 byte null byte is sent periodically after the connection is made - java does not
 * throw an exception on a socket when the remote side of the socket closes for instance until a read or write fails.
 *
 * Note: it takes some time to cleanup all of the threads so on the stop() call. This increases with the number of
 * ports being monitored, and the number of interfaces with local ipv4 address present on the device.
 *
 */
public class LibDetect {
    private Map<Integer, Detect> discoveryMap;

    LibDetect() {
        discoveryMap = new HashMap<>();
    }

    /**
     * Will attempt to issue a TCP connect on the given port, and if the connection is made, it will generate an event
     * which contains the TCP socket and the IP addres of each peer which responds.
     *
     * @param port the TCP port of the service to query
     * @param actionListener the listener of the PeerReachable and PeerUnreachable events
     */
    public void start(int port, ActionListener actionListener, boolean skipOurself) {
        Set<TCPMonitor> tcpMonitors = Sets.newConcurrentHashSet();
        Thread t = new Thread(() -> {
            Set<InetAddress> ipAddresses =  getInetAddresses();

            for(InetAddress ipAddress : ipAddresses) {
                Set<InetAddress> subnetIpAddresses = getSubnetIPAddresses(ipAddress, skipOurself);
                if(subnetIpAddresses.size() > 0) {
                    System.out.println("Testing from IP: " + ipAddress.getHostAddress() + " for port: " + port);
                    for(InetAddress subnetIpAddress : subnetIpAddresses) {
                        TCPMonitor tcpMonitor = new TCPMonitor(subnetIpAddress, port, actionListener);
                        tcpMonitors.add(tcpMonitor);
                        new Thread(tcpMonitor).start();
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                            //add this short sleep so that if things start to shut down while we are still adding more
                            //monitors an interrupted exception will throw to here and we can stop adding new monitors
                            //causing a concurrent mod exception on the tcpMonitors set.
                            return;
                        }
                    }
                }
            }
        });

        Detect detect = new Detect(t, tcpMonitors);
        discoveryMap.putIfAbsent(port, detect);
        t.start();
    }

    /**
     * Will return a set of subnet IP addresses given the ip addresse provided. Will exclude the given ip address from
     * the set.
     * @param ip the ip to determine the other subnet ips for
     * @return the set of ip addresses in the same subnet.
     */
    private Set<InetAddress> getSubnetIPAddresses(InetAddress ip, boolean skipOurself) {
        Set<InetAddress> ipAddresses = new HashSet<>();

        //for now only support ipv6
        if(ip instanceof Inet4Address) {
            String root = ip.getCanonicalHostName();

            //make sure we have an actual ip address and not something like localhost
            if(root.matches( "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")) {
                root = root.substring(0, root.lastIndexOf('.') + 1);
                for(int i = 1; i < 254; i++ ) {
                    String ipstring = root + i;
                    if(ipstring.equals(ip.getCanonicalHostName()) && skipOurself) {
                        continue;
                    }
                    try {
                        ipAddresses.add(Inet4Address.getByName(ipstring));
                    } catch(UnknownHostException ex) {
                        //ignore if we can't resolve - this should never happen anyway as long as we are still
                        //connected to the same subnet.
                    }
                }
            }
        }

        return ipAddresses;
    }

    /**
     * Will return all of the local ip addresses for all of the interfaces on this device (eth, wifi, local, etc)
     * @return a set of inetaddresses (ipv4 and ipv6)
     */
    private Set<InetAddress> getInetAddresses() {
        Set<InetAddress> ipAddresses = new HashSet<>();

        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while(ee.hasMoreElements()) {
                    ipAddresses.add((InetAddress)ee.nextElement());
                }
            }
        } catch(SocketException ex) {
            System.out.println("Can't get the interfaces for this device, having problems detecting the ip addresses: "
                    + ex.toString());
            ex.printStackTrace();
        }

        return ipAddresses;
    }

    /**
     * Will stop querying ips for tcp connections.
     * @param port the port with which we should stop scanning for
     */
    public void stop(int port) {
        System.out.println("Stopping discovery on port: " + port);
        Detect d = discoveryMap.get(port);
        if(d != null) {
            d.getConnectionThread().interrupt();
            Iterator<TCPMonitor> iterator = d.getTcpMonitors().iterator();
            while(iterator.hasNext()) {
                TCPMonitor t = iterator.next();
                t.killthread();
                try {
                    t.join();
                } catch(InterruptedException ex) {
                    //
                }
            }
        }
        discoveryMap.remove(port);
    }

    private class Detect {
        private Thread connectionThread;
        private Set<TCPMonitor> tcpMonitors;

        Detect(Thread connectionThread, Set<TCPMonitor> tcpMonitors) {
            this.connectionThread = connectionThread;
            this.tcpMonitors = tcpMonitors;
        }

        Thread getConnectionThread() {
            return connectionThread;
        }

        Set<TCPMonitor> getTcpMonitors() {
            return tcpMonitors;
        }
    }
}
