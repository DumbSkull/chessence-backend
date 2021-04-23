package com.chessencebackend;

import java.io.*;
import java.util.*;
import java.net.*;

// Server class
public class Server
{
    public static HashMap<String, ArrayList<ClientDetails>> connectedRooms = new HashMap<>();
    //public static HashMap<String, ArrayList<String>> usernamesByRooms = new HashMap<>();
    //public static ArrayList<ObjectOutputStream> connectedOutputStreams = new ArrayList<ObjectOutputStream>();

    public static void main(String[] args) throws IOException
    {
        // server is listening on port 7989
        ServerSocket serverSocket = new ServerSocket(7989);

        // running infinite loop for getting
        // client request
        System.out.println("\nListening for a client...");
        while (true)
        {
            Socket clientSocket = null;
            try
            {
                // socket object to receive incoming client requests
                clientSocket = serverSocket.accept();

                System.out.println("A new client is connected : " + clientSocket);

                // obtaining input and out streams
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

                System.out.println("Assigning new thread for this client");

                // create a new thread object

                Thread thread = new ClientHandler(clientSocket, objectInputStream, objectOutputStream);

                // Invoking the start() method
                thread.start();

            }
            catch (Exception e){
                clientSocket.close();
                e.printStackTrace();
                break;
            }
        }
    }

    //for development purposes:
    public static void showAllConnectedClientsDetails()
    {
        System.out.println("\nDisplaying the client details stored so far: \n");
        for (var entry : Server.connectedRooms.entrySet()) {
            System.out.println("\n=====================================");
            System.out.println("\nRoom ID: " + entry.getKey());
            for(int i=0; i<entry.getValue().size(); i++)
            {
                var currentValue = entry.getValue().get(i);
                System.out.println("\nClient " + (i+1) + " Details Below:- ");
                System.out.println("\nUsername: " + currentValue.getUsername());
                System.out.println("\nisPlayer: " + (currentValue.isPlayer() ? "true" : "false"));
                System.out.println("\nSocket Details: " + currentValue.getSocket().toString());
                System.out.println("\n-----------------------------------");
            }
            System.out.println("\n=====================================");
        }
    }
}

