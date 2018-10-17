import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TCPMonitor extends Thread {

    private ActionListener actionListener;
    private InetAddress subnetIpAddress;
    private int port;
    private volatile Socket s;
    private boolean running;

    public TCPMonitor(InetAddress subnetIpAddress, int port, ActionListener actionListener) {
        this.actionListener = actionListener;
        this.subnetIpAddress = subnetIpAddress;
        this.port = port;
    }

    @Override
    public void run() {
        running = true;
        while(running) {
            boolean connected = false;
            try {
                this.s = new Socket(subnetIpAddress, port);
                OutputStream os = s.getOutputStream();
                connected = true;
                //if we get here, we succesfully made the connection
                actionListener.onPeerReachable(new PeerReachable(subnetIpAddress, s));

                while(s.isConnected() && !s.isClosed() && !s.isInputShutdown() && !s.isOutputShutdown()) {
                    //only way we can actually detect the close is if we try to write something periodically
                    byte[] temp = new byte[1];
                    temp[0] = 0;
                    os.write(temp, 0, 1);
                    os.flush();
                    Thread.sleep(100);
                    //System.out.println("Still connected to " + subnetIpAddress.getCanonicalHostName());
                }
                actionListener.onPeerUnreachable(new PeerUnreachable(subnetIpAddress));
            } catch (IOException ex) {
                //if we get here and connected is true, fire the failure event
                if(connected) {
                    actionListener.onPeerUnreachable(new PeerUnreachable(subnetIpAddress));
                    connected = false;
                }
            } catch(InterruptedException ex) {
                try {
                    running = false;
                    s.close();
                } catch(IOException cex) {
                    cex.printStackTrace();
                }
            }
        }
    }

    public void killthread() {
        running = false;
        if(s == null) {
            return;
        }
        try {
            this.s.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
