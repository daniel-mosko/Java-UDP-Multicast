import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class InfoServer {
    public static final int SERVER_PORT = 5678;
    public static final int REQUEST_PORT = 6789;
    public static final String FILE_PATH = "PATH\\pruvodce-labyrintem-algoritmu-1.pdf";

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {   // nastavime pocuvanie na porte
            System.out.println("SERVER POCUVA NA PORTE " + SERVER_PORT);
            File file = new File(FILE_PATH);
            String fileName = file.getName();
            long fileSize = file.length();
            String response = fileName + "\n" + fileSize;
            byte[] responseData = response.getBytes();

            Interval intervalsToSend = Interval.empty(0, fileSize);
            RequestServerTask requestServerTask = new RequestServerTask(intervalsToSend);
            Thread worker1 = new Thread(requestServerTask);
            worker1.start();
            FileSenderTask fileSenderTask = new FileSenderTask(intervalsToSend,file);
            Thread worker2 = new Thread(fileSenderTask);
            worker2.start();

            while (true) {
                byte[] emptyBuf = new byte[socket.getReceiveBufferSize()];
                DatagramPacket requestPacket = new DatagramPacket(emptyBuf, emptyBuf.length);
                // naplni packet
                socket.receive(requestPacket); // spí, pokial nieco nepride
                System.out.println("** Prisla poziadavka z IP: " + requestPacket.getAddress() + " **");
                String request = new String(requestPacket.getData()).trim();

                if (InfoClient.INFO_REQUEST.equals(request)) {
                    System.out.println("Posielam data");
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, //data
                            responseData.length, //data size
                            requestPacket.getAddress(),
                            requestPacket.getPort());
                    socket.send(responsePacket);
                }
            }

        } catch (SocketException e) {
            System.err.println("Port je obsadeny, vypnite stary server");
        } catch (IOException e) {
            System.err.println("Nemozem pocuvat na sieti... mam prilis male prava");
            e.printStackTrace();
        }
    }
}
