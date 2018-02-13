package core;

import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

public class MessageHandler {
    public static final String CONNECT_OK = "220 Welcome to LJR's FTP service.";
    public static final String USER_OK = "331 Please specify the password.";
    public static final String PASS_OK = "230 Login successful.";
    public static final String PASS_ERR = "530 Login incorrect.";
    public static final String PASV_OK = "227 Entering Passive Mode.";
    public static final String RETR_START = "150 Opening ASCII mode data connection for ";
    public static final String RETR_END = "226 File send OK";
    public static final String FILE_NOT_FOUND = "550 Failed to open file.";
    public static final String STOR_READY = "150 Ok to send data.";
    public static final String STOR_END = "226 File receive OK.";
    public static final String NOT_LOGIN = "530 Please login with USER and PASS.";
    public static final String UNKNOWN_CMD = "500 Unknown command.";
    public static final String LIST_START = "150 Here comes the directory listing.";
    public static final String LIST_END = "226 Directory send OK.";
    public static final String NOT_PASV = "425 Use PORT or PASV first.";
    public static final String DELE_ERR = "550 Delete operation failed.";
    public static final String DELE_OK = "250 Delete operation successful.";
    public static final String PWD_OK = "257 ";
    public static final String TYPE_I_OK = "200 Switching to Binary mode.";
    public static final String CWD_OK = "250 Directory successfully changed.";
    public static final String CWD_ERR = "550 Failed to change directory.";
    public static final String REST_OK="350 Restart position accepted";
    public static final String NOT_REALISE = "504 I have no time to do this!!!";
    private String message;
    private ServerThread server;

    MessageHandler(ServerThread server) {
        this.server = server;
    }

    public void handle(String message) throws IOException {
        String messageArray[] = message.split(" ");
        String messageHead = messageArray[0];
        switch (messageHead) {
            case "USER":
                if (messageArray.length > 1)
                    handleUSER(message.substring(5));
                break;
            case "PASS":
                if (messageArray.length > 1)
                    handlePASS(message.substring(5));
                else
                    server.cmd(PASS_ERR);
                break;
            case "PASV":
                handlePASV();
                break;
            case "LIST":
                if (messageArray.length > 1) {
                    handleLIST(message.substring(5));
                } else {
                    handleLIST(null);
                }
                break;
            case "DELE":
                if (messageArray.length > 1)
                    handleDELE(message.substring(5));
                break;
            case "MKD":
                if (messageArray.length > 1)
                    handleMKD(message.substring(4));
                break;
            case "CWD":
                if (messageArray.length > 1)
                    handleCWD(message.substring(4));
                break;
            case "PWD":
                handlePWD();
                break;
            case "REST":
                if (messageArray.length > 1)
                    handleREST(message.substring(5));
                break;
            case "RETR":
                handleRETR(message.substring(5));
                break;
            case "STOR":
                handleSTOR(message.substring(5));
                break;
            case "TYPE":
                handleTYPE(message.substring(5));
                break;
            case "CDUP":
                handleCDUP();
                break;
            default:
                server.cmd(UNKNOWN_CMD);
                break;
        }
    }

    private void handleREST(String size) throws IOException {
        System.out.println(size);
        if(Utils.isInteger(size)){
            server.restart(Integer.valueOf(size));
            server.cmd(REST_OK);
        }
        server.cmd(UNKNOWN_CMD);
    }

    private void handleSTOR(String filename) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        if (!server.hasPASV()) {
            server.cmd(NOT_PASV);
            return;
        }
        server.saveFile(filename);
        server.endPASV();
    }

    private void handleCDUP() throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        server.setCurrentDir(ServerThread.getDefaultDir());
        server.cmd(CWD_OK);
    }

    private void handleCWD(String dir) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        if(server.changeWorkDir(dir))
            server.cmd(CWD_OK);
        else
            server.cmd(CWD_ERR);
    }

    private void handleTYPE(String s) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        switch (s.toUpperCase()) {
            case "I":
                server.cmd(TYPE_I_OK);
                break;
            default:
                server.cmd(NOT_REALISE);
                break;
        }
    }

    private void handlePWD() throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        server.cmd(PWD_OK + server.getCurrentDir());
    }

    private void handleRETR(String filename) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        if (!server.hasPASV()) {
            server.cmd(NOT_PASV);
            return;
        }
        if (filename == null) {
            server.cmd(FILE_NOT_FOUND);
        }
        server.sendFile(filename);
        server.endPASV();
    }

    private void handleUSER(String username) throws IOException {
        if (server.isLogin()) {
            server.cmd(UNKNOWN_CMD);
            return;
        }
        server.cmd(USER_OK);
        server.setUsername(username);
    }

    private void handlePASS(String password) throws IOException {
        if (server.isLogin()) {
            server.cmd(UNKNOWN_CMD);
            return;
        }
        server.setPassword(password);
        if (server.verify()) {
            server.cmd(PASS_OK);
            System.out.println("登陆成功");
        } else {
            server.cmd(PASS_ERR);
            System.out.println("密码错误");
        }
    }

    private void handlePASV() throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        String IPAndPortInfo = server.startPASV();
        server.cmd(PASV_OK + " " + IPAndPortInfo);
        server.waitForDataSocket();
    }

    private void handleLIST(String dir) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        if (!server.hasPASV()) {
            server.cmd(NOT_PASV);
            return;
        }
        server.cmd(LIST_START);
        if (dir == null) {
            server.sendDirInfo();
        } else {
            server.sendDirInfo(dir);
        }
        server.cmd(LIST_END);
        server.endPASV();
    }

    private void handleDELE(String filename) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        if (server.deleteFile(filename)) {
            server.cmd(DELE_OK);
        } else {
            server.cmd(DELE_ERR);
        }
        ;
    }

    private void handleMKD(String substring) throws IOException {
        if (!server.isLogin()) {
            server.cmd(NOT_LOGIN);
            return;
        }
        server.cmd(PWD_OK + server.getCurrentDir());
    }

}
