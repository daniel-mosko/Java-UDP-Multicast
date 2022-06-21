import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public record FileSenderTask(Interval intervalsToSend, File fileToSend) implements Runnable {
    public static final int CHUNK_SIZE = 1000;
    public static final String BROADCAST_IP = "ENTER YOUR IP";

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            RandomAccessFile raf = new RandomAccessFile(fileToSend, "r");
            while (true) {
                Interval interval = intervalsToSend.getAndEraseNextFullSubintervalBlocked(CHUNK_SIZE);
                // Nacitame z disku subor
                raf.seek(interval.getMin());
                byte[] filePart = new byte[(int) (interval.length())]; // kedze max je 87, tak mozeme pretypovat
                raf.read(filePart);

                // TODO naplnit data a odoslat
                /* --------------- */

                // 8+8B pre zapisanie intervalu (long)
                ByteArrayOutputStream baos = new ByteArrayOutputStream(16 + filePart.length);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeLong(interval.getMin()); // zaciatok intervalu
                oos.writeLong(interval.getMax()); // koniec intervalu
                oos.write(filePart); // data suboru
                oos.flush();

                byte[] data = baos.toByteArray();
                // naplnime packet
                DatagramPacket dataPacket = new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName(BROADCAST_IP),
                        InfoClient.DATA_PORT);
                socket.send(dataPacket); // posleme packet

                /* --------------- */
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
