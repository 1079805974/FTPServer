package core;

import utils.Utils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class ServerThread implements Runnable {
    private static String defaultDir = "F:\\";
    private Socket cmdSocket, dataSocket;
    private ServerSocket dataServer;
    private PrintWriter cmdWriter;
    private BufferedReader cmdReader;
    private MessageHandler messageHandler;
    private InetAddress remoteIP;
    private InputStream dataInputStream;
    private OutputStream dataOutputStream;
    private String username;
    private String password;
    private boolean isLogin;
    private HashMap<String, String> userdata;
    private String currentDir = defaultDir;
    private int restartSize=0;
    public ServerThread(Socket socket) {
        cmdSocket = socket;
        remoteIP = socket.getInetAddress();
        messageHandler = new MessageHandler(this);
        getUserData();
    }

    public ServerThread(Socket socket, String dir) {
        defaultDir = dir;
        cmdSocket = socket;
        remoteIP = socket.getInetAddress();
        messageHandler = new MessageHandler(this);
        getUserData();
    }

    public static String getDefaultDir() {
        return defaultDir;
    }

    public static void setDefaultDir(String defaultDir) {
        ServerThread.defaultDir = defaultDir;
    }

    public boolean isLogin() {
        return this.isLogin;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean verify() {
        isLogin = false;
        if (username == null) return false;
        if (password == null) return false;
        if (!userdata.containsKey(username)) {
            return false;
        }
        if (!userdata.get(username).equals(password)) {
            return false;
        }
        isLogin = true;
        return true;
    }

    private void getUserData() {
        File file = new File("loginInfos");
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        userdata = new HashMap<>();
        while (scanner.hasNext()) {
            String usernameStored = scanner.nextLine();
            System.out.println(usernameStored);
            String passwordStored = scanner.nextLine();
            System.out.println(passwordStored);
            userdata.put(usernameStored, passwordStored);
        }
    }

    @Override
    public void run() {
        try {
            InputStream clientInput = cmdSocket.getInputStream();
            OutputStream clientOutput = cmdSocket.getOutputStream();
            cmdWriter = new PrintWriter(clientOutput);
            cmdReader = new BufferedReader(new InputStreamReader(clientInput));
            cmd(MessageHandler.CONNECT_OK);
            String message;
            while ((message = cmdReader.readLine()) != null) {
                System.out.println(message);
                messageHandler.handle(message);
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("客户端断开了连接！");
        }
    }

    public void sendFile(String dir) throws IOException {
        String filename = getFullDir(currentDir,dir);
        System.out.println(filename);
        File file = new File(filename);
        if(!file.exists()){
            cmd(MessageHandler.FILE_NOT_FOUND);
            return;
        }
        cmd(MessageHandler.RETR_START);
        InputStream fileIS = new FileInputStream(file);
        byte flush[] = new byte[1024];
        int len = 0;
        if(!(restartSize>file.length()))
            fileIS.skip(restartSize);
        else{
            fileIS.skip(file.length());
        }
        while (0 <= (len = fileIS.read(flush))) {
            dataOutputStream.write(flush, 0, len);
        }
        dataOutputStream.flush();
        fileIS.close();
        cmd(MessageHandler.RETR_END);
        restartSize=0;
    }

    public void cmd(String cmd) throws IOException {
        cmdWriter.println(cmd);
        cmdWriter.flush();
    }

    public String startPASV() throws IOException {
        dataServer = new ServerSocket(0, 1);
        String IP = remoteIP.getHostAddress().replace(".", ",");
        int portInt = dataServer.getLocalPort();
        int portPart1 = portInt / 256;
        int portPart2 = portInt % 256;
        String portStr = portPart1 + "," + portPart2;
        return "(" + IP + "," + portStr + ")";
    }

    public void waitForDataSocket() throws IOException {
        dataSocket = dataServer.accept();
        dataInputStream = dataSocket.getInputStream();
        dataOutputStream = dataSocket.getOutputStream();
    }

    public void endPASV() throws IOException {
        dataInputStream.close();
        dataOutputStream.close();
        dataSocket.close();
        dataSocket = null;
        dataOutputStream = null;
        dataInputStream = null;
    }

    public String getFullDir(final String currentDir,String appendedDir){
        String result = currentDir;
        if (appendedDir.startsWith(defaultDir)) {
            result = appendedDir;
        } else {
            if (appendedDir.startsWith("\\")) {
                result = currentDir + appendedDir;
            } else {
                result = currentDir + "\\" + appendedDir;
            }
        }
        return result;
    }

    public boolean hasPASV() {
        return dataSocket != null;
    }


    public void sendDirInfo(String dir) throws IOException {
        File localDir = new File(dir);
        System.out.println(dir);
        File fileList[] = localDir.listFiles();
        if (fileList == null) {
            cmd(MessageHandler.FILE_NOT_FOUND);
            return;
        }
        PrintWriter dataWriter = new PrintWriter(dataOutputStream);
        for (File file : fileList) {
            String infoLine = Utils.getFileInfo(file);
            dataWriter.println(infoLine);
            dataWriter.flush();
        }
        System.out.println("读文件完成");
    }

    public void sendDirInfo() throws IOException {
        sendDirInfo(currentDir);
    }

    public boolean deleteFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return false;
        }
        if (!file.isFile()) {
            return false;
        }
        if (!file.delete()) {
            return false;
        }
        return true;
    }

    public boolean changeWorkDir(String dir) {
       String fullDir = getFullDir(currentDir,dir);
        System.out.println(fullDir);
        File file = new File(fullDir);
        if(!file.isDirectory()){
            return false;
        }
        currentDir = fullDir;
        return true;
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }

    public void saveFile(String filename) throws IOException {
        cmd(MessageHandler.STOR_READY);
        String filePath = getFullDir(currentDir,filename);
        RandomAccessFile file = new RandomAccessFile(filePath,"rw");
        file.seek(restartSize);
        byte flush[] = new byte[1024];
        int len = 0;
        while (0 <= (len = dataInputStream.read(flush))) {
            file.write(flush, 0, len);
        }
        file.close();
        cmd(MessageHandler.STOR_END);
        restartSize=0;
    }

    public void restart(int size) {
        restartSize = size;
    }
}
