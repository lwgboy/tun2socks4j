package util;

import com.sun.jna.Pointer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PacketUtil {

    /*public static void fakeICMP(Pointer packet, int packetSize, Pointer session) {
        log.info("get icmp!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        //源地址
        packet.write(12, new int[]{DllLib.WinSock.lib.htonl((192 << 24) | (168 << 16) | (0 << 8) | (109 << 0))}, 0, 1);
        //目标地址
        packet.write(16, new int[]{DllLib.WinSock.lib.htonl((192 << 24) | (168 << 16) | (0 << 8) | (111 << 0))}, 0, 1);
        packet.write(20, new byte[]{0}, 0, 1);

        //清空原checksum
        packet.write(10, new short[]{0}, 0, 1);
        short ipChecksum = IPHeader.checksum(packet.getByteArray(0, 20), 20);
        packet.write(10, new short[]{ipChecksum}, 0, 1);

        packet.write(22, new short[]{0}, 0, 1);
        short todoCheckSum = IPHeader.checksum(packet.share(20).getByteArray(0, 40), 40);
        packet.write(22, new short[]{todoCheckSum}, 0, 1);

        Pointer outgoingPacket = DllLib.Wintun.lib.WintunAllocateSendPacket(session, packetSize);
        outgoingPacket.write(0, packet.getByteArray(0, packetSize), 0, packetSize);
        //printHexs(packet.getByteArray(0,packetSize));
        DllLib.Wintun.lib.WintunSendPacket(session, outgoingPacket);
    }*/


    public static boolean isSyn(Pointer packet) {
        int tcpOffset = (packet.getByte(0) & 0xf) * 4;
        return (packet.getByte(tcpOffset + 13) & 0b10) == 0b10;
    }

    public static boolean isFin(Pointer packet) {
        int tcpOffset = (packet.getByte(0) & 0xf) * 4;
        return (packet.getByte(tcpOffset + 13) & 0b1) == 0b1;
    }

    @SneakyThrows
    public static InetAddress getSrcAddr(Pointer packet) {
        return Inet4Address.getByAddress(packet.getByteArray(12, 4));
    }

    @SneakyThrows
    public static InetAddress getDstAddr(Pointer packet) {
        return Inet4Address.getByAddress(packet.getByteArray(16, 4));
    }

    public static int getSrcPort(Pointer packet) {
        int tcpOffset = (packet.getByte(0) & 0xf) * 4;
        return (Byte.toUnsignedInt(packet.getByte(tcpOffset)) << 8)
                + Byte.toUnsignedInt(packet.getByte(tcpOffset + 1));
    }

    public static int getDstPort(Pointer packet) {
        int tcpOffset = (packet.getByte(0) & 0xf) * 4;
        return (Byte.toUnsignedInt(packet.getByte(tcpOffset + 2)) << 8)
                + Byte.toUnsignedInt(packet.getByte(tcpOffset + 3));
    }

    public static int getTcpOffset(Pointer packet) {
        return (packet.getByte(0) & 0xf) * 4;
    }

    public static int getTcpHeaderLen(Pointer packet) {
        //return packet.getByte(getTcpOffset(packet) + 12) >>> 4; 无符号右移会先将数字提升为int，在做转换，所以再本来就是负鼠的情况下结果不正确
        return (Byte.toUnsignedInt(packet.getByte(getTcpOffset(packet) + 12)) >>> 4) * 4;
    }

    public static int getTcpPayloadLen(Pointer packet) {
        //ip总长度-ipheaderlen-tcpheaderlen
        return getIpTotlen(packet) - getIpHeaderLen(packet) - getTcpHeaderLen(packet);
    }

    public static byte[] getTcpPayload(Pointer packet) {
        //ip总长度-ipheaderlen-tcpheaderlen
        return packet.getByteArray(getIpHeaderLen(packet) + getTcpHeaderLen(packet), getTcpPayloadLen(packet));
    }

    public static short getIpTotlen(Pointer packet) {
        return (short) ((Byte.toUnsignedInt(packet.getByte(2)) << 8)
                + (Byte.toUnsignedInt(packet.getByte(3)) << 0));
    }

    public static int getIpHeaderLen(Pointer packet) {
        return ((byte) (packet.getByte(0) & 0b1111)) * 4;
    }

    public static int getTcpAck(Pointer packet) {
        int tcpOffset = (packet.getByte(0) & 0xf) * 4;
        //todo 是否有大小端的问题
        return (int) ((Byte.toUnsignedLong(packet.getByte(tcpOffset + 8)) << 24)
                + (Byte.toUnsignedLong(packet.getByte(tcpOffset + 9)) << 16)
                + (Byte.toUnsignedLong(packet.getByte(tcpOffset + 10)) << 8)
                + (Byte.toUnsignedLong(packet.getByte(tcpOffset + 11)) << 0));
    }

    public static int getTcpSeq(Pointer packet) {
        int tcpOffset = (packet.getByte(0) & 0xf) * 4;
        //todo 是否有大小端的问题
        //windows是小段，网络传送要求大端，packet.getInt()是native调用的windows的方法，即虽然送来的packet是大端，但是是用小端读取的方式，所以错的
        return (int) ((Byte.toUnsignedLong(packet.getByte(tcpOffset + 4)) << 24)
                + (Byte.toUnsignedLong(packet.getByte(tcpOffset + 5)) << 16)
                + (Byte.toUnsignedLong(packet.getByte(tcpOffset + 6)) << 8)
                + (Byte.toUnsignedLong(packet.getByte(tcpOffset + 7)) << 0));
    }

    public static int getTcpFlag(Pointer packet) {
        return Byte.toUnsignedInt(packet.getByte((packet.getByte(0) & 0xf) * 4 + 13));
    }

    public static byte getIpVersion(Pointer packet) {
        return (byte) (packet.getByte(0) >> 4);
    }

    public static byte getIpProto(Pointer packet) {
        return packet.getByte(9);
    }

    public static List<Byte> int2Bytes(int n) {
        List<Byte> list = new ArrayList<>();
        list.add((byte) (n & 0xff));
        list.add((byte) (n >> 8 & 0xff));
        list.add((byte) (n >> 16 & 0xff));
        list.add((byte) (n >> 24 & 0xff));
        return list;
    }

    public static List<Byte> short2Bytes(short n) {
        List<Byte> list = new ArrayList<>();
        list.add((byte) (n & 0xff));
        list.add((byte) (n >> 8 & 0xff));
        return list;
    }

    public static short checkSum(byte[] p) {
        int Len = p.length;
        int Sum = 0;
        int offset = 0;
        for (; Len > 1; Len -= 2, offset += 2) {
            Sum += (Byte.toUnsignedInt(p[offset]) + (Byte.toUnsignedInt(p[offset + 1]) << 8));
            //   log.info("sum{}:{}", offset, Sum);
        }
        if (Len != 0) {
            Sum += Byte.toUnsignedInt(p[offset]);
        }
        Sum = (Sum >>> 16) + (Sum & 0xffff);
        Sum = Sum + (Sum >>> 16);
        return (short) ~Sum;
    }
}
