package com.xcheng.wiredecr;

interface IComm {
    int getConnectTimeout();
    void setConnectTimeout(int timeout);
    int getSendTimeout();
    void setSendTimeout(int timeout);
    int getRecvTimeout();
    void setRecvTimeout(int timeout);
    void connect(String deviceName);
    void disconnect();
    void send(in byte[] payload);
    byte[] recv(int length);
    byte[] recvNonBlocking();
    byte[] recvNonBlock();
    void reset();
    void open();
    void close();
    void cancelRecv();
    int getConnectStatus();
}
