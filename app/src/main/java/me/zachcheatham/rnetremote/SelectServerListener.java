package me.zachcheatham.rnetremote;

import java.net.InetAddress;

interface SelectServerListener
{
    void serverSelected(String name, InetAddress address, int port);
}
