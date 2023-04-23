package cn.edu.sustech.cs209.chatting.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatService implements Runnable{
    private Socket socket = null;
    String name;        // 客户端名字
    BufferedReader in;
    PrintWriter out;
    Map<String,Socket> clientMap;
    List<String> groupChatName;
    List<String> fileSendName;
    public final Object waitLockChatService = new Object();
    List<byte[]> fileInfo;


    public ChatService(Socket socket,Map<String,Socket> clientMap,List<String> fileSendName,List<byte[]> fileInfo)
        throws IOException {
        this.socket = socket;
        this.clientMap = clientMap;
        this.fileSendName = fileSendName;
        this.fileInfo = fileInfo;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
    }

    @Override
    public void run() {
        groupChatName = new ArrayList<>();
        while (true){
            try {
                System.out.println("A");
                String input = in.readLine();
                System.out.println(input);
                if (input != null){
                    String[] strings = input.split(";");
                    if (input.startsWith("0")){
                        Set<String> set = clientMap.keySet();
                        System.out.println(set);
                        StringBuilder sb = new StringBuilder();
                        sb.append(0+";");
                        for (String str : set) {
                            sb.append(str+";");
                        }
                        System.out.println(sb);
                        out.println(sb);
                        out.flush();
                    }
                    else if (input.startsWith("1")){
                        clientMap.put(strings[1],socket);
                        name = strings[1];
                    }
                    else if (input.startsWith("2")){
                        String username = strings[1];
                        String chatName = strings[2];
                        String message = strings[3];
                        Socket client = clientMap.get(chatName);
                        System.out.println(chatName);
                        PrintStream printStream = new PrintStream(client.getOutputStream());
                        printStream.println(input);
                        printStream.flush();
                    }else if (input.startsWith("3")){
                        Set<String> set = clientMap.keySet();
                        System.out.println(set);
                        StringBuilder sb = new StringBuilder();
                        sb.append(3+";");
                        for (String str : set) {
                            sb.append(str+";");
                        }
                        System.out.println(sb);
                        out.println(sb);
                        out.flush();
                    }else if (input.startsWith("4")){
                        groupChatName.clear();
                        StringBuilder sb = new StringBuilder();
                        sb.append(4+";");
                        for (int i = 1; i < strings.length; i++) {
                            sb.append(strings[i] + ";");
                            groupChatName.add(strings[i]);
                        }
                        for (int i = 0; i < groupChatName.size(); i++) {
                            System.out.println(groupChatName.get(i));
                            Socket client = clientMap.get(groupChatName.get(i));
                            PrintStream printStream = new PrintStream(client.getOutputStream());
                            printStream.println(sb);
                            printStream.flush();
                        }
                    }else if (input.startsWith("5")){
                        for (int i = 0; i < groupChatName.size(); i++) {
                            System.out.println("User = "+strings[1]);
                            if (!groupChatName.get(i).equals(strings[1])){
                                Socket client = clientMap.get(groupChatName.get(i));
                                PrintStream printStream = new PrintStream(client.getOutputStream());
                                printStream.println(input);
                                printStream.flush();
                            }
                        }
                    }else if (input.startsWith("6")){
                        fileSendName.clear();
                        fileSendName.addAll(Arrays.asList(strings).subList(1, strings.length));
                        System.out.println(fileSendName.toString());
                    }else if (input.startsWith("7")){
                        String user = strings[1];
                        System.out.println("fileSendName = "+fileSendName.toString());
                        System.out.println(fileInfo.toString());
                        if (fileSendName.contains(user)){
                            Socket client = clientMap.get(user);
                            OutputStream printStream = client.getOutputStream();
//                            printStream.write("8".getBytes());
                            printStream.write(fileInfo.get(0));
                            System.out.println("B");
                            printStream.flush();
                        }
                    }else {
                        System.out.println("file");
                        synchronized (waitLockChatService){
                            try {
                                fileService fileService = new fileService(socket,input,waitLockChatService);
                                fileService.start();
                                waitLockChatService.wait();
                                fileInfo.clear();
                                fileInfo.add(fileService.getFileInfo());
                                System.out.println(Arrays.toString(fileInfo.get(0)));
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }


                    }
                }else {
                    socket.close();
                    System.out.println("Client closed");
                    clientMap.remove(name);
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class fileService extends Thread{
    Socket socket;
    String path;
    Object wait;
    byte[] fileInfo;

    public fileService(Socket socket,String path,Object wait) {
        this.socket = socket;
        this.path = path;
        this.wait = wait;
    }
    @Override
    public void run(){
        try {
//            DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            fileInfo = new byte[(int) file.length()];
            int a = fis.read(fileInfo);
            System.out.println(a);

            OutputStream stream = socket.getOutputStream();
            stream.write(fileInfo);
            stream.flush();
            fis.close();
            synchronized (wait){
                wait.notifyAll();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public byte[] getFileInfo() {
        return fileInfo;
    }
}
