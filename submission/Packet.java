import java.io.Serializable;
import java.util.Arrays;

public class Packet implements Serializable, Comparable<Packet> {

    // SYN 1
    // SYNACK 2
    // FIN 3
    // FINACK 4
    // DATA 5
    // ACK 6

    private int type;
    private int seqNo;
    private int ackNo;
    private byte[] data;
    private int hash;

    public Packet(int type, byte[] data, int seqNo, int ackNo){
        this.type = type;
        this.data = data;
        this.seqNo = seqNo;
        this.ackNo = ackNo;
        this.hash = Arrays.hashCode(data);
    }

    public int getType() {
        return type;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public int getAckNo() {
        return ackNo;
    }

    public byte[] getData() {
        return data;
    }

    public int getHash(){
        return hash;
    }

    public void setData(byte[] data){
        this.data = data;
    }
    
    @Override
    public int compareTo(Packet p){
        return this.seqNo - p.seqNo == 0 ? this.ackNo - p.ackNo : this.seqNo - p.seqNo ;
    }
}
