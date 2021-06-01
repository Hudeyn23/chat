package main;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;


class ClientThread {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private BufferedReader inputUser;
    private final Gson gson = new Gson();

    public ClientThread(String addr, int port) {
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Socket failed");
        }
        try {

            inputUser = new BufferedReader(new InputStreamReader(System.in));
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            this.pressNickname();
            new ReadMsg().start();
            new WriteMsg().start();
        } catch (IOException e) {
            ClientThread.this.downService();
        }

    }


    private void pressNickname() {
        System.out.print("Press your nick: ");
        try {

            String nickname = inputUser.readLine();
            String json = gson.toJson(new Message(Message.MessageType.LOGIN, nickname));
            out.writeUTF(json);
            out.flush();
        } catch (IOException ignored) {
        }

    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {
        }
    }

    private class ReadMsg extends Thread {
        @Override
        public void run() {
            String str;
            try {
                while (true) {
                    str = in.readUTF();
                    Message message = gson.fromJson(str, Message.class);
                    System.out.println(message.getBody());
                }
            } catch (IOException e) {
                downService();
            }
        }
    }

    public class WriteMsg extends Thread {

        @Override
        public void run() {
            while (true) {
                String userWord;
                Message message;
                String json;
                try {
                    userWord = inputUser.readLine();
                    if (userWord.equals("/exit")) {
                        message = new Message(Message.MessageType.LOGOUT, "");
                        json = gson.toJson(message);
                        out.writeUTF(json);
                        out.flush();
                        downService();
                        break;
                    }
                    if (userWord.equals("/list")) {
                        message = new Message(Message.MessageType.REQUEST, "");
                        json = gson.toJson(message);
                        out.writeUTF(json);
                    } else {
                        message = new Message(Message.MessageType.MESSAGE, userWord);
                        json = gson.toJson(message);
                        out.writeUTF(json);
                    }
                    out.flush();
                } catch (IOException e) {
                    ClientThread.this.downService();

                }

            }
        }
    }
}

public class Client {

    public static String ipAddr = "192.168.31.169";
    public static int port = 8080;

    public static void main(String[] args) {
        new ClientThread(ipAddr, port);
    }
}