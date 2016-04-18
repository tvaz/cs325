package wifi;
import java.io.PrintWriter;
import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author Cody Kagawa & Tori Vaz(richards)
 */
public class LinkLayer implements Dot11Interface {
	private RF theRF;           // You'll need one of these eventually
	private short ourMAC;       // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	private static final short DATAC = 0x00;
	private static final short ACK = 0x01;
	private static final short Beacon = 0x02;
	private static final short CTS = 0x03;
	private static final short RTS = 0x04;

	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;
		theRF = new RF(null, null);
		output.println("LinkLayer: Constructor ran.");
	}


	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {

		int backoff = theRF.aCWmin;

		output.println("LinkLayer: Sending "+len+" bytes to "+dest);
		//-10 to remove header overhead

		Boolean sending = true;
		while(sending){
			if(!theRF.inUse()){
				try{
					wait(theRF.aSIFSTime);
				}
				catch(InterruptedException e)
				{
					
				}
				if(!theRF.inUse()){
					sending = false;
					return theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,(short)0)) -10;
				}
			}
			while(theRF.inUse()){
				try{
					wait((int)(backoff*Math.random()));
				}
				catch(InterruptedException e)
				{
					
				}
				backoff=backoff^2;

				if(backoff > theRF.aCWmax){
					backoff = theRF.aCWmax;
				}
			}
		}

		//return theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,(short)0)) -10;
		//call recv for ack?

		int retrn = theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,(short)0)) -10;
		boolean ak = false;
		while(!ak)
		{

		}
		//call recv for ack?
		return retrn;

	}

	/**
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
		output.println("LinkLayer: Pretending to block on recv()");
		while(!theRF.dataWaiting()){
			try{
				wait(100);
			}
			catch(InterruptedException e)
			{
				return -1;
			}
		}
		Packet temp = new Packet(theRF.receive());
		if(temp.getDestAddr() == ourMAC){
			//check if wanted packet
			switch(temp.getType()){
				case DATAC:
					t.setBuf(temp.getData());
					t.setDestAddr(temp.getDestAddr());
					t.setSourceAddr(temp.getSrcAddr());
					return temp.getData().length;
				case ACK:;
				case Beacon:;
				case CTS:;
				case RTS:;
			}
			//send ack?
		}
		//will eventually replace recursion with loop
		else
		{
			return recv(t);
		}
		return 0;
	}

	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		output.println("LinkLayer: Faking a status() return value of 0");
		return 0;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		output.println("LinkLayer: Sending command "+cmd+" with value "+val);
		return 0;
	}
	private class PacketParser implements Runnable{
		private static final short WAIT_INTERVAL = 100;
		PacketParser(){

		}
		public void run()
		{
			boolean valid = true;
			while(valid)
			{
				try{
					wait(WAIT_INTERVAL);
					if(theRF.dataWaiting())
					{
						recv();
					}
				}
				catch(InterruptedException e)
				{

				}
			}
		}
	}
}
