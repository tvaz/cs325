package wifi;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

class Packet {// adding this here so git will let me commit
	byte[] pack;

	public Packet(byte[] src) {
		pack = src;// TODO Auto-generated constructor stub
	}
	/**
	* generates the packet
	* @arg src : network layer packet
	* @arg dest : mac address to send to
	* @arg home: mac adress source
	* @arg type : 3 bits at front to show type of packet
	* @arg retry : retry bit
	* @arg seq: sequence number
	* @return : the finished packet
	**/
	public static byte[] generatePacket(byte[] src, short dest, short home, short type, boolean retry, short seq)
	{
		System.out.println("Making a Packet with destination: " + dest + " sequence number: " + seq);
		//Control, then Dest Addr, then Src Addr, the Data, and Checksum
		// [2][2][2][0-2038][4] Bytes
		// Control : Frame Type, Retry, Sequence [3][1][12] Bits
		CRC32 check = new CRC32();
		int size = src.length+10;

		byte[] packet = new byte[size];

		// get control from type
		byte control = (byte) (type << 5);
		if(retry) control |= 16;
		control |= (byte) ((seq<<4)>>12);
		packet[0] = control;
		packet[1] = (byte)(seq & 0xff);
		packet[2] = (byte) (dest >> 8);
		packet[3] = (byte) dest;
		packet[4] = (byte) (home >> 8);

	  	packet[5] = (byte) home;
	    for(int i=0;i<src.length;i++){
	        packet[i+6] = src[i];
	    }

	    check.update(packet,0,packet.length-4);
	    packet[packet.length-4] = (byte)(check.getValue()>>24);
	    packet[packet.length-3] = (byte)(check.getValue()>>16);
	    packet[packet.length-2] = (byte)(check.getValue()>>8);
	    packet[packet.length-1] = (byte)(check.getValue());

		System.out.println("Computed CRC: "+convCrcToLong(packet));
		System.out.println("Generated a packet:");

		//checksum goes here
	    //System.out.println("" + packet[0] + '\t' +packet[1] + '\t'+packet[2] + '\t' + packet[3]+ '\t'+packet[4] + '\t' + packet[5] );
	    return packet;
	}

	public short getSqnc()
	{
		//return sequence #
		byte t = (byte)(pack[0]&0x0f);
		ByteBuffer buf = ByteBuffer.allocate(Short.BYTES);
	    byte[] check = new byte[]{t,pack[1]};
	    buf.put(check);
	      buf.flip();
	      short crc_long = buf.getShort();
		return crc_long;
	}
	public boolean getRetry()
	{
		boolean retry = (16 & pack[0])>0;
		System.out.println("retry: "+retry);
		return retry;
	}
	public short getType()
	{
		//frame type
		short t = (short)(pack[0]>>5);
		System.out.println("type: "+t);
		return t;
	}
	public short getDestAddr()
	{
	    short adr = (short)( ((pack[2]&0xFF)<<8) | (pack[3]&0xFF) );
		System.out.println("destaddr: "+adr);
		return adr;

	}
	public short getSrcAddr()
	{
	    short adr = (short)( ((pack[4]&0xFF)<<8) | (pack[5]&0xFF) );
		System.out.println("hostaddr: "+adr);
		return adr;
	}
	public byte[] getData()
	{
		byte[] ary = Arrays.copyOfRange(pack,6,pack.length-4);
		System.out.println("data: "+ary);
		return ary;
	}
	public long getCRC()
	{
		long crc = convCrcToLong(pack);
	     System.out.println("Grabbed CRC:"+crc);
	     return crc;
	}
	public static long convCrcToLong(byte[] packet){
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
	    byte[] check = Arrays.copyOfRange(packet,packet.length-4,packet.length);
	    buf.put(check);
	      buf.flip();
	      long crc_long = buf.getInt();
		return crc_long;
	}
	public static long beaconCheck(byte[] packet){
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
	    byte[] check = Arrays.copyOfRange(packet,6,packet.length-4);
	    buf.put(check);
	      buf.flip();
	      long crc_long = buf.getLong();
		return crc_long;
	}
	public static boolean validate(Packet p)//compare wrapper packet against created packet for same crc
	{

		return p.getCRC() == new Packet(Packet.generatePacket(p.getData(), p.getDestAddr(), p.getSrcAddr(), p.getType(), p.getRetry(), p.getSqnc())).getCRC();
	}
	public static void printDebug(Packet p, int i) {
		System.out.println("");
		System.out.println("PRINTING PACKET DEBUG INFO");
		switch(i){
		case 1:
			System.out.println("MAKING A PACKET!");
			break;
		case 2:
			System.out.println("RETRANSMITTING A PACKET!");
			break;
		case 3:
			System.out.println("SENDING AN ACK!");
		}
		System.out.println("+++");
		p.getSrcAddr();
		p.getDestAddr();
		p.getData();
		p.getType();
		p.getCRC();
		p.getSqnc();
		p.getRetry();
		System.out.println("COMPLETE");
		System.out.println("");


	}
}
