import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class InfoClient {

    public static final String SERVER_NAME = "localhost"; //ip adresa
    public static final String INFO_REQUEST = "Send file info";
    public static final int MAX_INTERVALS = 87;
    public static final int TIMEOUT = 100; // 100ms
    public static final int DATA_PORT = 6578;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] requestData = INFO_REQUEST.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(requestData, //data
                    requestData.length, //data size
                    InetAddress.getByName(SERVER_NAME), InfoServer.SERVER_PORT);
            socket.send(requestPacket);

            byte[] emptyBuf = new byte[socket.getReceiveBufferSize()];
            DatagramPacket responsePacket = new DatagramPacket(emptyBuf, emptyBuf.length);
            // naplni packet
            socket.receive(responsePacket); // spí, pokial nieco nepride
            String response = new String(responsePacket.getData()).trim();

            Scanner sc = new Scanner(response);
            String fileName = sc.nextLine();
            long fileSize = sc.nextLong();
            sc.close();
            //System.out.println("Subor: " + fileName + ", velkost: " + fileSize / 1_000_000.0 + " MB ");
            System.out.println("Subor: " + fileName + ", velkost: " + fileSize + " B ");

            /*------------------- 25.03.2022 --------------------------*/
            Interval intervalsIHave = Interval.empty(0, fileSize);
            File file = new File(fileName);
            // nieco ako PrintWriter len na binarne subory ... random = priamy pristup
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(fileSize); // vznikne prazdny subor danej velkosti

            try (DatagramSocket dataSocket = new DatagramSocket(DATA_PORT)) {
                boolean stahujem = true;
                while (true) {
                    dataSocket.setSoTimeout(TIMEOUT + (int) (Math.random() * 100)); // kazdy klient v nahodnom poradi, aby
                    // cakam na data co posiela server cez broadcast
                    emptyBuf = new byte[dataSocket.getReceiveBufferSize()];
                    DatagramPacket dataPacket = new DatagramPacket(emptyBuf, emptyBuf.length);
                    try {
                        // naplni packet
                        dataSocket.receive(dataPacket); // spí, pokial nieco nepride

                        // TODO prijate data zapisat na disk a v intervalsIHave zaznacit - PROJEKT
                        /* --------------- */

                        if (stahujem) {
                            System.out.println("Stahujem subor...");
                            stahujem = false;
                        }

                        byte[] data = dataPacket.getData(); // ulozime si prijate data

                        ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        ObjectInputStream oos = new ObjectInputStream(bais);
                        long min = oos.readLong(); // minimum
                        long max = oos.readLong(); // maximum intervalu
                        if (intervalsIHave.isMissing(max)) {
                            intervalsIHave.addFullSubinterval(min, max); // zaznacime data, ktore prisli
                            int velkostDat = (int) (max - min) + 1;
                            byte[] obsah = new byte[velkostDat];
                            oos.read(obsah); // precitame data do pola bytov
                            raf.write(obsah); // zapiseme data na disk
                        }

                        // ak uz mame vsetky intervaly, odpojime sa
                        if (intervalsIHave.getEmptySubintervals(MAX_INTERVALS).isEmpty()) {
                            raf.close();
                            socket.close();
                            System.out.println("STAHOVANIE DOKONCENE - odpajam sa");
                            break;
                        }

                        /* --------------- */


                    } catch (SocketTimeoutException e) {
                        // Server prestal posielat data, poslem co mi chyba
                        List<Interval> intervalsINeed = intervalsIHave.getEmptySubintervals(MAX_INTERVALS);
                        // integer (4B) a pre kazdy interval 8+8B long
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(4 + intervalsINeed.size() * 16);
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeInt(intervalsINeed.size());
                        for (Interval interval : intervalsINeed) {
                            oos.writeLong(interval.getMin());
                            oos.writeLong(interval.getMax());
                        }
                        oos.flush();
                        byte[] reqData = baos.toByteArray();
                        DatagramPacket reqPacket = new DatagramPacket(
                                reqData,
                                reqData.length,
                                InetAddress.getByName(SERVER_NAME),
                                InfoServer.REQUEST_PORT);
                        socket.send(reqPacket);
                    }
                }
            }


        } catch (SocketException e) {
            // maximalne nepravdepodobne
            System.err.println("Nemame ziadne volne porty");
            e.printStackTrace();
        } catch (UnknownHostException e) {
            System.err.println(SERVER_NAME + " nema znamu IP adresu");
        } catch (IOException e) {
            // Mala pravdepodobnost
            // Nepodarilo sa komunikovat po internete (nas PC to nebol schopny poslat do internetu)
            System.err.println("neviem polsat packet do internetu, mam prava?");
            e.printStackTrace();
        }
    }
}
