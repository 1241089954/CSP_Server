package com.zjicm.csp;

import com.zjicm.csp.PlayerAction.ACTION;
import com.zjicm.csp.Result.TYPE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    //定义相关的参数,端口,存储Socket连接的集合,ServerSocket对象以及线程池
    private static final int PORT = 12345;
    private List<SocketInfo> mList = new ArrayList<>();
    private ServerSocket serverSocket = null;
    private ExecutorService myExecutorService = null;
    private Socket client = null;
    private ArrayList<Table> tables;
    private boolean id = false;

    private String[] strings = new String[]{
            "剪刀",
            "石头",
            "布"
    };


    public static void main(String[] args) throws UnknownHostException {
        System.out.println("服务器IP地址为：" + InetAddress.getLocalHost().getHostAddress());
        new Server();
    }

    private Server() {
        tables = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Table table = new Table("第" + i + "桌", false, "", false, "");
            tables.add(table);
        }

        new Thread(() -> {
            while (true) {
                for (Table table : tables) {
//                    System.out.println(table.toString());
                    if (table.isInGame()) continue;
                    if (!table.getPlayer1IP().equals("") && !table.getPlayer2IP().equals("")) {
                        for (SocketInfo info : mList) {
                            if (info.getSocket().getInetAddress().toString().equals(table.getPlayer1IP())) {
                                try {
                                    Thread.sleep(100);
                                    Table t = new Table();
                                    ArrayList<Table> list = new ArrayList<>();
                                    list.add(t);
                                    Result<ArrayList<Table>> result = new Result<>(TYPE.Game_START, "", "在线:" + mList.size(), list);
                                    info.getObjectOutputStream().writeObject(result);
                                    continue;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (info.getSocket().getInetAddress().toString().equals(table.getPlayer2IP())) {
                                try {
                                    Thread.sleep(100);
                                    Table t = new Table();
                                    ArrayList<Table> list = new ArrayList<>();
                                    list.add(t);
                                    Result<ArrayList<Table>> result = new Result<>(TYPE.Game_START, "", "在线:" + mList.size(), list);
                                    info.getObjectOutputStream().writeObject(result);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        table.setInGame(true);
                        System.out.println(table.getTableName() + "开始游戏\n");
                    } else {

                    }
                }
            }
        }).start();

        try {
            serverSocket = new ServerSocket(PORT);
            //创建线程池
            myExecutorService = Executors.newCachedThreadPool();
            System.out.println("服务器启动完毕" + "\n");
            while (true) {
                client = serverSocket.accept();
                SocketInfo info = new SocketInfo(client);
                mList.add(info);
                myExecutorService.execute(new Service(info));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Service implements Runnable {
        private SocketInfo info;
        private ObjectOutputStream objectOutputStream = null;
        private ObjectInputStream objectInputStream = null;
        private PlayerAction playerAction = null;

        private Service(SocketInfo info) {
            this.info = info;
            try {
                objectOutputStream = new ObjectOutputStream(info.getSocket().getOutputStream());
                objectInputStream = new ObjectInputStream(info.getSocket().getInputStream());
                info.setObjectInputStream(objectInputStream);
                info.setObjectOutputStream(objectOutputStream);
                System.out.println("用户:" + this.info.getSocket().getInetAddress() + "加入," + "当前在线人数:" + mList.size());
                Result<ArrayList<Table>> result = new Result<>(TYPE.TABLE_INFO, "", "在线:" + mList.size(), tables);
                this.sendMessageToAllClients(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (info.getSocket() == null || objectInputStream == null) {
                        continue;
                    }
                    if (!info.getSocket().isClosed() && info.getSocket().isConnected() && !info.getSocket().isInputShutdown()) {
                        String a = "";
                        if ((playerAction = (PlayerAction) objectInputStream.readObject()) != null) {
//                            System.out.println("收到消息" + playerAction.isIntoGame() + "  " + playerAction.getPosition() + "  " + playerAction.getSeat());
                            if (playerAction.getAction() == ACTION.InOrOut) {
                                for (Table table : tables) {
                                    if (table.getTableName().equals("第" + (playerAction.getPosition() + 1) + "桌")) {
                                        if (playerAction.isIntoGame() && playerAction.getSeat() == 1) {//玩家加入第一桌
                                            table.setPlayer1Online(true);
                                            table.setPlayer1IP(this.info.getSocket().getInetAddress().toString());
                                            a = "加入游戏";
                                        } else if (playerAction.isIntoGame() && playerAction.getSeat() == 2) {//玩家加入第二桌
                                            table.setPlayer2Online(true);
                                            table.setPlayer2IP(this.info.getSocket().getInetAddress().toString());
                                            a = "加入游戏";
                                        } else if (!playerAction.isIntoGame() && playerAction.getSeat() == 1) {//玩家退出第一桌
                                            table.setPlayer1Online(false);
                                            table.setPlayer1IP("");
                                            table.setInGame(false);
                                            a = "退出游戏";
                                        } else if (!playerAction.isIntoGame() && playerAction.getSeat() == 2) {//玩家退出第二桌
                                            table.setPlayer2Online(false);
                                            table.setPlayer2IP("");
                                            table.setInGame(false);
                                            a = "退出游戏";
                                        }
                                        ArrayList<Table> tl = new ArrayList<>();
                                        Table t = new Table(table.getTableName(), table.isPlayer1Online(), table.getPlayer1IP(), table.isPlayer2Online(), table.getPlayer2IP());
                                        tl.add(t);
                                        Result<ArrayList<Table>> result = new Result<>(TYPE.TABLE_UPDATE, "玩家" + info.getSocket().getInetAddress() + a, "在线:" + mList.size(), tl);
                                        System.out.println("玩家" + info.getSocket().getInetAddress() + a);
                                        this.sendMessageToAllClients(result);
                                    }
                                }
                            } else if (playerAction.getAction() == ACTION.CSP) {
                                System.out.println("收到消息:" + strings[playerAction.getCsp()]);
                                sendMessageToOther(this.info.getSocket(), playerAction.getCsp());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //通知所有客户端
        private void sendMessageToAllClients(Result<ArrayList<Table>> result) throws IOException {
            for (SocketInfo info : mList) {
                if (info.getSocket() != null && info.getObjectOutputStream() != null) {
                    if (info.getSocket().isConnected() && !info.getSocket().isOutputShutdown() && !info.getSocket().isClosed()) {
                        info.getObjectOutputStream().writeObject(result);
                        info.getObjectOutputStream().flush();
                        System.out.println("消息成功返回至" + info.getSocket().getInetAddress());
                        System.out.println("----------------------------------------");
                    }
                }
            }
        }
    }

    private void sendMessageToOther(Socket socket, int csp) {
        int a = 0;
        String IP = socket.getInetAddress().toString();
        String targetIP = "";
        for (Table table : tables) {
            if (table.getPlayer1IP().equals(IP)) {
                targetIP = table.getPlayer2IP();
                a++;
            }
            if (table.getPlayer2IP().equals(IP)) {
                targetIP = table.getPlayer1IP();
                a++;
            }
        }
        System.out.println(targetIP + "     " + a);

        for (SocketInfo info : mList) {
            if (info.getSocket().getInetAddress().toString().equals(targetIP)) {
                try {
                    Result<ArrayList<Table>> result = new Result<>(TYPE.CSP_INFO, strings[csp], "在线:" + mList.size(), tables);
                    info.getObjectOutputStream().writeObject(result);
                    System.out.println("向对手发送消息成功");
                    System.out.println("----------------------------------------");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}