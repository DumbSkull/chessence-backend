package com.chessencebackend;

import com.chessence.Message;
import com.chessence.Move;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class ClientHandler extends Thread {
    private ObjectOutputStream objectOutputStream = null;
    private ObjectInputStream objectInputStream = null;
    private String roomId = null;
    private String username;
    private boolean isPlayer = true;
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

                if (receivedObject instanceof Message) {
                    Message receivedMessage = (Message) receivedObject;
                    System.out.println("\nMessage received type: " + receivedMessage.getTypeOfMessage());
                    // ============================================= HANDLING THE CHATTING:  =============================================
                    if (receivedMessage.getTypeOfMessage().contains("chat")) {
                        if (this.roomId == null) {
                            String roomKey = "";
                            boolean found = false;
                            for (var entry : Server.connectedRooms.entrySet()) {
                                for (ClientDetails clientDetails : entry.getValue()) {
                                    if (clientDetails.getObjectOutputStream() == this.objectOutputStream) {
                                        roomKey = entry.getKey();
                                        this.roomId = roomKey;
                                        found = true;
                                        break;
                                    }
                                }
                                if (found)
                                    break;
                            }
                        }
                        broadcastToRoom(receivedObject, this.roomId);

                        // ========================================== END OF HANDLING CHATTING ================================================
                    } else if (receivedMessage.getTypeOfMessage().contains("leaveLobby")) {
                        //========================== REMOVING THE CURRENT USER FROM THE HASHMAP: ===============================
                        removeCurrentUser();
                        Server.showAllConnectedClientsDetails();
                        //=========================== END OF REMOVING THE CURRENT USER =========================================
                    } else if (receivedMessage.getTypeOfMessage().contains("lobbyInfo")) {
                        this.username = receivedMessage.getSecondaryMessage().trim().replaceAll(",", "");

                        // ============================================================ HANDLING REQUESTS TO CREATE NEW LOBBY:  ============================================================
                        if (receivedMessage.isNewLobbyRequest()) {
                            System.out.println("\nAsked to create new lobby...");

                            //=============CREATE A RANDOM 5 CHARACTER ROOM-ID:====================

                            String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; // create a string of all characters
                            StringBuilder sb = new StringBuilder(); // create random string builder
                            Random random = new Random(); // create an object of Random class
                            int length = 5; // specify length of random string

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

                            //====================================

                            //add the output stream the hashmap:
                            ClientDetails currentClientDetails = new ClientDetails(this.username, this.objectOutputStream, this.clientSocket, true);
                            ArrayList<ClientDetails> tempClientDetails = new ArrayList<ClientDetails>();
                            tempClientDetails.add(currentClientDetails);
                            Server.connectedRooms.put(roomId, tempClientDetails);
                            this.roomId = roomId;
                            Message response = new Message(roomId, "lobbyInfo");
                            objectOutputStream.writeObject(response);

                            Server.showAllConnectedClientsDetails();

                            // =========================================================== END OF CREATE-LOBBY-REQUEST =============================================================
                        } else {

                            // ============================================================ HANDLING REQUESTS TO JOIN EXISTING LOBBY:  ============================================================

                            //Error - Couldn't find the room:
                            String roomId = receivedMessage.getMessage();
                            if (!Server.connectedRooms.containsKey(roomId)) {
                                System.out.println("\nERROR: No such room exists!");
                                objectOutputStream.writeObject(new Message("_error", "lobbyInfo"));
                                continue;
                            } else {

                                //Success - When existing room is found:


                                //Getting all players names and putting in allUsernames:

                                var playerList = Server.connectedRooms.get(roomId).stream().
                                        filter(clientDetails -> clientDetails.isPlayer()).
                                        map(clientDetails -> clientDetails.getUsername()).
                                        collect(Collectors.toList());

                                var spectatorList = Server.connectedRooms.get(roomId).stream().
                                        filter(clientDetails -> !clientDetails.isPlayer()).
                                        map(clientDetails -> clientDetails.getUsername()).
                                        collect(Collectors.toList());

                                if ((playerList.size() == 2) && (spectatorList.size() == 4)) {
                                    var responseForFullLobby = new Message("_error", "lobbyInfo");
                                    responseForFullLobby.setSecondaryMessage("The lobby is full! Cannot join a full lobby.");
                                    objectOutputStream.writeObject(responseForFullLobby);
                                    continue;
                                }

                                var responseForJoiningExistingLobby = new Message("_success", "lobbyInfo");

                                String allUsernames = String.join(",", playerList);

                                //Getting all specatators names and appending it to allUsernames after the keyword "_#SPECTATORS#_":
                                allUsernames = allUsernames + "_#SPECTATORS#_" + String.join(",", spectatorList);

                                //putting this in the response message:
                                responseForJoiningExistingLobby.setSecondaryMessage(allUsernames);
                                this.isPlayer = (playerList.size() < 2);
                                ClientDetails currentClientDetails = new ClientDetails(this.username, this.objectOutputStream, this.clientSocket, this.isPlayer);
                                var connectedRooms = Server.connectedRooms.get(roomId);
                                connectedRooms.add(currentClientDetails);
                                Server.connectedRooms.replace(roomId, connectedRooms);
                                objectOutputStream.writeObject(responseForJoiningExistingLobby);

                                this.roomId = roomId;

                                //broadcasting the newly joined client to everyone in the room:
                                var newPersonJoinedLobby = new Message(this.username, "newPlayerJoinedLobby");
                                newPersonJoinedLobby.setSecondaryMessage(this.isPlayer ? "player" : "spectator");
                                broadcastToRoom(newPersonJoinedLobby, roomId);
                            }

                            Server.showAllConnectedClientsDetails();
                            // ======================================================== END OF HANDLING REQUESTS TO JOIN EXISTING LOBBY:  ========================================================
                        }
                    } else if (receivedMessage.getTypeOfMessage().contains("playerChangeStatus")) {
                        // ======================================================== CHANGING THE PLAYER STATUS (B/W PLAYER AND SPEC):  ========================================================
                        this.isPlayer = !this.isPlayer;
                        for (int i = 0; i < Server.connectedRooms.get(this.roomId).size(); i++) {
                            if (Server.connectedRooms.get(this.roomId).get(i).getSocket() == this.clientSocket) {
                                //updating the isPlayer condition:
                                var tempClient = Server.connectedRooms.get(this.roomId).get(i);
                                tempClient.setPlayer(this.isPlayer);
                                Server.connectedRooms.get(this.roomId).set(i, tempClient);

                                //broadcasting to everyone about this player's status change:
                                var anotherPlayerChangedStatus = new Message(this.username, "anotherPlayerChangedStatus");
                                anotherPlayerChangedStatus.setSecondaryMessage(!this.isPlayer ? "wasPlayer" : "wasSpectator");
                                broadcastToRoom(anotherPlayerChangedStatus, this.roomId);

                                break;
                            }
                        }

                        // =================================================== END OF CHANGING THE PLAYER STATUS (B/W PLAYER AND SPEC):  ======================================================
                    } else if (receivedMessage.getTypeOfMessage().contains("gameStart")) {
                        // ================================================================================
                        //Informing everyone that the game has begun!
                        var gameStartedMessage = new Message("", "gameStarted");
                        gameStartedMessage.setSecondaryMessage(receivedMessage.getSecondaryMessage());  //secondary message contains the name of the person who started
                        broadcastToRoom(gameStartedMessage, this.roomId);
                        // ================================================================================
                    } else if (receivedMessage.getTypeOfMessage().contains("playerForfeit")) {
                        // ================================================================================
                        //Informing everyone that a player has left
                        var playerForfeitMessage = new Message(receivedMessage.getMessage(), "playerForfeit");     //getMessage contains the name of the person who forfeited
                        playerForfeitMessage.setSecondaryMessage(this.isPlayer ? "player" : "spectator"); //secondary message contains the status of the player forfeiting (player/spec)
                        broadcastToRoom(playerForfeitMessage, this.roomId);
                        removeCurrentUser();
                        // ================================================================================
                    }
                    else if (receivedMessage.getTypeOfMessage().contains("gameOver")) {
                        // ================================================================================
                        //Informing everyone that a player won
                        var gameOverMessage = new Message(receivedMessage.getMessage(), "gameOver");     //getMessage contains the name of the person who won
                        broadcastToRoom(gameOverMessage, this.roomId);
                        // ================================================================================
                    }
                } else if (receivedObject instanceof Move) {
                    System.out.println("\nMOVE OPERATION RECEIVED!");
                    // =================================================== IF ITS A MOVE OPERATION:  ======================================================
                    var receivedMove = (Move) receivedObject;
                    broadcastToRoom(receivedMove, this.roomId);
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
            System.out.println("\nClosing the input and output streams for the socket: " + this.clientSocket.toString());
            removeCurrentUser();
            this.objectInputStream.close();
            this.objectOutputStream.close();

        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    void removeCurrentUser() throws IOException {

        //broadcasting to everyone about who left the lobby:
        var playerLeaveLobbyMessage = new Message(this.username, "anotherPlayerLeftLobby");
        broadcastToRoom(playerLeaveLobbyMessage, this.roomId);

        //informing the current client to stop the reading thread that he/she has started:
        var informToLeaveReadingThreadMessage = new Message("", "playerLeftLobby");
        this.objectOutputStream.writeObject(informToLeaveReadingThreadMessage);

        if (this.roomId != null) {
            Server.connectedRooms.get(roomId)
                    .removeIf(clientDetails -> clientDetails.getSocket() == this.clientSocket);
            if (Server.connectedRooms.get(roomId).size() == 0)
                Server.connectedRooms.remove(roomId);

            this.roomId = null;
        }
    }

    void broadcastToRoom(Object objectToBroadcast, String roomId) throws IOException {
        //Server.showAllConnectedClientsDetails();
        for (var clientDetails : Server.connectedRooms.get(roomId)) {
            if (clientDetails.getSocket() != this.clientSocket)
                clientDetails.getObjectOutputStream().writeObject(objectToBroadcast);
        }
    }
}


