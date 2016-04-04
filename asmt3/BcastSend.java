import rf.RF;
import java.nio.ByteBuffer;
import java.util.Random;

/*
* Handles sending thread for Asmt 3.
* Network Programming CSCI 325
* Tori | Mar 6 2016
*/
public class BcastSend implements Runnable
{
    RF rflayer;
    int mac;
    ByteBuffer long_buffer;

    public BcastSend(RF rflayer, int mac){
      this.rflayer = rflayer;
      this.mac = mac;
      long_buffer = ByteBuffer.allocate(Long.BYTES);
    }
    
    /*
    * Assembles a 10-byte packet from a MAC address and a clock time.
    */
    public byte[] makePacket(short shortMac,long clock){
      //The array representing the packet.
      byte[] packet = new byte[10];

      //The first two bytes represent the sender's MAC address.
      packet[0] = (byte) (shortMac >> 8);
      packet[1] = (byte) shortMac;

      //The latter eight bytes represent the the clock value.
      //Use the bytebuffer to get the byte values easily.
      long_buffer.putLong(clock);
      byte[] clock_array = long_buffer.array();
      long_buffer.clear();

      //Fill in the packet with the values returned by the bytebuffer.
      for(int i=0;i<8;i++){
        packet[i+2] = clock_array[i];
      }

      //Return the packet.
      return packet;
    }

    /**
     * Thread that sends packets at random intervals forever.
     */
    public void run(){
      System.out.println("Writer is alive and well");

      //Instantiate random interval generation.
      Random r = new Random();
      float interval;
      //Infinite loop that sends packets.
      while(true){
        //Calculate a new random interval.
        interval = r.nextInt(7000);
        //Wait for the amount of time in the interval.
        try{
          Thread.sleep((long)interval);
        }
          catch(InterruptedException e){
            System.out.println("Thread was interrupted!");
            e.printStackTrace();
          }

      //Once we are done waiting, assemble the packet to be sent.
      long clock = rflayer.clock();
      byte[] packet = makePacket((short)mac,clock);

      //Transmit the packet.
      int bytesSent = rflayer.transmit(packet);

      //Check to make sure the entire packet was sent.
      if (bytesSent != packet.length){
          System.err.println("Only sent "+bytesSent+" bytes of data!");
        }
      //Print out the packet's data.
        System.out.print("Sent packet: "+mac+" "+clock);
        System.out.print("  [");
        for(int i=0;i<10;i++){
          System.out.print(" "+packet[i]);
        }
        System.out.print(" ]");
        System.out.println("");
      }
     }
}
