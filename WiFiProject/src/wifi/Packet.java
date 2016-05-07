package wifi;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

class Packet {
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
		//Control, then Dest Addr, then Src Addr, the Data, and Checksum
		// [2][2][2][0-2038][4] Bytes
		// Control : Frame Type, Retry, Sequence [3][1][12] Bits
		CRC32 check = new CRC32();
		int size = src.length+10;

		byte[] packet = new byte[size];

		// get control from type
		byte control = (byte) (type << 5);
		if(retry) control |= 8;
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

		System.out.println("Computed crc: "+packet[packet.length-4]+" "+packet[packet.length-3]+" "+packet[packet.length-2]+" "+packet[packet.length-1]);



		//checksum goes here
	    System.out.println("" + packet[0] + '\t' +packet[1] + '\t'+packet[2] + '\t' + packet[3]+ '\t'+packet[4] + '\t' + packet[5] );
	    return packet;
	}

	public short getSqnc()
	{
		//return sequence #
		short t=(short)(pack[0] & 0x0f);
		return (short)((t<<8) + pack[1]);
	}
	public boolean getRetry()
	{
		return (8 & pack[0])>0;
	}
	public short getType()
	{
		//frame type
		return (short)(pack[0]>>5);
	}
	public short getDestAddr()
	{
	    short adr = (short)( ((pack[2]&0xFF)<<8) | (pack[3]&0xFF) );
		return adr;

	}
	public short getSrcAddr()
	{
		//return source MAC
		short t=pack[4];
		return (short)((t<<8) + pack[5]);
	}
	public byte[] getData()
	{
		return Arrays.copyOfRange(pack,6,pack.length-4);
	}
	public long getCRC()
	{
		System.out.println("Getting CRC.");
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
	    byte[] crc = new byte[4];
	    byte[] check = Arrays.copyOfRange(pack,pack.length-4,pack.length-1);
	    System.out.println("Printing CRC grabbed:");
	    for(int i=0;i<check.length;i++){
		System.out.print(check[i]+" ");
	    }
	    System.out.println("Done printing CRC.");
		return 0 ;
	}
	public static boolean validate(Packet p)//compare wrapper packet against created packet for same crc
	{
		CRC32 c = new CRC32();
		c.update(p.pack, 0, p.pack.length - 4);
		return p.getCRC() == c.getValue();
	}
}
