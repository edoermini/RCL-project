package com.worth.chat;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Vector;

public class Chat implements Runnable {
    private final String chatIp;
    private final Vector<String> messages;
    private final int port = 6662;
    private MulticastSocket ms;
    private Boolean deleted = false;

    public Chat(String chatIp) {
        this.chatIp = chatIp;
        this.messages = new Vector<>();

        try {
            ms = new MulticastSocket(port);
            ms.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        InetSocketAddress dategroup = new InetSocketAddress(chatIp, port);

        try {
            this.ms.joinGroup(dategroup, NetworkInterface.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            this.ms.setSoTimeout(5000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        byte[] buf;

        try {
            while(!deleted) {
                buf = new byte[1024*1024];

                DatagramPacket dp = new DatagramPacket(buf, buf.length);

                try {
                    this.ms.receive(dp);
                } catch (SocketTimeoutException e) {
                    continue;
                }

                String s = new String(dp.getData()).trim();
                messages.add(s);
            }
        } catch (IOException e) {
            System.out.println("reader");
            ms.close();
        }
    }

    public void stop() {
        this.deleted = true;
    }

    public Boolean isDeleted() {
        return this.deleted;
    }

    public List<String> getMessages() {
        List<String> retVal;

        synchronized (this.messages) {
            retVal = new Vector<>(this.messages);
            this.messages.clear();
        }

        return retVal;
    }

    public void sendChatMsg(String fromUser, String message) {

        InetAddress ia = null;

        try {
            ia = InetAddress.getByName(this.chatIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        byte[] msg = ("["+fromUser+"]: " + message).getBytes();
        DatagramPacket dp = new DatagramPacket(msg, msg.length, ia, 6662);

        try {
            this.ms.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
