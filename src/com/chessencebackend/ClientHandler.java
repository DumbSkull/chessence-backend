package com.chessencebackend;

import com.chessence.Message;
import com.chessence.Move;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

public class ClientHandler extends Thread {
    private ObjectOutputStream objectOutputStream = null;
    private ObjectInputStream objectInputStream = null;
    final Socket clientSocket;

    // Constructor
    public ClientHandler(Socket clientSocket
            , ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream) {
        this.clientSocket = clientSocket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
    }

    @Override
    public synchronized void run() {
        Message message;
        Move move;
        while (true) {
            try {
                Object receivedObject = objectInputStream.readObject();
                //System.out.println((String) receivedObject);
                Message receivedMessage = (Message) receivedObject;
                //broadcasting to all:
                if (receivedMessage.getTypeOfMessage().contains("chat")) {
                    String roomKey = "";
                    for(var entry : Server.connectedRooms.entrySet())
                    {
                        if(entry.getValue().contains(objectOutputStream))
                        {
                            roomKey = entry.getKey();
                            break;
                        }
                    }
                    broadcastToRoom(receivedObject, roomKey);
                } else if (receivedMessage.getTypeOfMessage().contains("lobbyInfo")){
                    System.out.println("\nReceived lobby request!");
                    if (receivedMessage.isNewLobbyRequest()) {
                        System.out.println("\nAsked to create new lobby...");
                        //======================CREATE A RANDOM 5 CHARACTER ROOM-ID:===================================
                        // create a string of all characters
                        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

                        // create random string builder
                        StringBuilder sb = new StringBuilder();
                        // create an object of Random class
                        Random random = new Random();
                        // specify length of random string
                        int length = 5;

                        for (int i = 0; i < length; i++) {
                            // generate random index number
                            int index = random.nextInt(alphabet.length());
                            // get character specified by index
                            // from the string
                            char randomChar = alphabet.charAt(index);
                            // append the character to string builder
                            sb.append(randomChar);
                        }
                        String roomId = sb.toString();
                        //===========================================================================================

                        //add the output stream the hashmap:
                        ArrayList<ObjectOutputStream> tempStreamArray = new ArrayList<>();
                        ArrayList<String> tempStringArray = new ArrayList<>();
                        tempStreamArray.add(this.objectOutputStream);
                        tempStringArray.add(receivedMessage.getSecondaryMessage());
                        Server.connectedRooms.put(roomId, tempStreamArray);
                        Server.usernamesByRooms.put(roomId, tempStringArray);

                        Message response = new Message(roomId, "lobbyInfo");
                        objectOutputStream.writeObject(response);
                    } else {
                        String roomId = receivedMessage.getMessage();
                        if (!Server.connectedRooms.containsKey(roomId)) {
                            System.out.println("\nno such room exists!");
                            objectOutputStream.writeObject(new Message("_error", "lobbyInfo"));
                            System.out.println("\nsend the error response back!");
                            continue;
                        } else {
                            var connectedRooms = Server.connectedRooms.get(roomId);
                            connectedRooms.add(objectOutputStream);
                            var usernamesByRoom = Server.usernamesByRooms.get(roomId);
                            usernamesByRoom.add(receivedMessage.getSecondaryMessage());

                            var responseForJoiningExistingLobby = new Message("_success", "lobbyInfo");
                            responseForJoiningExistingLobby.setSecondaryMessage(String.join(",", Server.usernamesByRooms.get(roomId)));

                            Server.connectedRooms.replace(roomId, connectedRooms);
                            Server.usernamesByRooms.replace(roomId, usernamesByRoom);

                            objectOutputStream.writeObject(responseForJoiningExistingLobby);
                            System.out.println("\nsent the success message back!");
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            // closing resources
            System.out.println("\nclosing the streams!");
            this.objectInputStream.close();
            this.objectOutputStream.close();

        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    void broadcastToRoom(Object objectToBroadcast, String roomId) throws IOException {
        System.out.println("\nBroadcasting..." + ((Message) objectToBroadcast).getMessage());
        for (var outputStream : Server.connectedRooms.get(roomId)) {
            outputStream.writeObject(objectToBroadcast);
        }
    }
}


