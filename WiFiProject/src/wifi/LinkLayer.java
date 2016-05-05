package wifi;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

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
	private Queue<byte[]> dQueue; //send data queue
	private HashMap<Short,Short[]> sequence; //current sequence number for each destination
	enum State{IDLE,}
	private Thread fState;
	private Thread watcher; //thread around queue, watch for incoming data
	private Queue<byte[]> rQueue; //receive data queue
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
		dQueue = (Queue<byte[]>) new ArrayList<byte[]>();
		rQueue = (Queue<byte[]>) new ArrayList<byte[]>();
		sequence = new HashMap<Short,Short[]>();
		//create background threads
		fState = new Thread(new FSM());
		watcher = new Thread(new Rcver());
		//start background threads
		watcher.start();
		fState.start();
	}

	//private send function for retransmission and acks
	/*
	private void pSend(short dest, byte[] data, int len,boolean re, int retry, short type, short seqNum){
		switch(type){
		//data retransmissions
		case DATAC:
			int backoff = theRF.aCWmin;
			try{
				synchronized(this){wait(theRF.aSlotTime);}
			}
			catch(InterruptedException e)
			{

			}
			if(!theRF.inUse()){
				try{
					synchronized(this){wait(theRF.aSlotTime);}
				}
				catch(InterruptedException e)
				{

				}
				if(!theRF.inUse()){
					theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,seqNum));
				}
			}

			while(theRF.inUse()){
				try{
					wait((int)(theRF.aSlotTime+(backoff*Math.random())));
				}
				catch(InterruptedException e)
				{

				}
				backoff=backoff^2;

				if(backoff > theRF.aCWmax){
					backoff = theRF.aCWmax;
				}
		}
		case ACK:
			try{
				synchronized(this){wait(theRF.aSIFSTime);}
			}
			catch(InterruptedException e)
			{

			}
			theRF.transmit(Packet.generatePacket(data,dest,ourMAC,ACK,re,seqNum));
		}
		//more for other stuff possibly
	}
	*/



	//returns next sequence number for the given destination address -- maybe unnecessary
	private short seqCheck(short addr)
	{
		if(sequence.get(addr)==null){
			sequence.put(addr, new Short[]{0,0});
			return 0;
		}
		else{
			return sequence.get(addr)[1];
		}
	}


	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 *
	 */
	//actually prepares data for send and transfers reliability responsibility
	public int send(short dest, byte[] data, int len) {
		dQueue.offer(Packet.generatePacket(data,dest,ourMAC,DATAC,false,seqCheck(dest)));
		//notify thread if currently idle
		return data.length;
		//old code
		/*int backoff = theRF.aCWmin;

		output.println("LinkLayer: Sending "+len+" bytes to "+dest);
		//-10 to remove header overhead
		try{
			synchronized(this){wait(theRF.aSlotTime);}
		}
		catch(InterruptedException e)
		{

		}
		if(!theRF.inUse()){
			try{
				synchronized(this){Thread.sleep(theRF.aSIFSTime);}
			}
			catch(InterruptedException e)
			{

			}
			if(!theRF.inUse()){
				int c = theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,seqCheck(dest))) -10;
				//there should be some checks here and proper handling of next sequence number for dest

				System.out.println(sequence.get(dest)[1]);
				return c;
			}
		}

		while(theRF.inUse()){
			try{
				wait((int)(theRF.aSlotTime+(backoff*Math.random())));
			}
			catch(InterruptedException e)
			{

			}
			backoff=backoff^2;

			if(backoff > theRF.aCWmax){
				backoff = theRF.aCWmax;
			}
		}


		//return theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,(short)0)) -10;
		//call recv for ack?

		int retrn = theRF.transmit(Packet.generatePacket(data,dest,ourMAC,DATAC,false,(short)0)) -10;

		//?
		return retrn;*/

	}
	/**
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
		output.println("LinkLayer: Pretending to block on recv()");
		/*while(!theRF.dataWaiting()){
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
		return 0;*/
		while(rQueue.isEmpty())
		{
			try{
				synchronized(this){wait(100);}
			}
			catch(InterruptedException e)
			{
				
			}
		}
		Packet temp = new Packet(rQueue.poll());
		t.setBuf(temp.getData());
		t.setDestAddr(temp.getDestAddr());
		t.setSourceAddr(temp.getSrcAddr());
		return temp.getData().length;
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

	class FSM implements Runnable{


		State currentState;

		public FSM(){
			currentState = State.IDLE;
		}
		public void run(){
			//Run stuff
		}

	}
	/**
	 * back end recv stuff.
	 */
	class Rcver implements Runnable
	{
		private static final short FRESHRATE = 100; //refresh interval
		Rcver()
		{
		}
		//might include code specifically for grabbing/compressing acks
		
		public void run()
		{
			while(true)
			{
				while(!theRF.dataWaiting()){
					try{
						synchronized(this){wait(FRESHRATE);}
					}
					catch(InterruptedException e)
					{
						;
					}
				}
				//fix this when switch to array
				Packet temp = new Packet(theRF.receive());
				if(temp.getDestAddr() == ourMAC){
					//check if wanted packet
					switch(temp.getType()){
						case DATAC:
							
							{
								rQueue.offer(temp.pack);
							}

						//code for sequence number checking
						case ACK: //code for updating sliding window;
						case Beacon:;
						case CTS:;
						case RTS:;
					}
				}
			}
		}
	}
}
