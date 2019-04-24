/*
    Created by : Rahil Agrawal - z5165505
    Created at : 9 September 2018
    This file contains code for Receiver
*/

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Receiver {

    private static double startTime = System.nanoTime();
    private static InetAddress clientAddress;
    private static int port;
    private static Helper helper = new Helper();
    private static int totalBytes = 0;
    private static int nSegments = 4;
    private static int dataSegments = 0;
    private static int corruptedSegments = 0;
    private static int dupSegments = 0;
    private static int dupAcks = 0;

    public Receiver() {}

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Required arguments: port file");
            return;
        }
        int host = Integer.parseInt(args[0]);
        DatagramSocket socket = new DatagramSocket(host);

        // Get File
        String fileName = args[1];
        FileOutputStream fos = new FileOutputStream(fileName);
        
        helper.fos = new FileOutputStream("receiverLog");

        int packetSize = establishConnection(socket);
        receiveData(socket, packetSize, fos);

    }

    private static int establishConnection(DatagramSocket socket) throws Exception {
        int packetSize = 1024;
        DatagramPacket request = new DatagramPacket(new byte[packetSize], packetSize);
        socket.receive(request);
        Packet reply = helper.toPacket(request.getData());
        clientAddress = request.getAddress();
        port = request.getPort();
        if (reply.getType() == 1) {
            helper.log("rcv", helper.getElapsedTime(startTime), "S", reply);
            packetSize = ByteBuffer.wrap(reply.getData()).getInt();
        }
        byte[] data = new byte[0];
        Packet packet = new Packet(2, data, 0, 1);
        helper.sendMessage(helper.toStream(packet), clientAddress, port, socket);
        helper.log("snd", helper.getElapsedTime(startTime), "SA", packet);
        request = new DatagramPacket(new byte[packetSize], packetSize);
        try {
            socket.receive(request);
            reply = helper.toPacket(request.getData());
            if (reply.getType() == 6) {
                helper.log("rcv", helper.getElapsedTime(startTime), "A", reply);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("TIMEOUT");
        }
        return packetSize;
    }

    private static void endConnection(Packet lastPacket, DatagramSocket socket, FileOutputStream fos, int ackNo, PacketList writtenPacketList) throws Exception {
        helper.log("rcv", helper.getElapsedTime(startTime), "F", lastPacket);
        byte[] data = new byte[0];
        Packet packet = new Packet(4, data, 1, ackNo + 1);
        helper.sendMessage(helper.toStream(packet), clientAddress, port, socket);
        helper.log("snd", helper.getElapsedTime(startTime), "A", packet);
        packet = new Packet(3, data, 1, ackNo + 1);
        helper.sendMessage(helper.toStream(packet), clientAddress, port, socket);
        helper.log("snd", helper.getElapsedTime(startTime), "F", packet);
        DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
        try {
            socket.receive(reply);
            Packet response = helper.toPacket(reply.getData());
            if (response.getType() == 4) {
                helper.log("rcv", helper.getElapsedTime(startTime), "A", response);
                for(Packet p : writtenPacketList.getPackets()) fos.write(p.getData());              
                helper.fos.write("==========================================================\n".getBytes());
                helper.fos.write(String.format("Amount of data received (bytes)                       " + totalBytes + "\n").getBytes());
                helper.fos.write(String.format("Total Segments Received                               " + nSegments + "\n").toString().getBytes());
                helper.fos.write(String.format("Data segments received                                " + dataSegments + "\n").toString().getBytes());
                helper.fos.write(String.format("Data segments with Bit Errors                         " + corruptedSegments + "\n").toString().getBytes());
                helper.fos.write(String.format("Duplicate data segments received                      " + dupSegments + "\n").toString().getBytes());
                helper.fos.write(String.format("Duplicate ACKs sent                                   " + dupAcks + "\n").toString().getBytes());
                helper.fos.write("==========================================================\n".getBytes());
                helper.fos.close();
                fos.close();
                socket.close();
                System.exit(0);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("TIMEOUT");
        }
    }

    private static void receiveData(DatagramSocket socket, int packetSize, FileOutputStream fos) throws Exception {
            // Receive Data
            int ackNo = 1;
            PacketList writtenPacketList = new PacketList();
            ReceiverThread responseThread = new ReceiverThread(packetSize, socket, helper);
            Thread responseHandler = new Thread(responseThread);
            PacketList ackedPacketList = new PacketList();
            responseHandler.start();
            while (true) {
                try {
                    Packet reply = responseThread.getLatestResponse();
                    if (reply == null) continue;
                    if (reply.getType() == 3) {
                        responseThread.terminate();
                        responseHandler.join();
                        endConnection(reply, socket, fos, ackNo, writtenPacketList);
                    }
                    if(writtenPacketList.packetWithSeqExists(reply.getSeqNo())) dupSegments++;
                    nSegments++;
                    dataSegments++;
                    totalBytes += reply.getData().length;
                    if (reply.getHash() == Arrays.hashCode(reply.getData())) {
                        helper.log("rcv", helper.getElapsedTime(startTime), "D", reply);
                        writtenPacketList.add(reply);
                        ackNo = getAckNo(writtenPacketList);
                    } else {
                        helper.log("rcv/corr", helper.getElapsedTime(startTime), "D", reply);
                        if(writtenPacketList.getPackets().size() == 0) ackNo = reply.getSeqNo(); 
                        else ackNo = getAckNo(writtenPacketList);
                        corruptedSegments++;
                    }
                    Packet ack = new Packet(6, new byte[0], reply.getAckNo(), ackNo);
                    helper.sendMessage(helper.toStream(ack), clientAddress, port, socket);
                    if (ackedPacketList.packetWithSeqExists(reply.getSeqNo())) {
                        helper.log("snd/DA", helper.getElapsedTime(startTime), "A", ack);
                        dupAcks++;
                    } else {
                        helper.log("snd", helper.getElapsedTime(startTime), "A", ack);
                        ackedPacketList.add(reply);
                    }
                } catch (IOException e) {
                    System.out.println("TIMEOUT");
                }
            }
        }

    private static int getAckNo(PacketList writtenPacketList){
        if(writtenPacketList.getPackets().size() == 1){
            if(writtenPacketList.getPackets().get(0).getSeqNo() != 1) return 1;
            return writtenPacketList.getLastPacket().getSeqNo() + writtenPacketList.getLastPacket().getData().length;
        }
        if(writtenPacketList.getPackets().get(0).getSeqNo() != 1) return 1;
        for (int i = 0; i < writtenPacketList.getPackets().size() - 1; i++){
            Packet p  = writtenPacketList.getPackets().get(i);
            Packet p2 = writtenPacketList.getPackets().get(i+1);
            if(p.getSeqNo() + p.getData().length != p2.getSeqNo()){
                return p.getSeqNo() + p.getData().length;
            }
        }
        return writtenPacketList.getLastPacket().getSeqNo() + writtenPacketList.getLastPacket().getData().length;
    }
}
