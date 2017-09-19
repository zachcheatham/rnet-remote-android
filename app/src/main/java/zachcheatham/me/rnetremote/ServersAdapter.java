package zachcheatham.me.rnetremote;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ViewHolder>
{
    private final List<RNetServer> servers = new ArrayList<>();
    private final ItemClickListener clickListener;

    ServersAdapter(ItemClickListener clickListener)
    {
        this.clickListener = clickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.item_server, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position)
    {
        RNetServer rNetServer = servers.get(position);
        holder.textView.setText(rNetServer.name);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                clickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return servers.size();
    }

    public void addServer(String name, InetAddress host, int port)
    {
        RNetServer rNetServer = new RNetServer();
        rNetServer.name = name;
        rNetServer.port = port;
        rNetServer.host = host;

        servers.add(rNetServer);

        if (getItemCount() > 1)
            notifyItemInserted(getItemCount());
        else
            notifyDataSetChanged();
    }

    public void removeServer(InetAddress host, int port)
    {
        for (int i = 0; i < servers.size(); i++)
        {
            RNetServer r = servers.get(i);
            if (r.host.equals(host) && r.port == port)
            {
                servers.remove(i);

                if (getItemCount() < 1)
                    notifyDataSetChanged();
                else
                    notifyItemRemoved(i);
            }
        }
    }

    void clearServers()
    {
        servers.clear();
        notifyDataSetChanged();
    }

    RNetServer getServer(int position)
    {
        return servers.get(position);
    }

    static class RNetServer
    {
        public String name;
        int port;
        InetAddress host;
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView textView;
        ImageView imageView;

        ViewHolder(View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.name);
            imageView = itemView.findViewById(R.id.icon);
        }
    }

    interface ItemClickListener
    {
        void onItemClick(int position);
    }
}
