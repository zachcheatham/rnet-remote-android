package me.zachcheatham.rnetremote.rnet.packet;

import me.zachcheatham.rnetremote.rnet.RNetServer;

public class PacketC2SProperty extends RNetPacket
{
    private static final byte ID = 0x02;

    public PacketC2SProperty(int property, Object value)
    {
        super();

        writeUnsignedByte(property);
        switch (property)
        {
        case RNetServer.PROPERTY_NAME:
            writeNTString((String) value);
            break;
        case RNetServer.PROPERTY_WEB_SERVER_ENABLED:
            writeUnsignedByte(((boolean) value) ? 0x01 : 0x00);
            break;
        default:
            throw new IllegalArgumentException(String.format("Unsupported C2S property: %d", property));
        }
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData(){}
}
