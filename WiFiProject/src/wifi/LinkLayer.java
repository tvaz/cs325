package wifi;
import java.io.PrintWriter;
import java.util.LinkedList;
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
	/* Constants for packet types*/
	private static final short DATAC = 0x00;
	private static final short ACK = 0x01;
	private static final short Beacon = 0x02;
	private static final short CTS = 0x03;
	private static final short RTS = 0x04;
	/**/

	private Queue<byte[]> dQueue; //send data queue
	private HashMap<Short,Short[]> sequence; //current sequence number for each destination

	//Enumerated states for internal FSM
	enum State{IDLE,WANTSEND1,WANTSEND2,TRYSEND,MUSTWAIT,TRYUPDATE}

	private Thread fState;
	private Thread watcher; //thread around queue, watch for incoming data
	private Queue<byte[]> rQueue; //receive data queue
	private Queue<byte[]> cQueue; //pass data for update to process

	/*State variables for link layer options*/
	private int debug = 1;
	private int slotMode = 0;
	private int beaconInterval = -1;


	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;
		theRF = new RF(output, null);
		dPrint("LinkLayer: Constructor ran.");
		dQueue = (Queue<byte[]>) new LinkedList<byte[]>();
		rQueue = (Queue<byte[]>) new LinkedList<byte[]>();
		cQueue = (Queue<byte[]>) new LinkedList<byte[]>();
		sequence = new HashMap<Short,Short[]>();
		//create background threads
		fState = new Thread(new FSM());
		watcher = new Thread(new Rcver());
		//start background threads
		watcher.start();
		fState.start();
	}
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

	//Print method that checks the debug level before deciding to print
	private void dPrint(String s){
		if(debug != 0)output.println(s);
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

		System.out.println(data.length);
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
		dPrint("LinkLayer: Pretending to block on recv()");
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
	// 1 - success - 2 - unspecified error - 3 - RF init fail - 4 - last transm acked - 5 - last transm discarded
	//  6 - bad buf. size - 7 - bad addr. - 8 - bad mac - 9 - illegal arg - 10 - insuf. buffer
	public int status() {
		dPrint("LinkLayer: Faking a status() return value of 0");
		return 0;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		dPrint("LinkLayer: Sending command "+cmd+" with value "+val);
		//TODO: Implement command codes 2 and 3
		//TODO: Make those values actually do something
		/*
		 * Command 0: Options and settings
		 * Should summarize all command options and report their current settings. The accompanying value parameter is ignored.
		 *
		 * Command 1: Debug level.
		 * The meaning of non-zero values can be implementation dependent, but passing a value of 0 should disable all debugging output.
		 *
		 * Command 2: Slot selection.
		 * If the accompanying value parameter is 0 the link layer should select slots randomly. Any other value should cause the link layer to always select maxCW.
		 *
		 * Command 3: Beacon interval.
		 * The accompanying value specifies the desired number of seconds between the start of beacon transmissions. A value of -1 should disable the sending of beacon frames.
		 */
		switch(cmd){
		case 0:
			//Display all options
			output.println("");
			output.println("====Command Settings====");
			output.println("##Cmd 0: Display command options and current settings.");
			output.println("Cmd 1: Set debug level. 0 for no debug output, 1 for full.");
			if(debug == 0){
				output.println("Current debug level: 0 (disabled)");
			}
			if(debug == 1){
				output.println("Current debug level: 1 (enabled)");
			}
			output.println("##Cmd 2: Set slot selection mode. 0 for random, anything else to use MaxCW.");
			if(slotMode == 0){
				output.println("Current mode: 0 (random).");
			} else{
			output.println("Current mode: using maxCW.");
			}
			output.println("##Cmd 3: Set beacon interval, in seconds. -1 to disable beacons.");
			if(beaconInterval == -1){
				output.println("Current interval: -1 (disabled).");
			} else{
			output.println("Current interval: "+beaconInterval);
			}
			output.println("========================");
			output.println("");
			return 0;
		case 1:
			//Set debug level
			switch(val){
			case 0:
				debug = 0;
				output.println("Debug output disabled.");
				return 0;
			case 1:
				debug = 1;
				output.println("Debug output enabled.");
				return 0;
			default:
				output.println("Invalid; Try 0 or 1.");
				return -1;
			}
		case 2:
			//If 0, switch to Random slot selection mode
			//Any other value, switch to always select maxCW
			switch(val){
			case 0:
				slotMode = 0;
				output.println("Using random slot selection mode.");
				return 0;
			default:
				slotMode = 1;
				output.println("Using maxCW slot selection mode.");
				return 0;
			}
		case 3:
			//-1 Disable beacon frames
			//Otherwise, val will specify # of seconds
			//between start of beacons
			switch(val){
			case -1:
				beaconInterval = -1;
				output.println("Beacons disabled.");
				return 0;
			case 0:
				output.println("Proabably shouldn't set interval to 0 seconds.");
				return -1;
			default:
				beaconInterval = val;
				output.println("Beacon interval set to "+val+" seconds.");
				return 0;
			}
		}
		return -1;
	}

	class FSM implements Runnable{
		State currentState;
		int sDelay;//current delay range for waiting to send;
		Queue<byte[]> sWindow;//sliding window

		public FSM(){
			currentState = State.IDLE;
			sWindow = (Queue<byte[]>) new LinkedList<byte[]>();
		}
		public void run(){
			//Run stuff
			while(true)
			{
				//wait here if necessary
				switch(currentState)
				{
				case IDLE://sleep till something changes
					if(!cQueue.isEmpty())
					{
						currentState = State.TRYUPDATE;

					}
					else if(!dQueue.isEmpty())
					{
						currentState = State.WANTSEND1;
					}
					else{
						//can change time if decide to use notifies;
						try{Thread.sleep(100);}
						catch(InterruptedException e)
						{
							//set status 2
						}
						break;
					}
				case WANTSEND1:
					wCase();
					break;
				case WANTSEND2:
					wCase();
					break;
				case TRYSEND://wait for update or new data
					sCase();
					break;
				case MUSTWAIT://channel busy
					iCase();
					break;
				case TRYUPDATE://wait for update
					uCase();
					break;
				}
			}
		}
		// busy channel helper
		void iCase()
		{
			//wait turn
			try{
				synchronized(this){
					wait(RF.aSlotTime);
				}
			}
			catch(InterruptedException e)
			{

			}
			if(theRF.inUse())
			{

			}
			else
			{
				currentState = State.WANTSEND1;
			}
		}
		//not really necessary but makes commands easier to isolate
		//handles command 2 changes -- need to check timing requirements if need to wait sifs
		void nWait()
		{
			int next = sDelay;
			if(slotMode == 0){
				next = (int)(Math.random()*next);
			}
			try{
				synchronized(this){
					wait(next);
				}
			}
			catch(InterruptedException e)
			{

			}
		}
		//idle channel helper
		void wCase(){
			//-10 to remove header overhead
			try{
				synchronized(this){wait(RF.aSIFSTime);}
			}

			catch(InterruptedException e)
			{

			}
			if(theRF.inUse())
			{
				currentState = State.MUSTWAIT;
				return;
			}
			else{
				switch(currentState){
				case WANTSEND2://actually try to send
					sDelay = RF.aSlotTime;
					currentState = State.TRYSEND;
					break;
				case WANTSEND1://pass to second try
					currentState = State.WANTSEND2;
					break;
				default://should probably have error here, should never run this method from other states
				}
			}
		}
		//trysend helper
		void sCase(){
			nWait();
			if(theRF.transmit(dQueue.peek())== dQueue.peek().length){//if packet sends properly
				sWindow.offer(dQueue.poll());
				currentState = State.IDLE;
			}
			else
			{
				if(theRF.inUse()){//exp backoff
					sDelay = RF.aSlotTime;
					currentState = State.MUSTWAIT;
				}
				else{
					sDelay+=sDelay;
				}
			}

		}

		//update helper -- need to have code that causes update state check priorities.
		void uCase(){
			for(byte[] pack: rQueue)
			{
				Packet target = new Packet(pack);
				for(byte[] slot : sWindow)
				{
					Packet p = new Packet(slot);
					if(p.getDestAddr()==target.getSrcAddr()&&p.getSqnc()<=target.getSqnc())
					{
						sWindow.remove(p);
					}
				}
			}
			if(false){//need checks to see which state to return to
				currentState=State.TRYSEND;
			}
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
