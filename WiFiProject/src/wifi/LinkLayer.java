package wifi;
import java.io.PrintWriter;
import java.util.Arrays;
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
	static final short FRESHRATE = 10;
	/* Constants for packet types*/
	private static final short DATAC = 0x00;
	private static final short ACK = 0x01;
	private static final short Beacon = 0x02;
	//unimplemented
	//private static final short CTS = 0x03;
	//private static final short RTS = 0x04;
	private static final int DIFS = RF.aSIFSTime*2;//timing constant
	private static int BUFFERSIZE = 4;//buffer limit
	Long cOffset; //clock offset, for beacons
	/**/

	Queue<byte[]> sWindow;//sliding window
	private Queue<byte[]> dQueue; //send data queue

	// size 3 array [last acked sqnc#,last sent sqnc#,sqnc# of last packet from this addr(for sequence checking receives--unused)]
	private HashMap<Short,Short[]> sequence;

	//Enumerated states for internal FSM
	enum State{IDLE,WANTSEND1,WANTSEND2,TRYSEND,MUSTWAIT,TRYUPDATE}
	
	private FSM fsm;
	private Thread fState;
	private Thread watcher; //thread around queue, watch for incoming data
	private Queue<byte[]> rQueue; //receive data queue
	private Queue<byte[]> cQueue; //pass data for update to process

	/*State variables for link layer options*/
	private int debug = 1;
	private int slotMode = 0;
	private int beaconInterval = -1;
	//Current status
	private int status;

	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		//instantiate variables
		status = 1;
		this.ourMAC = ourMAC;
		this.output = output;
		theRF = new RF(output, null);
		if(theRF==null)status = 3;
		cOffset = (long)0;
		sWindow = (Queue<byte[]>) new LinkedList<byte[]>();
		dQueue = (Queue<byte[]>) new LinkedList<byte[]>();
		rQueue = (Queue<byte[]>) new LinkedList<byte[]>();
		cQueue = (Queue<byte[]>) new LinkedList<byte[]>();
		sequence = new HashMap<Short,Short[]>();
		//create background threads
		fsm = new FSM();
		fState = new Thread(fsm);
		watcher = new Thread(new Rcver());
		//start background threads
		watcher.start();
		fState.start();
		dPrint("LinkLayer: Constructor ran.");
	}
	//for clock synch, beacons
	private long clock(){
		long ret =theRF.clock() + cOffset;
		return ret;
	}

	//returns next sequence number for the given destination address -- maybe unnecessary
	private short seqCheck(short addr)
	//TODO: Finish implementing the incrementing of sequence numbers
	//does not reuse sequence numbers that hit retry limit, and does not roll over
	{
		if(sequence.get(addr)==null){
			sequence.put(addr, new Short[]{0,0,-1});
			dPrint("Sequence info for: "+addr+'\t'+ sequence.get(addr)[1]);
			return 0;
		}
		else{
			Short[] n = sequence.get(addr);
			short ret = n[1];
			n[1] = new Short(n[1]++);
			sequence.put(addr, n);
			dPrint("Sequence info for: "+addr+'\t'+ sequence.get(addr)[1]);
			return ret;
		}
	}

	//Print method that checks the debug level before deciding to print
	private void dPrint(String s){
		if(debug != 0)output.println(s);
	}

	/*
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 *
	 */
	//actually prepares data for send and transfers reliability responsibility
	public int send(short dest, byte[] source, int len) {
		int leng = len;
		if(len<0){
			status = 6;
			return 0;
		}
		else if(source == null){
			status = 7;
			return 0;
		}
		else if (dest<-1 || dest>0xFFF)
		{
			status = 8;
			return 0;
		}
		if (leng > source.length)//cutoff if len longer than actual data
		{
			leng = source.length;
		}
		if(leng > 2038)//cutoff if data > packet max;
		{
			leng  = 2038;
		}
		byte[] data = Arrays.copyOfRange(source, 0, leng);
		synchronized(dQueue){if(dQueue.size()+sWindow.size()>=BUFFERSIZE)//make sure code isnt mid-update before calculating size
		{
			status = 10;
			return 0;
		}}
		Packet p = new Packet(Packet.generatePacket(data,dest,ourMAC,DATAC,false,seqCheck(dest)));
		Packet.printDebug(p,1);
		dPrint("Queued up packet of size " + data.length + " to "+p.getDestAddr());
		dQueue.offer(p.pack);/*
		Short[] nSeq = sequence.get(dest);
		nSeq[1]=p.getSqnc();
		sequence.put(dest,nSeq);*/
		//fake confirm of data sent, true to actual except when unanticipated queue error occurs
		return data.length;

	}
	/*
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
		//TODO: all this
		while(rQueue.isEmpty())
		{
			try{
				synchronized(this){wait(FRESHRATE);}
			}
			catch(InterruptedException e)
			{
				status = 2;
			}
		}
		Packet temp = new Packet(rQueue.poll());
		dPrint("Receiving packet " + temp.getSqnc() + " from " + temp.getSrcAddr());
		t.setBuf(temp.getData());
		t.setDestAddr(temp.getDestAddr());
		t.setSourceAddr(temp.getSrcAddr());
		status = 1;
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
		static final long TOPERIOD = 3000; //time out period
		long timeout; //time from which timeout is calculated
		HashMap<byte[],Integer> rWatch;

		public FSM(){
			currentState = State.IDLE;
			rWatch = new HashMap<byte[],Integer>();
			timeout = clock();
		}
		public void run(){
			//TODO needs update code
			//Run stuff
			while(true)
			{
				//timeout code
				if((clock()-timeout)>TOPERIOD)
				{
					dPrint("\nTimeout has occured\n");
					update();
				}
				//wait here if necessary
				synchronized(this){
				switch(currentState)
				{
				case IDLE://sleep till something changes-- also base state, used when next action is ambiguous
					if(!cQueue.isEmpty())
					{
						dPrint("Exiting Idle State");
						currentState = State.TRYUPDATE;
					}
					else if(!dQueue.isEmpty())
					{
						dPrint("Exiting Idle State");
						currentState = State.WANTSEND1;
					}
					else{
						//can change time if decide to use notifies;
						try{Thread.sleep(FRESHRATE);}
						catch(InterruptedException e)
						{
							status = 2;
						}
						timeout = clock();
					}
					break;
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
					dPrint("Retry: "+ rWatch.get(pack) +'/'+ RF.dot11RetryLimit);
					if(rWatch.get(pack)>=RF.dot11RetryLimit)//too many retries
					{
						dPrint("Packet to "+ temp.getDestAddr() +" has reached retransission limit -- discarding packet");
						status = 5;
						rWatch.remove(pack);// don't add to new sliding window
					}
					//resend if not at retry limit
					else{
						dPrint("Resending packet "+ temp.getSqnc() +" to "+temp.getDestAddr());
						byte[] t = Packet.generatePacket(temp.getData(), temp.getDestAddr(), ourMAC, temp.getType(), true, temp.getSqnc());
						Packet p = new Packet(t);
						Packet.printDebug(p,2);
						next.offer(t);
						rWatch.put(t, (rWatch.get(pack)+1));
						dPrint((rWatch.get(t))+"");
					}
				}
				//put all the ToSend stuff back
				next.addAll(dQueue);
				//replace the old queues
				dQueue.clear();
				dQueue.addAll(next);
				sWindow.clear();;
				//reset timer
				timeout = clock();
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
				status = 2;
			}
			if(theRF.inUse())
			{

			}
			else
			{
				try{//defer access, difs wait
					synchronized(this){
						wait(DIFS+(50-(clock()%50)));
					}
				}
				catch(InterruptedException e)
				{
					status = 2;
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
			if(slotMode == 0){//if set to slot mode 0, use random slot interval instead of forced max backoff
				next = (int)(Math.random()*next);
			}
			try{
				synchronized(this){
					wait(next+(50 -(clock()%50)));
				}
			}
			catch(InterruptedException e)
			{
				status = 2;
			}
		}
		//idle channel helper
		void wCase(){
			try{
				synchronized(this){wait(DIFS+(50-(clock()%50)));}
			}

			catch(InterruptedException e)
			{
				status = 2;
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
			dPrint("Beginning a send attempt");
			nWait();
			//debug line, stopgap for null pointer that we ran out of time to fix
			if(dQueue.isEmpty()){
				currentState=State.IDLE;
				return;
			}
			if(theRF.transmit(dQueue.peek())== dQueue.peek().length){//if packet sends properly
				dPrint("Sent a Packet");
				currentState = State.IDLE;//return to base state
				if(new Packet(dQueue.peek()).getDestAddr() == -1) return;//don't process retransmit on bcast since others shouldnt be acking bcast packets
				
				byte[] temp = dQueue.poll();
				dPrint("Added to sWindow: " +sWindow.offer(temp));//add to sliding window
				int rTry = 0;
				if(rWatch.containsKey(temp))rTry = rWatch.get(temp);
				rWatch.put(temp, rTry);//start watching retries
			}
			else
			{
				if(theRF.inUse()){//exp backoff
					sDelay = RF.aCWmin;//throw away backoff, wait for next chance to send
					currentState = State.MUSTWAIT;
				}
				else{
					sDelay+=sDelay;//double collision window
					dPrint("Encountered collision, updating collision window");
					if(sDelay > RF.aCWmax)sDelay = RF.aCWmax;// cap at maximum collision window size
				}
			}

		}

		//update helper -- need to have code that causes update state check priorities.
		void uCase(){
			//TODO this
			dPrint("Beginning an Update");
			synchronized(this){//make sure dQueue isnt being edited while replacing
			sWindow.addAll(dQueue);
			while(!cQueue.isEmpty())//process the accummulated acks
			{
				
				Packet target = new Packet(cQueue.poll());//make ack data reachable
				dPrint("Processing ack for packet "+ target.getSqnc()+ " to " + target.getDestAddr());
				for(byte[] slot : sWindow)//check what was acked
				{
					Packet p = new Packet(slot);//test if this was acked
					if(p.getDestAddr()==target.getSrcAddr())
					{
						Short[] nSeq = sequence.get(p.getDestAddr());
						if(nSeq[1] < target.getSqnc())
						{
							dPrint("ACK for sequence above highest sent");
							//error, got ack for seq greater than highest sent
						}
						else if (target.getSqnc()< nSeq[0]) // ack on confirmed packet
						{
							dPrint("Received an old ack");//do nothing
						}
						else
						{
							nSeq[0] = p.getSqnc(); // update last confirmed sequence #
						}
						//update sequence info
						sequence.put(p.getDestAddr(),nSeq);
						status = 4;
						dPrint("Removing ACKed packet " + p.getSqnc() + " to " + p.getDestAddr()+ " from sliding window");
						dPrint(sWindow.remove(slot)+" on removal from sWindow");//remove acked packet -- testing message
						}
					}
				}
				
					dQueue= new LinkedList<byte[]>();
					dQueue.addAll(sWindow); //lazy way to avoid incrementing retry when unwarranted
					sWindow.clear();
					//dPrint("Refreshing sliding window with " + dQueue.size() + " remaining transmissions");
					update();//reform sliding window -- in practice, throw away sliding window
				
			}
			dPrint("Finished an Update");
		}
		State force(){
			State ret = currentState;
			uCase();
			return ret;
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
						status = 2;
					}
				}
				//fix this when switch to array
				Packet temp = new Packet(theRF.receive());
				if(temp.getDestAddr() == ourMAC||temp.getDestAddr()==-1){
					//check if wanted packet
					if(!Packet.validate(temp))
					{
						//might be bugged, this gets called a lot on bcast packets, not sure which types
						dPrint("Got a bad packet");//do nothing if crc bad?
					}
					else if(temp.getType()== Beacon)
					{
						//beacon info doesn't really matter, old beacon should be slower anyway
						long oClock = clock();
						long nClock = Packet.beaconCheck(temp.pack);
						synchronized(cOffset){
						if(nClock>oClock)
							{
								cOffset += nClock - oClock;
							}
						}
						dPrint("Finished beacon synchronization, current clock is: " + clock());
					}
					else if (rQueue.size()>=BUFFERSIZE)
					{

					}
					else{
						switch(temp.getType()){
							case DATAC:
								{
									rQueue.offer(temp.pack);
									if(temp.getDestAddr()==-1)break;//don't ack broadcast packets
									else{
									try{synchronized(this){wait(RF.aSIFSTime);}}
									catch(InterruptedException e)
									{
										status = 2;
									}
									//ack -- no data, reverse destination, change packet type,acks don't retry, copy sequence #
									Packet p = new Packet(Packet.generatePacket(new byte[]{}, temp.getSrcAddr(), ourMAC, ACK, false, temp.getSqnc()));
									dPrint("Sending ACK to " + p.getDestAddr() + " for packet "+ p.getSqnc());
									Packet.printDebug(p,3);
									theRF.transmit(p.pack);
									}
									break;
								}

							//code for sequence number checking
							case ACK: 
								if(temp.getDestAddr()==-1){
									break;//dont check broadcast acks -- not sure why these exist but was getting some in testing
								}
								
								dPrint("Received an ACK for packet " + temp.getSqnc() + " to " + temp.getSrcAddr());
								cQueue.offer(temp.pack); //code for updating sliding window
								synchronized(fsm){fsm.force();}//put fsm into udpate mode
						}
					}
				}
			}
		}
	}
}
