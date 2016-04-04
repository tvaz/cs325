import rf.RF;
import java.nio.ByteBuffer;
import java.util.Random;
/*
* Handles recieving thread for Asmt 3.
* Network Programming CSCI 325
* Tori | Mar 6 2016
*/
public class BcastRecv implements Runnable
{
    RF rflayer;
    int mac;
    ByteBuffer long_buffer;

    public BcastRecv(RF rflayer, int mac){
      this.rflayer = rflayer;
      this.mac = mac;
      long_buffer = ByteBuffer.allocate(Long.BYTES);
    }

    /*
    * Takes a 10 byte packet and reads it.
    * Returns an array with two values:
    * array[0] is the sender's MAC address;
    * array[1] is the clock time.
    */
    public long[] readPacket(byte[] packet){
      //The first two bytes represent the sender's MAC address.
      short packet_mac = (short)( ((packet[0]&0xFF)<<8) | (packet[1]&0xFF) );

      //The latter eight bytes represent the the clock value.
      //Use the bytebuffer to read the bytes into a long.
      byte[] data = new byte[8];
      for(int i=2;i<Long.BYTES;i++){
        data[i] = packet[i+2];
      }
      long_buffer.put(data);
      long_buffer.flip();
      long clock = long_buffer.getLong();
      long_buffer.clear();

      //Return the MAC address and the parsed clock time.
      long[] toReturn = new long[2];
      toReturn[0] = (long) packet_mac;
      toReturn[1] = clock;
      return toReturn;
    }

    /**
     * Waits for packets, and prints them out when they are received.
     */
    public void run(){
      System.out.println("Reader is alive and well");

      //Infinite loop that waits for packets.
      while(true){
      //Wait for packets
      byte[] packet;
      packet = rflayer.receive();
      //Check to make sure its the proper length
      if(packet.length != 10){
        System.err.println("Only received "+packet.length+" bytes of data!");
      }

      //read packet when received
      long[] data = readPacket(packet);
      //Print out bytes
      System.out.print("Recieved [");
        for(int i=0;i<10;i++){
          System.out.print(" "+packet[i]);
        }
        System.out.print(" ]");
        System.out.println("");
        //Print out results
        System.out.println("Host "+data[0]+" says time is "+data[1]);

      }
     }
}
