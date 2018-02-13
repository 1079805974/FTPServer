package core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FTPServer {
    private ServerSocket cmdServer;
    private ArrayList<ServerThread> socketList = new ArrayList<>();
    private File path;

    //private
    public FTPServer(String path) throws IOException {
        cmdServer = new ServerSocket(21);
        this.path = new File(path);
        if (!this.path.isDirectory()) {
            throw new IOException("无效路径！");
        }
        new Thread(() -> {
            while (true) {
                try {
                    Socket cmdSocket = cmdServer.accept();
                    System.out.println(cmdSocket.getInetAddress().getHostAddress());
                    ServerThread serverThread = new ServerThread(cmdSocket);
                    new Thread(serverThread).start();
                    socketList.add(serverThread);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public FTPServer(int port, String path) throws Exception {
        cmdServer = new ServerSocket(port);
        this.path = new File(path);
        if (!this.path.isDirectory()) {
            throw new IOException("无效路径！");
        }

    }

    public static void main(String args[]) throws IOException {
        FTPServer server = new FTPServer("C:\\");
       /* ServerSocket dataServer=new ServerSocket(0,1);
        dataServer.getLocalSocketAddress();
       System.out.println(dataServer.getInetAddress().toString());
        System.out.println(dataServer.getLocalPort());*/

    }
}
