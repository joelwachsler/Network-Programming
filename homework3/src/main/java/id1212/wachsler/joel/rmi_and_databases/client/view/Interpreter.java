package id1212.wachsler.joel.rmi_and_databases.client.view;

import id1212.wachsler.joel.rmi_and_databases.common.*;
import id1212.wachsler.joel.rmi_and_databases.common.dto.CredentialDTO;
import id1212.wachsler.joel.rmi_and_databases.common.dto.SocketIdentifierDTO;
import id1212.wachsler.joel.rmi_and_databases.common.exceptions.RegisterException;
import id1212.wachsler.joel.rmi_and_databases.common.net.FileTransferHandler;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.logging.FileHandler;

public class Interpreter implements Runnable {
  private FileServer server;
  private boolean running = false;
  private Console console;
  private CmdLineParser parser;
  private long userId;
  private SocketChannel socket;

  /**
   * Starts a new interpreter on a separate thread.
   *
   * @param server The server registry to communicate with.
   */
  public void start(FileServer server) throws RemoteException {
    this.server = server;

    if (running) return;

    running = true;
    console = new Console();
    new Thread(this).start();
  }

  /**
   * Main interpreter loop on a separate thread.
   * Waits for user input and then evaluates the command accordingly.
   */
  @Override
  public void run() {
    while (running) {
      try {
        parser = new CmdLineParser(console.readNextLine());

        switch (parser.getCmd()) {
          case LOGIN:     login();    break;
          case REGISTER:  register(); break;
          case LIST:      list();     break;
          case UPLOAD:    upload();   break;
          case QUIT:
            console.disconnect();
            running = false;
            break;
          default:
            throw new InvalidCommandException("The provided command does not exist!");
        }
      } catch (Exception e) {
        console.error(e.getMessage(), e);
      }
    }
  }

  private void upload() throws IOException, InvalidCommandException, IllegalAccessException {
    if (userId == 0) throw new IllegalAccessException("You must be logged in to upload!");

    try {
      String localFilename = parser.getArg(0);
      String serverFilename = parser.getArg(1);
      boolean publicAccess = Boolean.valueOf(parser.getArg(2));
      boolean readable = Boolean.valueOf(parser.getArg(3));
      boolean writable = Boolean.valueOf(parser.getArg(4));

      server.upload(userId, serverFilename, publicAccess, readable, writable);

      FileTransferHandler.sendFile(socket, localFilename);
    } catch (InvalidCommandException e) {
      throw new InvalidCommandException(
        "Invalid use of the upload command!\n" +
          "the correct way is:\n" +
          "upload <local filename:string> <upload filename:string> <public:boolean> <read:boolean> <write:boolean>");
    }
  }

  private void list() throws RemoteException, IllegalAccessException {
    server.list(userId);
  }

  private void register() throws RemoteException, RegisterException, InvalidCommandException {
    try {
      CredentialDTO credentialDTO = createCredentials(parser);
      server.register(console, credentialDTO);
    } catch (InvalidCommandException e) {
      throw new InvalidCommandException(
        "Invalid use of the register command!\n" +
          "the correct way is:\n" +
          "register <username:string> <password:string>");
    }
  }

  private void login() throws IOException, LoginException, InvalidCommandException {
    try {
      userId = server.login(console, createCredentials(parser));

      createServerSocket(userId);
    } catch (InvalidCommandException e) {
      throw new InvalidCommandException(
        "Invalid use of the login command!\n" +
          "The correct way is:\n" +
          "login <username> <password>");
    }
  }

  private void createServerSocket(long userId) throws IOException {
    // Create the actual socket
    socket = SocketChannel.open();
    socket.connect(new InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_SOCKET_PORT));

    // Lets identify this socket with the current user to the server.
    ObjectOutputStream output = new ObjectOutputStream(socket.socket().getOutputStream());

    output.writeObject(new SocketIdentifierDTO(userId));
    output.flush();
    output.reset();
  }

  private CredentialDTO createCredentials(CmdLineParser parser) throws InvalidCommandException {
    String username = parser.getArg(0);
    String password = parser.getArg(1);
    return new CredentialDTO(username, password);
  }

  public class Console extends UnicastRemoteObject implements Listener {
    private static final String PROMPT = "> ";
    private final ThreadSafeStdOut outMsg = new ThreadSafeStdOut();
    private final Scanner console = new Scanner(System.in);

    Console() throws RemoteException {
    }

    @Override
    public void print(String msg) throws RemoteException {
      outMsg.println("\r" + msg);
      outMsg.print(PROMPT);
    }

    @Override
    public void error(String error, Exception e) {
      outMsg.println("ERROR:");
      outMsg.println(error);
    }

    @Override
    public void disconnect() throws RemoteException {
      print("You are now disconnected!");
    }

    String readNextLine() throws RemoteException {
      outMsg.print(PROMPT);

      return console.nextLine();
    }
  }
}
