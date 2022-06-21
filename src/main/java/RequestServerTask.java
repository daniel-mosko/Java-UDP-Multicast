import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RequestServerTask implements Runnable {

    private Interval intervalsToSend;

    public RequestServerTask(Interval intervalsToSend) {
        this.intervalsToSend = intervalsToSend;
    }

    @Override
    public void run() {
        // Prijatie dat
        try (DatagramSocket socket = new DatagramSocket(InfoServer.REQUEST_PORT)) {
            while (true) {
                byte[] emptyBuf = new byte[socket.getReceiveBufferSize()];
                DatagramPacket requestPacket = new DatagramPacket(emptyBuf, emptyBuf.length);
                // naplni packet
                socket.receive(requestPacket); // spí, pokial nieco nepride
                System.out.println("RequestServerTask: Prisla poziadavka");

                byte[] reqData = requestPacket.getData();
                ByteArrayInputStream bais = new ByteArrayInputStream(reqData);
                ObjectInputStream ois = new ObjectInputStream(bais);
                int intervalsCount = ois.readInt(); // vrati pocet intervalov
                for (int i = 0; i < intervalsCount; i++) {
                    long min = ois.readLong();
                    long max = ois.readLong();
                    intervalsToSend.addFullSubinterval(min, max);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
