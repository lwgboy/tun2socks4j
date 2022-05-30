package entity;

import com.sun.jna.Pointer;
import com.sun.jna.Structure.FieldOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import util.PacketUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Source Port          |       Destination Port        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Sequence Number                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Acknowledgment Number                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Data |           |U|A|P|R|S|F|                               |
 * | Offset| Reserved  |R|C|S|S|Y|I|            Window             |
 * |       |           |G|K|H|T|N|N|                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Checksum            |         Urgent Pointer        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Options                    |    Padding    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                             data                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */

@FieldOrder({"srcPort", "dstPort", "seq", "ack", "dataOffset_rsv", "flag", "window", "tcpCheckSum", "urgentPointer"})
@Data
@NoArgsConstructor
@Slf4j
public class TCPHeader extends IPHeader {
    public short srcPort;
    public short dstPort;
    public int seq;
    public int ack;
    public byte dataOffset_rsv;
    public byte flag;
    public short window = (short) 0xffff;
    public short tcpCheckSum;
    public short urgentPointer;

    public void setTcpOptions(byte[] options) {
    }

    public byte[] getTcpOptions() {
        throw new UnsupportedOperationException();
    }

    public void setData(byte[] data) {
    }

    public void checkSum(int tcpLength) {
        this.setTcpCheckSum(tcpChecksum(new byte[0], tcpLength));
    }

    public byte[] getData() {
        throw new UnsupportedOperationException();
    }

    public TCPHeader(Pointer p, TCPHeader src) {
        super(p, src);
        this.srcPort = src.srcPort;
        this.dstPort = src.dstPort;
        this.seq = src.seq;
        this.ack = src.ack;
        this.dataOffset_rsv = src.dataOffset_rsv;
        this.flag = src.flag;
        this.window = src.window;
        this.tcpCheckSum = src.tcpCheckSum;
        this.urgentPointer = src.urgentPointer;
    }

    public short tcpChecksum(byte[] additionArray, int tcpLength) {
        //ip报的数据部分=tcp内容=总长度-ip首部长度
        //(Byte.toUnsignedInt(pointer.getByte(3)) + (Byte.toUnsignedInt(pointer.getByte(2)) << 8))  - ipHeaderLength
        byte[] tmpBytes = new byte[tcpLength + 12];
        //12byte dummy header
        //source ip
        System.arraycopy(this.getSrcAddr(), 0, tmpBytes, 0, 4);
        //dest ip
        System.arraycopy(this.getDstAddr(), 0, tmpBytes, 4, 4);
        //8bit 0
        System.arraycopy(new byte[]{0}, 0, tmpBytes, 8, 1);
        //8bit ipProto
        System.arraycopy(new byte[]{this.getProto()}, 0, tmpBytes, 9, 1);
        //16bit tcpLength
        System.arraycopy(new byte[]{(byte) (tcpLength >>> 8), (byte) (tcpLength & 0xff)}, 0, tmpBytes, 10, 2);

        byte[] tcpCheckSumArray = getTcpCheckSumArray();
        System.arraycopy(tcpCheckSumArray, 0, tmpBytes, 12, tcpCheckSumArray.length);
        System.arraycopy(additionArray, 0, tmpBytes, 12 + tcpCheckSumArray.length, additionArray.length);

        return PacketUtil.checkSum(tmpBytes);
    }

    public byte[] getTcpCheckSumArray() {
        List<Byte> list = new ArrayList<>();
        list.addAll(PacketUtil.short2Bytes(srcPort));
        list.addAll(PacketUtil.short2Bytes(dstPort));
        list.addAll(PacketUtil.int2Bytes(seq));
        list.addAll(PacketUtil.int2Bytes(ack));
        list.add(dataOffset_rsv);
        list.add(flag);
        list.addAll(PacketUtil.short2Bytes(window));
        list.addAll(PacketUtil.short2Bytes(tcpCheckSum));
        list.addAll(PacketUtil.short2Bytes(urgentPointer));

        byte[] rs = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            rs[i] = list.get(i);
        }
        return rs;
    }


}
