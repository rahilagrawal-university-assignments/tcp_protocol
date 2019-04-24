import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.ArrayList;

public class ReceiverThread implements Runnable {
    private DatagramSocket socket;
    private int packetSize;
    private boolean running;
    private Helper helper;
    private ArrayList < Packet > packets;
    private boolean timeout;
    private ArrayList <Long> times;
    private int timeoutValue;

    public ReceiverThread(int packetSize, DatagramSocket socket, Helper helper) {
        this.socket = socket;
        this.packetSize = packetSize;
        this.running = true;
        this.helper = helper;
        this.packets = new ArrayList < Packet > ();
        this.timeout = false;
        this.times = new ArrayList<Long>();
        this.timeoutValue = 0;

    }

    public void terminate() {
        this.running = false;
    }

    public void run() {
        while (true) {
            if(!running) break;
            try {
                if(!(timeoutValue == 0)) socket.setSoTimeout(timeoutValue);
                DatagramPacket reply = new DatagramPacket(new byte[this.packetSize], this.packetSize);
                socket.receive(reply);
                Packet response = helper.toPacket(reply.getData());
                packets.add(response);
                times.add(System.currentTimeMillis());
                this.timeout = false;
                if(response.getType() == 3) break;
            } catch (Exception e) {
                this.timeout= true;
            }
        }
    }

    public Packet getLatestResponse() {
        try{
            return packets.remove(0);
        }
        catch(Exception e){
            return null;
        }
    }

    public boolean isTimeout(){
        return this.timeout;
    }

    public long getTimeofLatestPacket(){
        try{
            return times.remove(0);
        }
        catch(Exception e){
            return -1;
        }    
    }

    public void setTimeout(int timeout){
        this.timeoutValue = timeout;
    }
}