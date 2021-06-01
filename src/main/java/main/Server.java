package main;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


class ServerSomthing extends Thread {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Gson gson = new Gson();
    private String nickname;
    Date time;
    SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm:ss");
    String dtime;

    public String getNickname() {
        return nickname;
    }


    public ServerSomthing(Socket socket) throws IOException {
        this.socket = socket;

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        start();
    }

    @Override
    public void run() {
        String json;
        try {
            while (true) {
                json = in.readUTF();
                Message message = gson.fromJson(json, Message.class);
                Message sendMessage;
                String sendJson;
                if (message.getType() == Message.MessageType.LOGIN) {
                    nickname = message.getBody();
                    sendMessage = new Message(Message.MessageType.INFO, "Hello " + nickname);
                    sendJson = gson.toJson(sendMessage);
                    sendAll(sendJson);
                }
                if (message.getType() == Message.MessageType.REQUEST) {
                    StringBuilder userList = new StringBuilder();
                    userList.append("User list:").append("\n");
                    for (ServerSomthing vr : Server.serverList) {
                        userList.append(vr.getNickname()).append("\n");
                    }
                    sendMessage = new Message(Message.MessageType.INFO, userList.toString());
                    sendJson = gson.toJson(sendMessage);
                    send(sendJson);
                }
                if (message.getType() == Message.MessageType.MESSAGE) {
                    time = new Date();
                    dtime = dt1.format(time);
                    sendMessage = new Message(Message.MessageType.MESSAGE, dtime + " " + nickname + " " + message.getBody());
                    sendJson = gson.toJson(sendMessage);
                    sendAll(sendJson);
                }
                if (message.getType() == Message.MessageType.LOGOUT) {
                    time = new Date();
                    dtime = dt1.format(time);
                    sendMessage = new Message(Message.MessageType.INFO, dtime + " " + nickname + " " + "left the chat");
                    sendJson = gson.toJson(sendMessage);
                    Server.serverList.remove(this);
                    this.socket.close();
                    in.close();
                    out.close();
                    sendAll(sendJson);
                    break;
                }
            }
        } catch (IOException e) {
            this.downService();
        }
    }


    private void sendAll(String json) {
        for (ServerSomthing vr : Server.serverList) {
            vr.send(json);
        }
    }

    private void send(String msg) {
        try {
            out.writeUTF(msg);
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
                for (ServerSomthing vr : Server.serverList) {
                    if (vr.equals(this)) vr.interrupt();
                    Server.serverList.remove(this);
                }
            }
        } catch (IOException ignored) {
        }
    }
}


public class Server {

    public static final int PORT = 8080;
    public static List<ServerSomthing> serverList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT,50,InetAddress.getLocalHost());
        System.out.println("Server Started");
        try {
            while (true) {
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerSomthing(socket));
                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}