package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class RNetPacket
{
    final ByteBuffer buffer;

    RNetPacket()
    {
        buffer = ByteBuffer.allocate(255);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        writeUnsignedByte(getPacketID());
    }

    RNetPacket(ByteBuffer buffer)
    {
        this.buffer = buffer;
        parseData();
    }

    abstract byte getPacketID();

    abstract void parseData();

    public byte[] getData()
    {
        byte[] data = new byte[this.buffer.position() + 1];
        buffer.rewind();
        buffer.get(data);

        System.arraycopy(data, 1, data, 2, data.length - 2);
        data[1] = (byte) (data.length - 2);

        return data;
    }

    void writeUnsignedByte(int i)
    {
        buffer.put((byte) (i & 0xff));
    }

    void writeUnsignedShort(int i)
    {
        buffer.putShort((short) (i & 0xffff));
    }

    int readUnsignedByte()
    {
        return (buffer.get() & 0xff);
    }

    void writeNTString(String s)
    {

        buffer.put(s.getBytes());
        buffer.put((byte) 0x00);
    }

    String readNTString()
    {
        byte[] remaining = new byte[this.buffer.remaining()];
        buffer.slice().get(remaining);

        int nullPosition = -1;
        for (int i = 0; i < remaining.length; i++)
            if (remaining[i] == 0x00)
                nullPosition = i;

        if (nullPosition == -1)
            return null;

        byte[] bytes = new byte[nullPosition];
        System.arraycopy(remaining, 0, bytes, 0, nullPosition);
        buffer.position(buffer.position() + nullPosition + 1);

        return new String(bytes);
    }
}
