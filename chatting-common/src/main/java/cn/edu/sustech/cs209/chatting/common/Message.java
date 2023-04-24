package cn.edu.sustech.cs209.chatting.common;

public class Message {

  private String timestamp;

  private String sentBy;

  private String sendTo;

  private String data;

  public Message(String timestamp, String sentBy, String sendTo, String data) {
    this.timestamp = timestamp;
    this.sentBy = sentBy;
    this.sendTo = sendTo;
    this.data = data;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getSentBy() {
    return sentBy;
  }

  public String getSendTo() {
    return sendTo;
  }

  public String getData() {
    return data;
  }
}
