import rf.RF;
import java.util.Random;

/**
 * Main class for Asmt 3
 * Network Programming CSCI 325
 * Tori | Mar 6 2016
 */
public class BcastMain
{
    public static void main(String[] args)
    {
      //Get or create mac address randomly
      int mac = 0;
      try{
        mac = Integer.parseInt(args[0]);
        System.out.println("Using specified MAC address: "+mac);
      }
      catch(ArrayIndexOutOfBoundsException e){
        Random r = new Random();
        mac = r.nextInt(2000);
        System.out.println("Using a random MAC address: "+mac);
      }
      catch(NumberFormatException e){
        System.out.println("Please enter a number for the MAC address.");
        System.exit(0);
      }
      RF rflayer = new RF(null, null);  // Create an instance of the RF layer

      //Make the two threads
      BcastSend send = new BcastSend(rflayer,mac);
      BcastRecv recv = new BcastRecv(rflayer,mac);
      Thread t_send = new Thread(send);
      Thread t_recv = new Thread(recv);
      //Start the threads
      t_send.start();
      t_recv.start();
      //Hang forever while threads run
      while(true){}
    }
}
