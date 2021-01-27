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
    private Boolean stopped = false;

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

    /**
     * This code is run by a thread.
     * Reads new incoming messages from the multicast group
     * and saves them in a list for an asynchronous read.
     */
    @Override
    public void run() {
        InetSocketAddress dategroup = new InetSocketAddress(chatIp, port);

        // joining multicast group for receiving messages
        try {
            this.ms.joinGroup(dategroup, NetworkInterface.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // the socket timeout is used to check if this thread should be stopped
        try {
            this.ms.setSoTimeout(5000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        byte[] buf;

        try {
            while(!stopped) {
                buf = new byte[1024*1024];

                DatagramPacket dp = new DatagramPacket(buf, buf.length);

                try {
                    // receiving message
                    this.ms.receive(dp);
                } catch (SocketTimeoutException e) {
                    // checking if stopped
                    continue;
                }

                String s = new String(dp.getData()).trim();

                // saving received message
                messages.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (ms != null) {
                try {
                    ms.leaveGroup(dategroup, NetworkInterface.getByName("127.0.0.1"));
                    ms.close();
                } catch (IOException e) {
                 e.printStackTrace();
                }
            }
        }
    }

    /**
     * Stops running messages reader thread
     */
    public void stop() {
        this.stopped = true;
    }

    /**
     * Returns true if messages reader thread is stopped, false otherwise
     * @return true if messages reader thread is stopped, false otherwise
     */
    public Boolean isDeleted() {
        return this.stopped;
    }

    /**
     * Returns the messages received and not read from last reading
     * @return the messages received and not read from last reading
     */
    public List<String> getMessages() {
        List<String> retVal;


        synchronized (this.messages) {
            retVal = new Vector<>(this.messages);
            this.messages.clear();
        }

        return retVal;
    }

    /**
     * Sends a message in multicast group
     * @param fromUser user that sends teh message
     * @param message the message to send
     */
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
