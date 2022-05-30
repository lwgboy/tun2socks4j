package tcptest;

import lombok.SneakyThrows;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static java.net.InetAddress.getByName;

public class Mysever {
    @SneakyThrows
    public static void main(String[] args) throws IOException {
        //建立服务端对象，绑定4574端口。
        ServerSocket serv =new ServerSocket(4574);

        System.out.println("服务器启动成功，等待用户接入");

        //accept()等待用户接入,如果没有用户连接，会一直等待。
        //有客户连接后，accept()方法会返回一个Socket对象，代表客户端
        Socket sc=serv.accept();

        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sc.getInputStream()))
        ) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {//循环读取缓冲区中的数据
                stringBuilder.append(line).append("\n");
                System.out.println("脚本执行信息：" + stringBuilder.toString());
            }
            System.out.println("over");
            //拼接得到脚本进程的异常消息
        }
    }
}
