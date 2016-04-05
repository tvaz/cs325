package wifi;

import java.util.Arrays;

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
	**/
	public static byte[] generatePacket(byte[] src, short dest, short home, short type, boolean retry, short seq)
	{
		//Control, then Dest Addr, then Src Addr, the Data, and Checksum
		// [2][2][2][0-2038][4] Bytes
		// Control : Frame Type, Retry, Sequence [3][1][12] Bits
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
	    //checksum goes here
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
		//return dest MAC
		short t=pack[2];
		return (short)((t<<8) + pack[3]);
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
}
