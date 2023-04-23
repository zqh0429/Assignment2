package cn.edu.sustech.cs209.chatting.client;

import java.io.Serializable;

public class FileTransferRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileName;
    private byte[] fileContent;

    public FileTransferRequest(String fileName, byte[] fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

}