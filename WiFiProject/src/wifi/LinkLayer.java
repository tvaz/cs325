package wifi;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Queue;

import rf.RF;

/*
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author Cody Kagawa & Tori Vaz(richards)
 */
public class LinkLayer implements Dot11Interface {
	private RF theRF;           // RF layer
	private short ourMAC;       // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	/* constant for debug purposes, how often code checks for changes*/
	static final short FRESHRATE = 100; //refresh interval
	/* Constants for packet types*/
	private static final short DATAC = 0x00;
	private static final short ACK = 0x01;
	private static final short Beacon = 0x02;
	//private static final short CTS = 0x03;
	//private static final short RTS = 0x04;
	private static final int DIFS = RF.aSIFSTime*2;
	/**/

	Queue<byte[]> sWindow;//sliding window
	private Queue<byte[]> dQueue; //send data queue
	
	// size 3 array [last acked sqnc#,last sent sqnc#,sqnc# of last packet from this addr(for receives)]
	private HashMap<Short,Short[]> sequence; 
	
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
	//Current status
	private int status = 1;

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
		sWindow = (Queue<byte[]>) new LinkedList<byte[]>();
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
	//TODO: Finish implementing the incrementing of sequence numbers
	{
		if(sequence.get(addr)==null){
			sequence.put(addr, new Short[]{0,0,-1});
			return 0;
		}
		else{
			return sequence.get(addr)[1];
		}
	}

	//Print method that checks the debug level before deciding to print
	private void dPrint(String s){
		//TODO: add more debug print statements throughout the code
		if(debug != 0)output.println(s);
	}

	/*
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 *
	 */
	//actually prepares data for send and transfers reliability responsibility
	public int send(short dest, byte[] data, int len) {
		synchronized(dQueue){if(dQueue.size()+sWindow.size()>3)//make sure code isnt mid-update before calculating size
		{
			return 0;
		}}
		dQueue.offer(Packet.generatePacket(data,dest,ourMAC,DATAC,false,seqCheck(dest)));
		Short[] nSeq = sequence.get(dest);
		++nSeq[1];
		sequence.put(dest,nSeq);
		//fake confirm of data sent, true to actual except when unanticipated queue error occurs
		return data.length;

	}
	/*
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {	//TODO: Finish implementing the incrementing of sequence numbers
		//TODO: all this
		while(rQueue.isEmpty())
		{
			try{
				synchronized(this){wait(FRESHRATE);}
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
		//TODO: update status codes properly in error handling
		//dPrint("LinkLayer: Faking a status() return value of 0");
		return status;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		dPrint("LinkLayer: Sending command "+cmd+" with value "+val);
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
		//TODO remember to take thsi out later
		case 4:
			output.println("Current status: "+status());
			return 0;
		}
		return -1;
	}

	class FSM implements Runnable{
		State currentState;
		int sDelay;//current delay range for waiting to send;
		static final long TOPERIOD = 1000; //time out period
		long timeout; //time from which timeout is calculated
		HashMap<byte[],Integer> rWatch;

		public FSM(){
			currentState = State.IDLE;
			rWatch = new HashMap<byte[],Integer>();
			timeout = -1;
		}
		public void run(){
			//TODO needs update code
			//Run stuff
			while(true)
			{
				//timeout code
				if( timeout>=0 && theRF.clock()-timeout>TOPERIOD)
				{
					update();
				}
				//wait here if necessary
				switch(currentState)
				{
				case IDLE://sleep till something changes-- also base state, used when next action is ambiguous
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
						try{Thread.sleep(FRESHRATE);}
						catch(InterruptedException e)
						{
							//set status 2
						}
						timeout = -1;
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
		/* private helper, handles retry limit*/
				
		/* function to process refreshing sliding window, could use optimization time permitting*/
		void update(){
			synchronized(dQueue){// no adding to dqueue while this goes on, sWindow shouldn't matter if in this method
				//new send queue
				Queue<byte[]> next = new LinkedList<byte[]>();
				for(byte[] pack: sWindow)
				{
					Packet temp = new Packet(pack);// make old packet's data accessible
					if(rWatch.get(pack)>=RF.dot11RetryLimit)//too many retries
					{
						;// don't add to new sliding window
					}
					//resend if not at retry limit
					else{
						byte[] t = Packet.generatePacket(temp.getData(), temp.getDestAddr(), temp.getSqnc(), temp.getType(), true, temp.getSqnc());
						next.offer(t);
						rWatch.put(t, (rWatch.get(pack)+1));
					}
					rWatch.remove(pack);
				}
				//put all the ToSend stuff back
				next.addAll(dQueue);
				//replace the old queues
				dQueue = next;
				sWindow = new LinkedList<byte[]>();
				//reset timer
				timeout = theRF.clock();
			}
		}

		// busy channel helper
		void iCase()
		{
			//wait turn
			try{
				synchronized(this){//wait program refresh period; used to avoid busy wait when channel in use
					wait(FRESHRATE);
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
				try{//defer access, difs wait
					synchronized(this){
						wait(DIFS);
					}
				}
				catch(InterruptedException e)
				{

				}
				currentState = State.TRYSEND;//immediately try to send
			}
		}
		//not really necessary but makes commands easier to isolate
		//handles command 2 changes -- need to check timing requirements if need to wait sifs
		void nWait()
		{
			int next = sDelay;
			if (next == RF.aCWmin) return;//first try should be 1 persistent, no wait besides difs
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
			try{
				synchronized(this){wait(DIFS);}
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
					sDelay = RF.aCWmin;//reusing acwmin as flag for first time running, should cause 1 persistent immediate call for first attempt
					currentState = State.TRYSEND;
					break;
				case WANTSEND1://pass to second try
					currentState = State.WANTSEND2;
					break;
				default:
					currentState = State.IDLE;//should probably have error here, should never run this method from other states
				}
			}
		}
		//trysend helper
		void sCase(){
			nWait();
			if(theRF.transmit(dQueue.peek())== dQueue.peek().length){//if packet sends properly
				
				byte[] temp =dQueue.poll();
				sWindow.offer(temp);//add to sliding window
				rWatch.put(temp, 0);//start watching retries
				currentState = State.IDLE;//return to base state
			}
			else
			{
				if(theRF.inUse()){//exp backoff
					sDelay = RF.aCWmin;//throw away backoff, wait for next chance to send
					currentState = State.MUSTWAIT;
				}
				else{
					sDelay+=sDelay;//double collision window
					if(sDelay > RF.aCWmax)sDelay = RF.aCWmax;// cap at maximum collision window size
				}
			}

		}

		//update helper -- need to have code that causes update state check priorities.
		void uCase(){
			//TODO this
			while(!cQueue.isEmpty())//process the accummulated acks
			{
				Packet target = new Packet(cQueue.poll());//make ack data reachable
				for(byte[] slot : sWindow)//check what was acked
				{
					Packet p = new Packet(slot);//test if this was acked
					if(p.getDestAddr()==target.getSrcAddr()&&p.getSqnc()<=target.getSqnc())
					{
						dPrint(sWindow.remove(slot)+" on sWindow removal");//remove acked packet -- testing message
						Short[] nSeq = sequence.get(p.getDestAddr());
						if(nSeq[1] < p.getSqnc())
						{
							//error, got ack for seq greater than highest sent
						}
						else if (p.getSqnc()< nSeq[0]) // ack on confirmed packet
						{
							;//do nothing
						}
						else
						{
							nSeq[0] = p.getSqnc(); // update last confirmed sequence #
						}
						//update sequence info
						sequence.put(p.getDestAddr(),nSeq);
					}
				}
				synchronized(dQueue){//make sure dQueue isnt being edited while replacing
					sWindow.addAll(dQueue);
					dQueue = sWindow; //lazy way to avoid incrementing retry when unwarranted
				}
			}
			update();//reform sliding window -- in practice, throw away sliding window
			currentState = State.IDLE;
		}
	}
	/**
	 * back end recv stuff.
	 */
	class Rcver implements Runnable
	{
		/*offset for use with beacon, used for clock synchronization*/
		int offset;
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
				dPrint(temp.getDestAddr()+"");
				if(temp.getDestAddr() == ourMAC||temp.getDestAddr()==-1){
					//check if wanted packet
					dPrint(temp.getCRC()+"");
					dPrint(Packet.validate(temp)+"");
					dPrint(Packet.validate(temp)+"");
					if(!Packet.validate(temp))
					{
						//do nothing if crc bad?
					}
					else if(temp.getType()== Beacon)
					{

					}
					else if (rQueue.size()>3)
					{

					}
					else{
						switch(temp.getType()){
							case DATAC:
								{
									rQueue.offer(temp.pack);
								}

							//code for sequence number checking
							case ACK: dPrint("got an ack");
								cQueue.offer(temp.pack); //code for updating sliding window
						}
					}
				}
			}
		}
	}
}
