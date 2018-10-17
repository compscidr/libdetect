public class libdetectsample {
    public static void main(String [ ] args) {

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


        test.start(443, new ActionListener() {
            @Override
            public void onPeerReachable(PeerReachable peer) {
                System.out.println("PEER REACHABLE on " + peer.address.getHostAddress() + " port: " + 443);
            }

            @Override
            public void onPeerUnreachable(PeerUnreachable peer) {
                System.out.println("PEER UNREACHABLE on " + peer.address.getHostAddress() + " port: " + 443);
            }
        }, false);

        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            //
        }

        test.stop(80);

        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            //
        }

        test.stop(443);
    }
}
