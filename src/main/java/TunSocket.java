import dll.DllLib;
import entity.TCPHeader;
import entity.TCPHeaderWithOptions;
import entity.TCPPacket;
import lombok.Builder;
import lombok.Data;
import util.PacketUtil;

import java.net.InetAddress;

@Data
@Builder
public class TunSocket {
    private WintunAdapter adapter;
    private TunPacket tunPacket;

    private int dstSeq;
    private int dstAck;


    //"52.196.9.164", "52.196.0.0/16"
    public static boolean isInRange(String ip, String cidr) {
        String[] ips = ip.split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24)
                | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) | Integer.parseInt(ips[3]);
        int type = Integer.parseInt(cidr.replaceAll(".*/", ""));
        int mask = 0xFFFFFFFF << (32 - type);
        String cidrIp = cidr.replaceAll("/.*", "");
        String[] cidrIps = cidrIp.split("\\.");
        int cidrIpAddr = (Integer.parseInt(cidrIps[0]) << 24)
                | (Integer.parseInt(cidrIps[1]) << 16)
                | (Integer.parseInt(cidrIps[2]) << 8)
                | Integer.parseInt(cidrIps[3]);

        return (ipAddr & mask) == (cidrIpAddr & mask);
    }


    public void updateDstAck() {
        dstAck = (int) (tunPacket.getUnsignedSrcSeq() + tunPacket.getUnsignedSrcLen());
    }

    public void updateDstSeq() {
        dstSeq = tunPacket.getSrcAck();
    }

    public InetAddress getDstAddr() {
        return tunPacket.getDstAddr();
    }

    public InetAddress getSrcAddr() {
        return tunPacket.getSrcAddr();
    }

    public int getDstPort() {
        return tunPacket.getDstPort();
    }

    public int getSrcPort() {
        return tunPacket.getSrcPort();
    }

    public synchronized void updateSeqAndAck(TunPacket tunPacket) {
        //收到了新的报文，更新dstack和seq
        setTunPacket(tunPacket);
        updateDstAck();
        updateDstSeq();
    }

    public void sendPshToMxd(byte[] data, WintunAdapter adapter) {
        //组tcp报文
        int ipHeaderLen = 20, tcpHeaderLen = 20;
        int totLen = data.length + ipHeaderLen + tcpHeaderLen;//tcp payload(tcpPayloadLen)+ipheader(20)+tcpHeaderLen(5x4=24)

        TCPHeader src = new TCPPacket();
        src.setFlag((byte) (Constant.TCP_FLAG_ACK | Constant.TCP_FLAG_PSH));
        src.setData(data);
        src.setDataOffset_rsv((byte) ((tcpHeaderLen / 4) << 4));
        src.setTotalLen(DllLib.WinSock.lib.htons(totLen));
        initTcpHeader(src);
        src.checkSum(totLen - ipHeaderLen);

        adapter.sendPacket(new TCPPacket(DllLib.Wintun.lib.WintunAllocateSendPacket(adapter.session, totLen), src));
    }


    public void sendAckToMxd(WintunAdapter adapter) {
        //组tcp报文
        int ipHeaderLen = 20, tcpHeaderLen = 20;
        int totLen = ipHeaderLen + tcpHeaderLen;//tcp payload(tcpPayloadLen)+ipheader(20)+tcpHeaderLen(5x4=24)

        TCPHeader src = new TCPHeader();
        src.setFlag(Constant.TCP_FLAG_ACK);
        src.setDataOffset_rsv((byte) ((tcpHeaderLen / 4) << 4));
        src.setTotalLen(DllLib.WinSock.lib.htons(totLen));
        initTcpHeader(src);
        src.checkSum(totLen - ipHeaderLen);

        //todo 使用结构体来做
        adapter.sendPacket(new TCPHeader(DllLib.Wintun.lib.WintunAllocateSendPacket(adapter.session, totLen), src));
    }

    private void initTcpHeader(TCPHeader src) {
        src.setSrcAddr(getDstAddr().getAddress());
        src.setDstAddr(getSrcAddr().getAddress());
        src.setIpCheckSum(PacketUtil.checkSum(src.getIpCheckSumArray()));
        src.setSrcPort(DllLib.WinSock.lib.htons(getDstPort()));
        src.setDstPort(DllLib.WinSock.lib.htons(getSrcPort()));
        src.setSeq(DllLib.WinSock.lib.htonl(getDstSeq()));
        src.setAck(DllLib.WinSock.lib.htonl(getDstAck()));
    }

    public void sendSynAckToMxd() {
        //组tcp报文
        byte[] data = new byte[0];
        int ipHeaderLen = 20, tcpHeaderLen = 20;
        byte[] tcpOptions = {0x02, 0x04, (byte) 0xff, (byte) 0xd7, 1, 0x03, 0x03, 0x08, 1, 1, 0x04, 0x02};
        int totLen = data.length + ipHeaderLen + tcpHeaderLen + tcpOptions.length;//tcp payload(tcpPayloadLen)+ipheader(20)+tcpHeaderLen(5x4=24)

        TCPHeader src = new TCPHeaderWithOptions();
        src.setFlag((byte) (Constant.TCP_FLAG_ACK | Constant.TCP_FLAG_SYN));
        src.setTcpOptions(tcpOptions);
        src.setDataOffset_rsv((byte) (((tcpHeaderLen + tcpOptions.length) / 4) << 4));
        src.setTotalLen(DllLib.WinSock.lib.htons(totLen));
        initTcpHeader(src);
        src.checkSum(totLen - ipHeaderLen);

        adapter.sendPacket(new TCPHeaderWithOptions(DllLib.Wintun.lib.WintunAllocateSendPacket(adapter.session, totLen), src));
    }

}