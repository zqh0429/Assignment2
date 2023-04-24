package cn.edu.sustech.cs209.chatting.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

  Socket socket;
  String name;
  BufferedReader in;
  PrintWriter out;


  public static void main(String[] args) {
    launch();
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
    VBox pane = fxmlLoader.load();
    Controller controller = fxmlLoader.getController();
    name = controller.username;
    socket = Controller.socket;
    stage.setScene(new Scene(pane));
    stage.setTitle("Chatting Client");
    stage.setOnCloseRequest(event -> {
      try {
        socket.close();
      } catch (IOException e) {
        throw new RuntimeException();
      }
    });
    stage.show();
  }

  public Socket getSocket() {
    return socket;
  }

  public String getName() {
    return name;
  }

  public BufferedReader getIn() {
    return in;
  }

  public PrintWriter getOut() {
    return out;
  }
}
