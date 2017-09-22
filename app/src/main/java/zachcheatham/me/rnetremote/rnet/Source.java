package zachcheatham.me.rnetremote.rnet;

public class Source
{
    private final int sourceId;
    private final RNetServer server;
    private String name;

    public Source(int sourceId, RNetServer server)
    {
        this.sourceId = sourceId;
        this.server = server;
    }

    public void setName(String name, boolean setRemotely)
    {
        this.name = name;

        if (!setRemotely)
        {
            // TODO Send rename packet
        }
    }

    public String getName()
    {
        return name;
    }
}
