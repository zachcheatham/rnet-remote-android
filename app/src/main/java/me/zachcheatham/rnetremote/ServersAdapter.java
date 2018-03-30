package me.zachcheatham.rnetremote;

import android.support.annotation.NonNull;
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.item_server, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        if (position == servers.size())
        {
            holder.textView.setText(R.string.action_enter_ip);
            holder.imageView.setImageResource(R.drawable.ic_keyboard_white_24dp);
        }
        else
        {
            RNetServer rNetServer = servers.get(position);
            holder.textView.setText(rNetServer.name);
            holder.imageView.setImageResource(R.drawable.ic_wifi_white_24dp);
        }
    }

    @Override
    public int getItemCount()
    {
        return servers.size() + 1;
    }

    void addServer(String name, InetAddress host, int port)
    {
        RNetServer rNetServer = new RNetServer();
        rNetServer.name = name;
        rNetServer.port = port;
        rNetServer.host = host;

        servers.add(rNetServer);

        if (getItemCount() > 2)
            notifyItemInserted(servers.size() - 1);
        else
            notifyDataSetChanged();
    }

    void removeServer(InetAddress host, int port)
    {
        for (int i = 0; i < servers.size(); i++)
        {
            RNetServer r = servers.get(i);
            if (r.host.equals(host) && r.port == port)
            {
                servers.remove(i);

                if (getItemCount() < 2)
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

    interface ItemClickListener
    {
        void onItemClick(int position);
    }

    static class RNetServer
    {
        public String name;
        int port;
        InetAddress host;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView textView;
        ImageView imageView;

        ViewHolder(View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.name);
            imageView = itemView.findViewById(R.id.icon);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            clickListener.onItemClick(getAdapterPosition());
        }
    }
}
