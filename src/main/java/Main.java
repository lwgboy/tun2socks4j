import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import util.PacketUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Main {

    private static final WintunAdapter tunAdapter = new WintunAdapter();

    public final static Map<Integer, Socket> socks5Map = new HashMap<>();
    public final static Map<Integer, TunSocket> tunSocketsMap = new HashMap<>();

    public static final byte[] VER = {0x5, 0x1, 0x0};


    @SneakyThrows
    public static void main(String[] args) {
        tunAdapter.initSession();

        while (true) {
            TunPacket tunPacket = tunAdapter.reciveGamePacket();

            Integer socketKey = tunPacket.getSrcPort();
            if (PacketUtil.isSyn(tunPacket.getPacket())) {
                log.info("isSyn(tunPacket.getPacket())");
                //todo 这一步把原报文改了,是否保留原报文
                tunSocketsMap.put(socketKey, connectToMxd(tunPacket));

                socks5Map.put(socketKey, connectToSocks5(0, tunPacket.getDstAddr(), tunPacket.getDstPort()));

                new Socks5Handle(socketKey).forwardS5PacketToMxd();
            } else if (PacketUtil.isFin(tunPacket.getPacket())) {
                log.info("isFin(tunPacket.getPacket())");
                tunAdapter.fin();
            } else if (!tunPacket.isOnlyAck()) {
                log.info("!tunPacket.isOnlyAck()");
                tunSocketsMap.get(socketKey).updateSeqAndAck(tunPacket);

                tunSocketsMap.get(tunPacket.getSocketKey()).sendAckToMxd(tunAdapter);

                tunAdapter.forwardMxdPacketToS5(tunPacket);
            } else if (tunPacket.isOnlyAck()) {
                log.info("tunPacket.isOnlyAck()");
                tunSocketsMap.get(socketKey).updateSeqAndAck(tunPacket);
            }

            tunAdapter.releasePacket(tunPacket);
        }
    }

    public static TunSocket connectToMxd(TunPacket tunPacket) {
        TunSocket rs = TunSocket.builder()
                .adapter(tunAdapter)
                .dstAck((int) (tunPacket.getUnsignedSrcSeq() + 1))
                .dstSeq(0)
                .tunPacket(tunPacket)
                .build();
        rs.sendSynAckToMxd();
        return rs;
    }

    private static Socket connectToSocks5(int localPort, InetAddress appDstAddr, int appDstPort) throws IOException {
        //建立Socket连接，new Socket(服务端ip,端口port);
        Socket clientSocket = new Socket("127.0.0.1", 1080, InetAddress.getByName("127.0.0.1"), localPort);
        //从Socket中得到网络输出流，将数据发送到网络上
        InputStream is = clientSocket.getInputStream();
        OutputStream os = clientSocket.getOutputStream();
        os.write(VER);
        byte[] buff = new byte[255];
        // 接收客户端的支持的方法
        is.read(buff, 0, 2);
        int version = buff[0];
        int methodNum = buff[1];
        if (version != 5) {
            throw new RuntimeException("version must 0X05");
        } else if (methodNum != 0) {
            throw new RuntimeException("method num must equal 0");
        }

        //write方法中只能为byte[]，或者int。
        //若服务端没有read(),则客户端会一直等。即停留在write方法中。

        byte[] dstAddr = appDstAddr.getAddress();
        byte[] CONNECT_OK = {0x5, 0x1, 0x0, 0x1, dstAddr[0], dstAddr[1], dstAddr[2], dstAddr[3], (byte) (appDstPort >> 8), (byte) (appDstPort & 0xff)};
        os.write(CONNECT_OK);
        is.read(buff, 0, 10);
        //todo 校验返回结果是succ
        return clientSocket;
    }

    static class Socks5Handle extends Thread {
        private final Integer socketKey;

        @Override
        @SneakyThrows
        public void run() {
            int len;
            byte[] dataBuffer = new byte[10240];
            InputStream in = Main.socks5Map.get(socketKey).getInputStream();
            while ((len = in.read(dataBuffer)) != -1) {
                Main.tunSocketsMap.get(socketKey).sendPshToMxd(dataBuffer, tunAdapter);
            }
        }

        public Socks5Handle(Integer socketKey) {
            super();
            this.socketKey = socketKey;
        }

        public void forwardS5PacketToMxd() {
            this.start();
        }
    }
}
