package com.chessencebackend;

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
                Message receivedMessage = (Message) receivedObject;
                System.out.println("\nreceived a message!");
                System.out.println("\nmessage: " + receivedMessage.getMessage() + "\ntype of message: "+receivedMessage.getTypeOfMessage() + "\n" + receivedMessage.isNewLobbyRequest());
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
                    System.out.println("\nBroadcasting..." + ((Message) receivedObject).getMessage());
                    for (var outputStream : Server.connectedRooms.get(roomKey)) {
                        outputStream.writeObject(receivedObject);
                    }
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
                        tempStreamArray.add(this.objectOutputStream);
                        Server.connectedRooms.put(roomId, tempStreamArray);

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
                            System.out.println("\nRoom exists!");
                            var connectedRooms = Server.connectedRooms.get(roomId);
                            connectedRooms.add(objectOutputStream);
                            Server.connectedRooms.replace(roomId, connectedRooms);
                            objectOutputStream.writeObject(new Message("_success", "lobbyInfo"));
                            System.out.println("\nsent the success message back!");
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                continue;
            } catch (IOException e) {
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
}


