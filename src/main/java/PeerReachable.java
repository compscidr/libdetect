import java.net.InetAddress;
import java.net.Socket;

public class PeerReachable {
    InetAddress address;
    Socket socket;

    PeerReachable(InetAddress address, Socket socket) {
        this.address = address;
        this.socket = socket;
    }
}
