package cn.edu.sustech.cs209.chatting.client;

//import static cn.edu.sustech.cs209.chatting.server.Main;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller extends Parent implements Initializable, Runnable {

  public static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  public static PrintWriter stream = null;
  public static OutputStream outputStream = null;

  public static synchronized void println(String out) {
    try {
      if (stream == null) {
        stream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
      }
      stream.println(out);
      stream.flush();
    } catch (Exception e) {
      throw new RuntimeException();
    }

  }

  public static synchronized void printByte(byte[] out) {
    try {
      if (outputStream == null) {
        outputStream = socket.getOutputStream();
      }
      outputStream.write(out);
      outputStream.flush();
    } catch (Exception e) {
      throw new RuntimeException();
    }

  }

  public ListView<String> chatList;
  List<String> clients;
  public TextArea inputArea;
  public Label currentUsername;
  public Label currentOnlineCnt;
  @FXML
  ListView<Message> chatContentList;

  public static Socket socket;

  String username;
  String chatName;
  volatile List<String> usersList;
  ComboBox<String> userSel;
  private final Object waitLock = new Object();
  private final Object waitLock2 = new Object();


  Map<String, ListView<Message>> privateChatMap = new HashMap<>();
  Map<String, ListView<Message>> groupChatMap = new HashMap<>();
  ListView<Message> groupMessageView;
  //    Map<String,Stage> chatWindowMap = new HashMap<>();
  int fileLength;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    try {
      socket = new Socket("localhost", 30429);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");
    Optional<String> input = dialog.showAndWait();
    BufferedReader reader;
    usersList = new ArrayList<>();

    if (input.isPresent() && !input.get().isEmpty()) {
      /*
              TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
                     */
      username = input.get();
      System.out.println(username);
      new Thread(this).start();

      synchronized (waitLock) {
        try {
          //                    System.out.println("2");
          println("0");
          waitLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (usersList.isEmpty() || !usersList.contains(username)) {
        currentUsername = new Label();
        currentOnlineCnt = new Label();
        clients = new ArrayList<>();
      } else {
        System.out.println("Username exists");
        Platform.exit();
      }
    } else {
      System.out.println("Invalid username " + input + ", exiting");
      Platform.exit();
    }
    chatContentList.setCellFactory(new MessageCellFactory());
  }

  @FXML
  public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();

    Stage stage = new Stage();
    userSel = new ComboBox<>();

    // FIXME: get the user list from server, the current user's name should be filtered out
    userSel.getItems().addAll(usersList);

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      user.set(userSel.getSelectionModel().getSelectedItem());
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    if (!privateChatMap.containsKey(user.get() + ";" + username) && !privateChatMap.containsKey(
        username + ";" + user.get())
        && user.get() != null) {
      Stage chatWindow = new Stage();
      stage.setTitle(user.get());
      ListView<Message> messageListView = new ListView<>();
      messageListView.setCellFactory(new MessageCellFactory());
      StringBuilder sb = new StringBuilder();
      sb.append(username + ";" + user.get());
      privateChatMap.put(sb.toString(), messageListView);
      chatWindow.setScene(new Scene(messageListView));
      chatWindow.show();
      if (!clients.contains(user.get())) {
        chatList.getItems().add(user.get());
        clients.add(user.get());
      }
    }
    chatName = user.get();
    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
    Stage stage = new Stage();

    List<CheckBox> checkBoxList = new ArrayList<>();
    for (String user : usersList) {
      CheckBox checkBox = new CheckBox(user);
      checkBoxList.add(checkBox);
    }
    System.out.println("A");

    Button okBtn = new Button("OK");

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(checkBoxList);
    box.getChildren().add(okBtn);
    System.out.println("B");
    stage.setScene(new Scene(box));
    stage.show();
    System.out.println("C");
    List<String> strings = new ArrayList<>();
    okBtn.setOnAction(e -> {
      for (Node node : box.getChildren()) {
        if (node instanceof CheckBox) {
          CheckBox checkBox = (CheckBox) node;
          if (checkBox.isSelected()) {
            strings.add(checkBox.getText());
          }
          checkBox.setOnAction(
              null); // remove event handler to prevent multiple triggering
        }
      }
      stage.close();
      strings.add(username);
      Collections.sort(strings);
      StringBuilder groupChatInfo = new StringBuilder();
      for (int i = 0; i < strings.size() - 1; i++) {
        groupChatInfo.append(strings.get(i) + ";");
      }
      groupChatInfo.append(strings.get(strings.size() - 1));
      chatName = groupChatInfo.toString();
      if (!groupChatMap.containsKey(chatName)) {
        chatList.getItems().add(groupChatInfo.toString());
        Stage chatWindow = new Stage();
        chatWindow.setTitle(chatName);
        ListView<Message> messageListView = new ListView<>();
        messageListView.setCellFactory(new MessageCellFactory());
        chatWindow.setScene(new Scene(messageListView));
        chatWindow.show();
        groupChatMap.put(chatName, messageListView);
      }

    });

  }

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed. After sending the message, you should clear the text input
   * field.
   */
  @FXML
  public void doSendMessage() {
    System.out.println(inputArea.getText());
    if (!inputArea.getText().contains(" ") && !inputArea.getText().isEmpty()) {

//            chatContentList.getItems().add(message);
      String[] strings = inputArea.getText().split("\n");
      if (strings.length == 1) {
        System.out.println("M");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        Message message = new Message(formatter.format(date), username, chatName,
            inputArea.getText());
        if (privateChatMap.containsKey(username + ";" + chatName)) {
          Platform.runLater(() -> {
            ListView<Message> messageListView = privateChatMap.get(
                username + ";" + chatName);
            messageListView.getItems().add(message);
          });

        } else if (privateChatMap.containsKey(chatName + ";" + username)) {
          Platform.runLater(() -> {
            ListView<Message> messageListView = privateChatMap.get(
                chatName + ";" + username);
            messageListView.getItems().add(message);
          });
        }
        println("2" + ";" + username + ";" + chatName + ";" + inputArea.getText());
      } else {
        for (int i = 0; i < strings.length; i++) {
          SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          Date date = new Date(System.currentTimeMillis());
          Message message = new Message(formatter.format(date), username, chatName,
              strings[i]);
          if (privateChatMap.containsKey(username + ";" + chatName)) {
            Platform.runLater(() -> {
              ListView<Message> messageListView = privateChatMap.get(
                  username + ";" + chatName);
              messageListView.getItems().add(message);
            });

          } else if (privateChatMap.containsKey(chatName + ";" + username)) {
            Platform.runLater(() -> {
              ListView<Message> messageListView = privateChatMap.get(
                  chatName + ";" + username);
              messageListView.getItems().add(message);
            });
          }
          println("2" + ";" + username + ";" + chatName + ";" + strings[i]);
        }
      }
    }
    inputArea.clear();
  }

  @FXML
  public void doSendGroupMessage() {
    System.out.println(inputArea.getText());
    if (!inputArea.getText().contains(" ")) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date(System.currentTimeMillis());
      Message message = new Message(formatter.format(date), username, chatName,
          inputArea.getText());
      Platform.runLater(() -> {
        ListView<Message> messageListView = groupChatMap.get(chatName);
        messageListView.getItems().add(message);
      });
      println(4 + ";" + chatName);
//            chatContentList.getItems().add(message);
      String[] strings = inputArea.getText().split("\n");
      if (strings.length == 1) {
        println("5" + ";" + username + ";" + inputArea.getText());
      } else {
        for (int i = 0; i < strings.length; i++) {
          println("5" + ";" + username + ";" + strings[i]);
        }
      }
    }
    inputArea.clear();
  }

  @FXML
  public void selectAndSendFile() {
    // 弹出文件选择对话框，让用户选择要发送的文件
    FileChooser fileChooser = new FileChooser();
    File selectedFile = fileChooser.showOpenDialog(null);
    Stage stage = new Stage();

    List<CheckBox> checkBoxList = new ArrayList<>();
    for (String user : usersList) {
      CheckBox checkBox = new CheckBox(user);
      checkBoxList.add(checkBox);
    }
    System.out.println("A");

    Button okBtn = new Button("OK");

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(checkBoxList);
    box.getChildren().add(okBtn);
    System.out.println("B");
    stage.setScene(new Scene(box));
    stage.show();
    System.out.println("C");
    List<String> strings = new ArrayList<>();
    okBtn.setOnAction(e -> {
      for (Node node : box.getChildren()) {
        if (node instanceof CheckBox) {
          CheckBox checkBox = (CheckBox) node;
          if (checkBox.isSelected()) {
            strings.add(checkBox.getText());
          }
          checkBox.setOnAction(
              null); // remove event handler to prevent multiple triggering
        }
      }
      stage.close();
      StringBuilder fileSendInfo = new StringBuilder();
      for (int i = 0; i < strings.size(); i++) {
        fileSendInfo.append(strings.get(i) + ";");
      }
      println("6" + ";" + fileSendInfo);
      if (selectedFile != null) {
//                String path = selectedFile.getPath();
        try {
          byte[] bytes = Files.readAllBytes(selectedFile.toPath());
          println("9" + ";" + bytes.length);
          System.out.println(Arrays.toString(bytes));
          printByte(bytes);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    });

  }

  public void receiveFile() {
    println("7" + ";" + username);
  }

  @Override
  public void run() {
    println(1 + ";" + username);
    BufferedReader reader = null;
    try {
      InputStream inputStream = socket.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
//            groupMessageView.setCellFactory(new MessageCellFactory());
      while (true) {
        String input = reader.readLine();
        System.out.println("Read [" + input + "]");
        currentOnlineCnt.setText(String.valueOf(usersList.size() + 1));
        queue.put(input);
        while (!queue.isEmpty()) {
          String peek = queue.peek();
          if (peek == null) {
            return;
          }
          if (peek.startsWith("0")) {
            String str = queue.poll();
            String[] messageInfo = str.split(";");
            for (int i = 1; i < messageInfo.length; i++) {
              usersList.add(messageInfo[i]);
            }
            synchronized (waitLock) {
              waitLock.notifyAll();
            }
          } else if (peek.startsWith("2")) {
            String str = queue.poll();
            String[] messageInfo = str.split(";");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            System.out.println(formatter.format(date));
            Message message = new Message(formatter.format(date), messageInfo[1],
                messageInfo[2], messageInfo[3]);
            String key = messageInfo[1] + ";" + messageInfo[2];
            System.out.println(key);
            if (!privateChatMap.containsKey(key) && !privateChatMap.containsKey(
                messageInfo[2] + ";" + messageInfo[1])) {
              ListView<Message> messageListView = new ListView<>();
              messageListView.setCellFactory(new MessageCellFactory());
              privateChatMap.put(key, messageListView);
              messageListView.getItems().add(message);
              Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setScene(new Scene(messageListView));
                stage.show();
              });
            } else {
              Platform.runLater(() -> {
                if (privateChatMap.containsKey(key)) {
                  ListView<Message> messageListView = privateChatMap.get(key);
                  messageListView.getItems().add(message);
                } else if (privateChatMap.containsKey(
                    messageInfo[2] + ";" + messageInfo[1])) {
                  ListView<Message> messageListView = privateChatMap.get(
                      messageInfo[2] + ";" + messageInfo[1]);
                  messageListView.getItems().add(message);
                }
              });

            }
            if (!chatList.getItems().contains(messageInfo[1])) {
              chatList.getItems().add(messageInfo[1]);
            }
          } else if (peek.startsWith("3")) {
            String str = queue.poll();
            String[] newUsers = str.split(";");
            usersList.clear();
            for (int i = 1; i < newUsers.length; i++) {
              if (!newUsers[i].equals(username)) {
                usersList.add(newUsers[i]);
              }
            }
            System.out.println("userslist: " + usersList.toString());
            userSel = new ComboBox<>();
            userSel.getItems().addAll(usersList);
          } else if (peek.startsWith("4")) {//groupChat
            String str = queue.poll();
            String[] strings = str.split(";");
            String[] messageInfo = new String[strings.length - 1];
            for (int i = 1, j = 0; i < strings.length; i++, j++) {
              messageInfo[j] = strings[i];
            }
            Arrays.sort(messageInfo);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < messageInfo.length - 1; i++) {
              sb.append(messageInfo[i] + ";");
            }
            sb.append(messageInfo[messageInfo.length - 1]);
            chatName = sb.toString();
            if (!groupChatMap.containsKey(chatName)) {
              ListView<Message> messageListView = new ListView<>();
              messageListView.setCellFactory(new MessageCellFactory());
              groupChatMap.put(chatName, messageListView);
              groupMessageView = messageListView;
              Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setScene(new Scene(messageListView));
                stage.setTitle(chatName);
                stage.show();
              });
            } else {
              Platform.runLater(() -> {
                groupMessageView = groupChatMap.get(chatName);
              });
            }
          } else if (peek.startsWith("5")) {
            String str = queue.poll();
            String[] messageInfo = str.split(";");
            System.out.println(chatName);
            Platform.runLater(() -> {
              SimpleDateFormat formatter = new SimpleDateFormat(
                  "yyyy-MM-dd HH:mm:ss");
              Date date = new Date(System.currentTimeMillis());
              System.out.println(formatter.format(date));
              Message message = new Message(formatter.format(date), messageInfo[1],
                  chatName, messageInfo[2]);
              groupMessageView.getItems().add(message);
              if (!chatList.getItems().contains(chatName)) {
                chatList.getItems().add(chatName);
              }
            });

          } else if (peek.startsWith("7")) {
            String str = queue.poll();
            String[] messageInfo = str.split(";");
            fileLength = Integer.parseInt(messageInfo[1]);
            System.out.println("R");
            synchronized (waitLock2) {
              try {
                fileService fileService = new fileService(socket, waitLock2,
                    fileLength);
                fileService.start();
                waitLock2.wait();
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            System.out.println("T");
          }
        }
      }
    } catch (Throwable e) {
//            throw new RuntimeException(e);
      System.err.println("Disconnected");
      Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
        Platform.exit();
      });
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void refresh() {
    println("3");
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label("By: " + msg.getSentBy());
          Label msgLabel = new Label(msg.getData());
          Label timeLabel = new Label(msg.getTimestamp());
          Label sendToLabel = new Label("To: " + msg.getSendTo());

          msgLabel.setPrefSize(300, 20);
          msgLabel.setWrapText(true);
          msgLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          nameLabel.setPrefSize(80, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          timeLabel.setPrefSize(150, 20);
          timeLabel.setWrapText(true);
          timeLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          sendToLabel.setPrefSize(80, 20);
          sendToLabel.setWrapText(true);
          sendToLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel, sendToLabel, timeLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, sendToLabel, timeLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }

    @Override
    public String toString() {
      return chatContentList.getAccessibleText();
    }
  }
}

class fileService extends Thread {

  Socket socket;
  Object wait;
  byte[] fileInfo;
  int fileLength;

  public fileService(Socket socket, Object wait, int fileLength) {
    this.socket = socket;
    this.wait = wait;
    this.fileLength = fileLength;
  }

  @Override
  public void run() {
    try {
      synchronized (wait) {
        InputStream inputStream = socket.getInputStream();

        File file = new File("C:\\Users\\DELL\\Desktop\\new1.docx");
        file.canWrite();
        fileInfo = new byte[(int) fileLength];
        inputStream.read(fileInfo);
        System.out.println(Arrays.toString(fileInfo));

        //                ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //
        //                byte[] buffer = new byte[1024];
        //                int bytesRead = 0;
        //                while ((bytesRead = inputStream.read(buffer)) != -1) {
        //                    baos.write(buffer, 0, bytesRead);
        //                }
        //                byte[] fileData = baos.toByteArray();
        //                baos.close();
        //                inputStream.close();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(fileInfo);
        fileOutputStream.flush();
        fileOutputStream.close();
//
//                socket.close();

        wait.notifyAll();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public byte[] getFileInfo() {
    return fileInfo;
  }
}
