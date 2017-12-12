package id1212.wachsler.joel.homework5.server.net;

import id1212.wachsler.joel.homework5.common.GameGuess;
import id1212.wachsler.joel.homework5.common.GameState;
import id1212.wachsler.joel.homework5.server.controller.Controller;

import java.io.*;
import java.net.Socket;

/**
 * Handles a client socket instance.
 */
public class ClientHandler implements Runnable {
  private final HangmanServer server;
  private final Socket clientSocket;
  private ObjectInputStream fromClient;
  private ObjectOutputStream toClient;
  private boolean connected;
  private Controller controller = new Controller();

  ClientHandler(HangmanServer server, Socket clientSocket) {
    this.server = server;
    this.clientSocket = clientSocket;
    controller.newHangmanGame();
    connected = true;
  }

  @Override
  public void run() {
    // Create input and output streams to the client
    try {
      fromClient = new ObjectInputStream(clientSocket.getInputStream());
      toClient = new ObjectOutputStream(clientSocket.getOutputStream());
    } catch (IOException e) {
      System.err.println("Failed to create read/write streams...");
      e.printStackTrace();
    }

    while (connected) {
      try {
        GameGuess msg = (GameGuess) fromClient.readObject(); // Blocking TCP connection

        sendMsg(controller.guess(msg.getGuess()));
      } catch (EOFException e) {
        System.err.println("The client unexpectedly disconnected!");
        disconnectClient();
      } catch (IOException | ClassNotFoundException e) {
        System.err.println(e.getMessage());
        e.printStackTrace();
        disconnectClient();
      }
    }
  }

  /**
   * Sends a message to the client.
   */
  private void sendMsg(GameState state) throws IOException {
    toClient.writeObject(state);
    toClient.flush(); // Flush the pipe
    toClient.reset(); // Remove object cache
  }

  private void disconnectClient() {
    try {
      clientSocket.close();
    } catch (IOException e) {
      System.err.println("Couldn't close a client connection!");
      e.printStackTrace();
    }

    connected = false;
    server.removeHandler(this);
  }
}