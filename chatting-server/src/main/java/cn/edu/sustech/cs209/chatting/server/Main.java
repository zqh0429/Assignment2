package cn.edu.sustech.cs209.chatting.server;

//import cn.edu.sustech.cs209.chatting.client.Client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

  public static Map<String, Socket> clientMap;

  public static void main(String[] args) throws IOException {
    ServerSocket serverSocket = new ServerSocket(30429);
    System.out.println("Waiting");
    clientMap = new ConcurrentHashMap<>();
    ArrayList<String> fileSendName = new ArrayList<>();
    List<byte[]> fileInfo = new ArrayList<>();
    List<Long> fileLength = new ArrayList<>();
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        System.out.println("Starting server" + socket);
        System.out.println(1);
        new Thread(new ChatService(socket, clientMap, fileSendName, fileInfo, fileLength)).start();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
