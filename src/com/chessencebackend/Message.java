package com.chessencebackend;

import java.io.Serializable;

public class Message implements Serializable {
    private String message = "";
    private String typeOfMessage = "";
    private boolean isNewLobbyRequest = false;

    public Message(String message){
        this.message = message;
        this.typeOfMessage = "chat";
    }

    public Message(String message, String typeOfMessage){
        this.message = message;
        this.typeOfMessage = typeOfMessage; // -> "chat" or "lobbyInfo"
    }

    public Message(String message, String typeOfMessage, boolean isNewLobbyRequest){
        this.message = message;
        this.typeOfMessage = typeOfMessage; // -> "chat" or "lobbyInfo"
        this.isNewLobbyRequest = isNewLobbyRequest;
    }

    public String getMessage() {
        return message;
    }


    public String getTypeOfMessage() {
        return typeOfMessage;
    }

    public boolean isNewLobbyRequest() {
        return isNewLobbyRequest;
    }
}
