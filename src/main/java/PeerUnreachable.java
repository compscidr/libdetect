import java.net.InetAddress;

public class PeerUnreachable {
    InetAddress address;

    PeerUnreachable(InetAddress address) {
        this.address = address;
    }

    InetAddress getAddress() {
        return address;
    }

}