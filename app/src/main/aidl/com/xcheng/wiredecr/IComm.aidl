package com.xcheng.wiredecr;

interface IComm {
    int getConnectTimeout();
    void setConnectTimeout(int var1);

    int getSendTimeout();
    void setSendTimeout(int var1);

    int getRecvTimeout();
    void setRecvTimeout(int var1);

    void connect(String deviceName);
    int getConnectStatus();
    void disconnect();

    void send(in byte[] var1);
    byte[] recv(int var1);

    byte[] recvNonBlocking();
    byte[] recvNonBlock();

    void reset();
    void cancelRecv();
    void open();
    void close();
}