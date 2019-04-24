/*
    Created by : Rahil Agrawal - z5165505
    Created at : 28 September 2018
    This file contains Helper functions
*/

// region import
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
// endregion

public class Helper {

    public Helper() {
        random = new Random(getSeed());
    }

    static double startTime = System.nanoTime();
    public FileOutputStream fos;

    // Code Segment Borrowed from : https://www.javahelps.com/2015/07/serialization-in-java.html
    //region packet conversions
    public byte[] toStream(Packet packet) {
        byte[] stream = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos);) {
            oos.writeObject(packet);
            stream = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream;
    }

    // Code Segment Borrowed from : https://www.javahelps.com/2015/07/serialization-in-java.html
    public Packet toPacket(byte[] stream) {
        Packet packet = null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(stream); ObjectInputStream ois = new ObjectInputStream(bais);) {
            packet = (Packet) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return packet;
    }

    //endregion

    public void sendMessage(byte[] packet, InetAddress host, int port, DatagramSocket socket) {
        DatagramPacket request = new DatagramPacket(packet, packet.length, host, port);
        try {
            socket.send(request);
        } catch (Exception e) {
            System.out.println("No");
        }
    }

    public void log(String event, float timeElapsed, String type, Packet packet) {
        try{
        if (!(type.equals("S")))
            fos.write(String.format("%-8s      %-8.2f      %-8s      %-8d      %-8d      %-8d\n", event, timeElapsed, type, packet.getSeqNo(), packet.getData().length, packet.getAckNo()).toString().getBytes());
        else
            fos.write(String.format("%-8s      %-8.2f      %-8s      %-8d      %-8d      %-8d\n", event, timeElapsed, type, packet.getSeqNo(), 0, packet.getAckNo()).toString().getBytes());
        }catch(Exception e){}
    }

    public float getElapsedTime(double startTime) {
        double endTime = System.nanoTime();
        double timeElapsed = (endTime - startTime) / 1000000000.0;
        return Float.parseFloat(String.format("%f", timeElapsed));
    }

    //region Host Arguments
    public void processArgs(String[] args) {
        if (args.length != 14) {
            System.out.println("Required arguments: receiver_port file.pdf MWS MSS gamma pDrop pDuplicate pCorrupt pOrder maxOrder pDelay maxDelay seed");
            System.exit(1);
        }
        try {
            senderHost = InetAddress.getByName(args[0]);
        } catch (IOException e) {
            System.out.println("Host not found");
        }
        senderPort = Integer.parseInt(args[1]);
        senderFileName = args[2];
        MWS = Integer.parseInt(args[3]);
        MSS = Integer.parseInt(args[4]);
        gamma = Float.parseFloat(args[5]);
        pDrop = Float.parseFloat(args[6]);
        pDuplicate = Float.parseFloat(args[7]);
        pCorrupt = Float.parseFloat(args[8]);
        pOrder = Float.parseFloat(args[9]);
        maxOrder = Integer.parseInt(args[10]);
        pDelay = Float.parseFloat(args[11]);
        maxDelay = Integer.parseInt(args[12]);
        seed = Integer.parseInt(args[13]);
    }

    static String senderFileName;
    static InetAddress senderHost;
    static int senderPort;
    static int MWS;
    static int MSS;
    static float gamma;
    static float pDrop;
    static float pDuplicate;
    static float pCorrupt;
    static float pOrder;
    static float pDelay;
    static int maxDelay;
    static int maxOrder;
    static int seed;
    static Random random;


    public String getSenderFile() {
        return senderFileName;
    }

    public InetAddress getSenderHost() {
        return senderHost;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public int getMWS() {
        return MWS;
    }

    public int getMSS() {
        return MSS;
    }

    public int getMaxOrder() {
        return maxOrder;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public int getSeed() {
        return seed;
    }

    public float getpOrder() {
        return pOrder;
    }

    public float getpDuplicate() {
        return pDuplicate;
    }

    public float getpCorrupt() {
        return pCorrupt;
    }

    public float getpDrop() {
        return pDrop;
    }

    public float getpDelay() {
        return pDelay;
    }

    public float getGamma() {
        return gamma;
    }

    //endregion

    public float getProb() {
        return random.nextFloat();
    }
}