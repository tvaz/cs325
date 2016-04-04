package wifi;

class Packet {

	public Packet(byte[] src) {
		// TODO Auto-generated constructor stub
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
		int size = src.length()+10;

		byte[] packet = byte[size];

		// get control from type
		byte control = (byte) (type << 5);
		byte retryBit;
		if(retry) control = control+8;
		else control = control&;


	}

	public short getSqnc()
	{
		//return sequence #
		return 0;
	}
	public boolean getRetry()
	{
		//retry bit
		return false;
	}
	public short getType()
	{
		//frame type
		return 0;
	}
	public short getDestAddr()
	{
		return 0;
	}
	public short getSrcAddr()
	{
		return 0;
	}
	public byte[] getData()
	{
		return null;
	}
}
