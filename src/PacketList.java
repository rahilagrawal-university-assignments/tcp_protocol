import java.util.ArrayList;
import java.util.Collections;

public class PacketList {

    private ArrayList < Packet > packets;

    public PacketList() {
        packets = new ArrayList < Packet > ();
    }

    public void add(Packet packet) {
        for (Packet p: this.packets) {
            if (p.getSeqNo() == packet.getSeqNo() && p.getAckNo() == packet.getAckNo()) {
                p.setData(packet.getData());
                return;
            }
        }
        this.packets.add(packet);
    }

    public Packet getPacketByAck(int ack) {
        for (Packet p: packets) {
            if (p.getAckNo() == ack) return p;
        }
        return null;
    }

    public Packet getPacketBySeq(int seq) {
        for (Packet p: packets) {
            if (p.getSeqNo() == seq) return p;
        }
        return null;
    }

    public boolean packetWithSeqExists(int seqNo) {
        for (Packet p: packets) {
            if (p.getSeqNo() == seqNo) return true;
        }
        return false;
    }

    public boolean packetWithAckExists(int ackNo) {
        for (Packet p: packets) {
            if (p.getAckNo() == ackNo) return true;
        }
        return false;
    }

    public ArrayList < Packet > getPackets() {
        Collections.sort(this.packets);
        return this.packets;
    }
    public Packet getLastPacket() {
        if(packets.size() == 0) return null;
        return this.getPackets().get(packets.size() - 1);
    }

    public int getPacketSeqbyAck(int ack){
        for (Packet p: packets) {
            if (p.getSeqNo() + p.getData().length == ack) return p.getSeqNo();
        }
        return -1;
    }

}