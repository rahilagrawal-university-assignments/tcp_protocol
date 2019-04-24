/*
    Created by : Rahil Agrawal - z5165505
    Created at : 9 September 2018
    This file contains code for Sender
*/

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Sender {

    public Helper helper;
    public double startTime = System.nanoTime();
    public int seqNo = 0;
    public PacketList packetList;
    public int fileSize = 0;
    public int nSegments = 4;
    public int PLDSegments = 0;
    public int droppedSegments = 0;
    public int corruptedSegments = 0;
    public int reOrderedSegments = 0;
    public int duplicateSegments = 0;
    public int delayedSegments = 0;
    public int retransmittedSegments = 0;
    public int fastRetransmissionPackets = 0;
    public int dupAcks = 0;
    public ArrayList < Long > times;

    public PacketList sentPackets = new PacketList();

    public Sender() {
        this.helper = new Helper();
        this.startTime = System.nanoTime();
        this.seqNo = 0;
        this.packetList = new PacketList();
        this.times = new ArrayList < Long > ();
    }

    public static void main(String[] args) throws Exception {

        Sender sender = new Sender();

        // Process and Store arguments
        sender.helper.processArgs(args);
        InetAddress host = sender.helper.getSenderHost();
        int port = sender.helper.getSenderPort();
        int MSS = sender.helper.getMSS();

        // Setup variables for connection and data transfer
        int packetSize = MSS + 150;
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout((int)(500 + sender.helper.getGamma() * 250));

        sender.helper.fos = new FileOutputStream("senderLog");
        // Establish Connection
        while (sender.establishConnection(host, port, socket, packetSize, sender));

        // Transfer Data
        sender.createPacketList(MSS, sender);
        sender.sendData(host, port, sender.packetList, socket, packetSize, sender);

        //End Connection
        sender.endConnection(host, port, socket, packetSize, sender);
    }

    private boolean establishConnection(InetAddress host, int port, DatagramSocket socket, int packetSize, Sender sender) throws Exception {
        byte[] data = ByteBuffer.allocate(4).putInt(packetSize).array();
        Packet packet = new Packet(1, data, 0, 0);
        sender.helper.sendMessage(sender.helper.toStream(packet), host, port, socket);
        sender.helper.log("snd", sender.helper.getElapsedTime(sender.startTime), "S", packet);
        DatagramPacket reply = new DatagramPacket(new byte[packetSize], packetSize);
        try {
            socket.receive(reply);
            Packet response = sender.helper.toPacket(reply.getData());
            if (response.getType() == 2) {
                sender.helper.log("rcv", sender.helper.getElapsedTime(sender.startTime), "SA", response);
                data = new byte[0];
                packet = new Packet(6, data, response.getAckNo(), 1);
                sender.helper.log("snd", sender.helper.getElapsedTime(sender.startTime), "A", packet);
                sender.helper.sendMessage(sender.helper.toStream(packet), host, port, socket);
                return false;
            }
        } catch (SocketTimeoutException e) {
            return true;
        }
        return true;
    }

    private void endConnection(InetAddress host, int port, DatagramSocket socket, int packetSize, Sender sender) throws Exception {
        byte[] data = new byte[0];
        Packet packet = new Packet(3, data, sender.seqNo, 1);
        sender.helper.sendMessage(sender.helper.toStream(packet), host, port, socket);
        sender.helper.log("snd", sender.helper.getElapsedTime(sender.startTime), "F", packet);
        Packet response;
        try {
            ReceiverThread responseThread = new ReceiverThread(packetSize, socket, sender.helper);
            Thread responseHandler = new Thread(responseThread);
            responseHandler.start();
            while (true) {
                response = responseThread.getLatestResponse();
                if (response == null) continue;
                if (response.getType() == 4) break;
            }
            if (response.getType() == 4) {
                sender.helper.log("rcv", sender.helper.getElapsedTime(sender.startTime), "A", response);
                while (true) {
                    response = responseThread.getLatestResponse();
                    if (response == null) continue;
                    if (response.getType() == 3) break;
                }
                if (response.getType() == 3) {
                    sender.helper.log("rcv", sender.helper.getElapsedTime(sender.startTime), "F", response);
                    packet = new Packet(4, data, response.getAckNo(), response.getSeqNo() + 1);
                    sender.helper.sendMessage(sender.helper.toStream(packet), host, port, socket);
                    sender.helper.log("snd", sender.helper.getElapsedTime(sender.startTime), "A", packet);
                    sender.helper.fos.write("==========================================================\n".getBytes());
                    sender.helper.fos.write(String.format("Size of the file (in Bytes)                         " + sender.fileSize + "\n").getBytes());
                    sender.helper.fos.write(String.format("Segments Transmitted (including drop & RXT)         " + sender.nSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Segments handled by PLD                   " + sender.PLDSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Segments dropped                          " + sender.droppedSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Segments Corrupted                        " + sender.corruptedSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Segments Re-ordered                       " + sender.reOrderedSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Segments Duplicated                       " + sender.duplicateSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Segments Delayed                          " + sender.delayedSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of Retransmissions due to TIMEOUT            " + sender.retransmittedSegments + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of FAST RETRANSMISSION                       " + sender.fastRetransmissionPackets + "\n").toString().getBytes());
                    sender.helper.fos.write(String.format("Number of DUP ACKS received                         " + sender.dupAcks + "\n").toString().getBytes());
                    sender.helper.fos.write("==========================================================\n".getBytes());
                    sender.helper.fos.close();
                    socket.close();
                    System.exit(0);
                }
            }

        } catch (SocketTimeoutException e) {
            System.out.println("TIMEOUT");
            System.exit(1);
        }
    }

    private void createPacketList(int MSS, Sender sender) {
        byte[] data = new byte[MSS];
        int bytesRead = 0;
        sender.seqNo = 1;
        try {
            FileInputStream fis = new FileInputStream(sender.helper.getSenderFile());
            while ((bytesRead = fis.read(data)) != -1) {
                sender.fileSize += bytesRead;
                sender.packetList.add(new Packet(5, Arrays.copyOfRange(data, 0, bytesRead), sender.seqNo, 1));
                sender.seqNo += bytesRead;
            }
            // Close the file
            fis.close();
            sender.seqNo = 1;
        } catch (IOException ioe) {
            System.out.println("Error " + ioe.getMessage());
        }
    }

    private void sendData(InetAddress host, int port, PacketList packetList, DatagramSocket socket, int packetSize, Sender sender) throws Exception {

        ReceiverThread responseThread = new ReceiverThread(packetSize, socket, sender.helper);
        Thread responseHandler = new Thread(responseThread);
        responseHandler.start();
        PacketList receivedAcks = new PacketList();
        int tripleAck = 0;
        int MWS = sender.helper.getMWS();
        int lastByteSent = 1;
        int lastByteAcked = 1;
        int currByteAcked = 1;
        int lastByteSuccessfullySent = 1;
        double estimatedRTT = 500;
        double devRTT = 250;
        double sampleRTT = estimatedRTT;
        while (true) {


            if (responseThread.isTimeout()) {
                lastByteSent = lastByteAcked;
                sender.seqNo = lastByteAcked;
                sender.retransmittedSegments++;
            }
            while (lastByteSent - lastByteAcked <= MWS) {
                if (sender.seqNo >= sender.fileSize + 1) {
                    if (lastByteAcked != sender.fileSize + 1) break;
                    responseThread.terminate();
                    responseHandler.join();
                    return;
                }
                Packet packet;
                packet = packetList.getPacketBySeq(sender.seqNo);
                SenderThread requestHandler = new SenderThread(host, port, socket, sender, packet);
                if (sentPackets.packetWithSeqExists(sender.seqNo)) {
                    requestHandler.run();
                    sender.seqNo = lastByteSuccessfullySent;
                    lastByteSent = lastByteSuccessfullySent;
                } else {
                    requestHandler.run();
                    sender.seqNo += packet.getData().length;
                    lastByteSuccessfullySent += packet.getData().length;
                    lastByteSent = lastByteSuccessfullySent;
                }
            }
            // Receive reply from other node and process it.
            while (true) {
                Packet response = responseThread.getLatestResponse();
                if (response == null) break;

                if (lastByteAcked < response.getAckNo()) lastByteAcked = response.getAckNo();
                if (currByteAcked == response.getAckNo()) tripleAck++;
                else tripleAck = 0;
                currByteAcked = response.getAckNo();

                if (receivedAcks.packetWithAckExists(response.getAckNo())) {
                    sender.dupAcks++;
                    sender.helper.log("rcv/DA", sender.helper.getElapsedTime(sender.startTime), "A", response);
                    times.remove(0);
                    responseThread.getTimeofLatestPacket();
                } else {
                    long receivalTime = responseThread.getTimeofLatestPacket();
                    sender.helper.log("rcv", sender.helper.getElapsedTime(sender.startTime), "A", response);
                    if (receivalTime == -1) sampleRTT = estimatedRTT;
                    else sampleRTT = receivalTime - times.remove(0);
                    estimatedRTT = 0.875 * estimatedRTT + 0.125 * sampleRTT;
                    devRTT = 0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimatedRTT);
                    int timeout = (int)(estimatedRTT + sender.helper.getGamma() * devRTT);
                    if (timeout > 5000) timeout = 5000;
                    responseThread.setTimeout((timeout));
                    System.out.println(timeout);
                    receivedAcks.add(response);
                }
                if (tripleAck == 3 || response.getAckNo() == sentPackets.getLastPacket().getSeqNo()) {
                    sender.seqNo = response.getAckNo();
                    lastByteSent = response.getAckNo();
                    tripleAck = 0;
                    sender.fastRetransmissionPackets++;
                    break;
                }
                if (lastByteSent - lastByteAcked > MWS) {
                    sender.seqNo = lastByteAcked;
                    lastByteSent = lastByteAcked;
                    break;
                }
            }
        }
    }
    public boolean allPacketsAreValid() {
        for (Packet p: packetList.getPackets()) {
            Packet p2 = sentPackets.getPacketBySeq(p.getSeqNo());
            if (p2 == null) return false;
            if (p2.getHash() != p.getHash()) return false;
        }
        return true;
    }

    Packet reOrderedPacket = null;
    int waitPackets = 0;
}