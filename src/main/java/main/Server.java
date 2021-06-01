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
import java.util.LinkedList;

/**
 * проект реализует консольный многопользовательский чат.
 * вход в программу запуска сервера - в классе Server.
 *
 * @author izotopraspadov, the tech
 * @version 2.0
 */

class ServerSomthing extends Thread {

    private Socket socket; // сокет, через который сервер общается с клиентом,
    // кроме него - клиент и сервер никак не связаны
    private ObjectInputStream in; // поток чтения из сокета
    private ObjectOutputStream out; // поток завписи в сокет
    private Gson gson = new Gson();
    private String nickname;
    Date time;
    SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm:ss");
    String dtime;

    public String getNickname() {
        return nickname;
    }

    /**
     * для общения с клиентом необходим сокет (адресные данные)
     *
     * @param socket
     * @throws IOException
     */

    public ServerSomthing(Socket socket) throws IOException {
        this.socket = socket;
        // если потоку ввода/вывода приведут к генерированию искдючения, оно проброситься дальше
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        start(); // вызываем run()
    }

    @Override
    public void run() {
        String json;
        try {
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
                        for (ServerSomthing vr : Server.serverList) {
                            userList.append(vr.getNickname() + "\n");
                        }
                        sendMessage = new Message(Message.MessageType.INFO, userList.toString());
                        sendJson = gson.toJson(sendMessage);
                        send(sendJson);
                    }
                    if (message.getType() == Message.MessageType.MESSAGE) {
                        time = new Date(); // текущая дата
                        dtime = dt1.format(time); // время
                        sendMessage = new Message(Message.MessageType.MESSAGE, dtime + " " + nickname + " " + message.getBody());
                        sendJson = gson.toJson(sendMessage);
                        sendAll(sendJson);
                    }
                    if (message.getType() == Message.MessageType.LOGOUT) {
                        time = new Date(); // текущая дата
                        dtime = dt1.format(time); // время
                        sendMessage = new Message(Message.MessageType.INFO, dtime + " " + nickname + " " + "left the chat");
                        sendJson = gson.toJson(sendMessage);
                        Server.serverList.remove(this);
                        this.socket.close();
                        sendAll(sendJson);
                        break;
                    }
                    if (json.equals("stop")) {
                        this.downService(); // харакири
                        break; // если пришла пустая строка - выходим из цикла прослушки
                    }
                }
            } catch (NullPointerException ignored) {
            }


        } catch (IOException e) {
            this.downService();
        }
    }

    private void sendAll(String json) {
        for (ServerSomthing vr : Server.serverList) {
            vr.send(json); // отослать принятое сообщение с привязанного клиента всем остальным влючая его
        }
    }

    private void send(String msg) {
        try {
            out.writeUTF(msg + "\n");
            out.flush();
        } catch (IOException ignored) {
        }

    }

    /**
     * закрытие сервера
     * прерывание себя как нити и удаление из списка нитей
     */
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

/**
 * класс хранящий в ссылочном приватном
 * списке информацию о последних 10 (или меньше) сообщениях
 */


public class Server {

    public static final int PORT = 8080;
    public static LinkedList<ServerSomthing> serverList = new LinkedList<>(); // список всех нитей - экземпляров
    // сервера, слушающих каждый своего клиента

    /**
     * @param args
     * @throws IOException
     */

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(6770,50, InetAddress.getByName("192.168.31.169"));
        System.out.println("Server Started");
        try {
            while (true) {
                // Блокируется до возникновения нового соединения:
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerSomthing(socket)); // добавить новое соединенние в список
                } catch (IOException e) {
                    // Если завершится неудачей, закрывается сокет,
                    // в противном случае, нить закроет его:
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}