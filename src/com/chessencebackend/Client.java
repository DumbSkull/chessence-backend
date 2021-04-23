package com.chessencebackend;

import com.chessence.Message;

import java.io.*;
import java.net.*;
import java.util.Scanner;

// Client class
public class Client {

    public static String username;

    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);

        // getting localhost ip
        InetAddress ec2Instance = InetAddress.getByName("52.66.201.63");
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection with server port 5056
        Socket s = new Socket(ip, 7989);

        System.out.println("\nConnected to the server! ");
        // obtaining input and out streams
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream objectInputStream = new ObjectInputStream(s.getInputStream());

        Scanner input = new Scanner(System.in);
        System.out.print("\nEnter your name: ");
        Client.username = input.nextLine();
        System.out.println("\nDo you want to create a new lobby? (Y/N)");
        char newLobbyChoice = input.next().charAt(0);
        if (newLobbyChoice == 'Y' || newLobbyChoice == 'y') {
            var msg = new Message("", "lobbyInfo", true);
            msg.setSecondaryMessage(username);
            objectOutputStream.writeObject(msg);
            System.out.println("\nsent lobby request!");
            while (true) {
                try {
                    Object response = objectInputStream.readObject();
                    Message createLobbyResponse = (Message) response;
                    System.out.println("\nYour new room's ID is: " + createLobbyResponse.getMessage());
                    break;
                } catch (ClassNotFoundException e) {
                    //e.printStackTrace();
                    continue;
                }
            }
        } else {
            boolean roomJoined = false;
            while (true) {
                System.out.println("\nEnter the room ID to join: ");
                String roomId = input.nextLine();
                while(roomId.length()==0)
                {
                    System.out.println("\nEnter a valid room ID: ");
                    roomId = input.nextLine();
                }
                Message roomIdMessage = new Message(roomId, "lobbyInfo", false);
                roomIdMessage.setSecondaryMessage(username);
                objectOutputStream.writeObject(roomIdMessage);
                while (true) {
                    try {
                        Message response = (Message) objectInputStream.readObject();
                        System.out.println("\nMessage received: " + response.getMessage());
                        if (response.getMessage().contains("_error")) {
                            System.out.println("\nERROR: Enter a valid room ID!");
                            break;
                        } else if (response.getMessage().contains("_success")) {
                            System.out.println("\nSuccessfully joined the room!");
                            roomJoined = true;
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                if (roomJoined)
                    break;
            }
        }

        Thread writingThread = new ClientWriter(s, objectOutputStream);
        Thread readingThread = new ClientReader(s, objectInputStream);

        writingThread.start();
        readingThread.start();
    }
}