import java.net.DatagramSocket;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;

public class SenderThread {

    private Sender sender;
    private InetAddress host;
    private int port;
    private DatagramSocket socket;
    private Packet packet;

    public SenderThread(InetAddress host, int port, DatagramSocket socket, Sender sender, Packet packet) {
        this.sender = sender;
        this.host = host;
        this.port = port;
        this.socket = socket;
        this.packet = packet;
    }

    public void run() {
        Helper helper = sender.helper;
        // Get PLD Module fields
        float pDrop = helper.getpDrop();
        float pCorrupt = helper.getpCorrupt();
        float pDuplicate = helper.getpDuplicate();
        float pOrder = helper.getpOrder();
        int maxOrder = helper.getMaxOrder();
        sender.nSegments++;
        sender.PLDSegments++;
        if (sender.reOrderedPacket != null && sender.waitPackets == 0) {
                packet = sender.reOrderedPacket;
                helper.sendMessage(helper.toStream(packet), host, port, socket);
                sender.times.add(System.currentTimeMillis());
                helper.log("snd/rord", helper.getElapsedTime(sender.startTime), "D", packet);
                sender.reOrderedPacket = null;
                sender.reOrderedSegments++;
            } else {
                if (sender.waitPackets > 0) {
                    sender.waitPackets--;
                }
        if (helper.getProb() <= pDrop) {
            helper.log("drop", helper.getElapsedTime(sender.startTime), "D", packet);
            sender.droppedSegments++;
        } else if (helper.getProb() <= pDuplicate) {
            helper.sendMessage(helper.toStream(packet), host, port, socket);
            helper.log("snd", helper.getElapsedTime(sender.startTime), "D", packet);
            sender.times.add(System.currentTimeMillis());
            helper.sendMessage(helper.toStream(packet), host, port, socket);
            helper.log("snd/dup", helper.getElapsedTime(sender.startTime), "D", packet);
            sender.times.add(System.currentTimeMillis());
            sender.nSegments++;
            sender.duplicateSegments++;
            sender.PLDSegments++;
        } else if (helper.getProb() <= pCorrupt) {
            Packet copyPacket = new Packet(packet.getType(), Arrays.copyOf(packet.getData(), packet.getData().length), packet.getSeqNo(), packet.getAckNo());
            byte[] data = copyPacket.getData();
            data[0] = data[0] == (byte) 1 ? (byte) 0 : (byte) 1;
            copyPacket.setData(data);
            helper.sendMessage(helper.toStream(copyPacket), host, port, socket);
            helper.log("snd/corr", helper.getElapsedTime(sender.startTime), "D", packet);
            sender.times.add(System.currentTimeMillis());
            sender.corruptedSegments++;
        }
        else if (helper.getProb() <= pOrder) {
            //no packet is waiting, so make this packet wait
            if (sender.reOrderedPacket == null) {
                sender.reOrderedPacket = packet;
                sender.waitPackets = maxOrder;
            } else {
                helper.sendMessage(helper.toStream(packet), host, port, socket);
                helper.log("snd", helper.getElapsedTime(sender.startTime), "D", packet);
                sender.times.add(System.currentTimeMillis());
            }
        }
        // None of the PLD factors were used so transfer the packet normally.
        else {
            sender.times.add(System.currentTimeMillis());
            helper.sendMessage(helper.toStream(packet), host, port, socket);
            if (sender.sentPackets.getPacketBySeq(packet.getSeqNo()) != null) {
                helper.log("snd/RXT", helper.getElapsedTime(sender.startTime), "D", packet);
            } else {
                helper.log("snd", helper.getElapsedTime(sender.startTime), "D", packet);
            }
        }}
        sender.sentPackets.add(packet);
    }
}