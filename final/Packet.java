
class Packet {
	
	public Packet(byte[] src) {
		// TODO Auto-generated constructor stub
	}
	public static byte[] generatePacket(byte[] src, short dest, short home, short type, boolean retry, short seq)
	{
		return null;
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
