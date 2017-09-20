package zachcheatham.me.rnetremote.server;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RNetServer
{
    private final InetAddress address;
    private final int port;
    private final Listener listener;

    private SocketChannel channel;
    private boolean run;

    public RNetServer(InetAddress address, int port, Listener listener)
    {
        this.address = address;
        this.port = port;
        this.listener = listener;
    }

    public void run()
    {
        run = true;

        try
        {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(address, port));

            readChannel();

            try
            {
                channel.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            listener.connectError();
        }

        listener.disconnected(run);
    }

    private void readChannel()
    {
        int bytesRead;
        ByteBuffer buffer = ByteBuffer.allocateDirect(128);

        try
        {
            while (run)
            {
                bytesRead = channel.read(buffer);
                if (bytesRead == -1)
                {
                    channel.close();
                }
                else
                {
                    buffer.rewind();
                    buffer.limit(bytesRead);

                    Log.d("RNetProxy Server", buffer.toString());
                }
            }
        }
        catch (IOException e)
        {
            //e.printStackTrace();
        }
    }

    public void disconnect()
    {
        run = false;
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // TODO SEND

    public interface Listener
    {
        void connectError();
        void connected();
        void packetReceived();
        void disconnected(boolean unexpected);
    }
}
