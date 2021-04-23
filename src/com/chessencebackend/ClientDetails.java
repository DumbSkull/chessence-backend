package com.chessencebackend;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientDetails {
    private String username;
    private ObjectOutputStream objectOutputStream;
    private Socket socket;
    private boolean isPlayer;

    ClientDetails(String username, ObjectOutputStream objectOutputStream, Socket socket, boolean isPlayer)
    {
        this.username = username.trim().replaceAll(",", "");
        this.objectOutputStream = objectOutputStream;
        this.socket = socket;
        this.isPlayer = isPlayer;
    }

    public String getUsername() {
        return username;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isPlayer() {
        return isPlayer;
    }

    public void setPlayer(boolean player) {
        isPlayer = player;
    }
}
