package tcptest;

import lombok.SneakyThrows;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static java.net.InetAddress.getByName;

public class MyClient {

    private static final byte[] VER = {0x5, 0x1, 0x0};
    private static final byte[] CONNECT_OK = {0x5, 0x1, 0x0, 0x1, (byte) 202, 80, 104, 24, 8484 >> 8, 8484 & 0xff};

    //服务端用同一个端口，accept一次会产生一个客户端socket对象，客户端的信息存在这个socket里，因此socks5应该有个个map<客户端IP 端口，socks5服务端的对应端口>
    public static void main(String[] args) throws IOException, InterruptedException {
        Socket so = new Socket("127.0.0.1", 4574);

        System.out.println("连接服务器成功");

        //从Socket中得到网络输入流，接收来自网络的数据
        InputStream in = so.getInputStream();
        //从Socket中得到网络输出流，将数据发送到网络上

        OutputStreamWriter out = new OutputStreamWriter(so.getOutputStream());
        for (int i = 0; i < 5; i++) {
            out.write("FFii\n");
            out.flush();
            out.write("\n");
            out.flush();
            out.write("\r");
            out.flush();
        }
    }

    private static Socket getSocket(int localPort) throws IOException {
        //建立Socket连接，new Socket(服务端ip,端口port);
        Socket so = new Socket("127.0.0.1", 1080, InetAddress.getByName("127.0.0.1"), localPort);

        System.out.println("连接服务器成功");

        //从Socket中得到网络输入流，接收来自网络的数据
        InputStream in = so.getInputStream();
        //从Socket中得到网络输出流，将数据发送到网络上
        OutputStream out = so.getOutputStream();

        //write方法中只能为byte[]，或者int。
        //若服务端没有read(),则客户端会一直等。即停留在write方法中。
        out.write(VER);
        //接收服务端发来的数据
        byte[] buffer = new byte[4096];
        //将数据存入bs数组中，返回值为数组的长度
        int len = in.read(buffer);
        /*String host = findHost(buffer, 4, 7);
        int port = findPort(buffer, 8, 9);
        System.out.println("host=" + host + ",port=" + port);*/

        out.write(CONNECT_OK);
        return so;
    }

    public static String findHost(byte[] bArray, int begin, int end) {
        StringBuffer sb = new StringBuffer();
        for (int i = begin; i <= end; i++) {
            sb.append(Integer.toString(0xFF & bArray[i]));
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static int findPort(byte[] bArray, int begin, int end) {
        int port = 0;
        for (int i = begin; i <= end; i++) {
            port <<= 16;
            port += bArray[i];
        }
        return port;
    }
}
