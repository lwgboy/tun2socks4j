package entity;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import lombok.*;
import util.PacketUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Version|  IHL  |Type of Service|          Total Length         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Identification        |Flags|      Fragment Offset    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Time to Live |    Protocol   |         Header Checksum       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       Source Address                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Destination Address                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Options                    |    Padding    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
@Data
@NoArgsConstructor
@FieldOrder({"verion_ihl", "tos", "totalLen", "identification", "flags_FragmentOffset", "ttl", "proto", "ipCheckSum", "srcAddr", "dstAddr"})
public class IPHeader extends Structure implements Serializable {
    public byte verion_ihl = 0x45;
    public byte tos;
    public short totalLen;
    public short identification;
    public byte[] flags_FragmentOffset = new byte[]{0x40, 0x00};
    public byte ttl = (byte) 255;
    public byte proto = 6;
    public short ipCheckSum;
    public byte[] srcAddr = new byte[1];
    public byte[] dstAddr = new byte[1];
    // public byte[] ipOptions = new byte[32];


    public IPHeader(Pointer p, IPHeader src) {
        super(p);
        this.verion_ihl = src.verion_ihl;
        this.tos = src.tos;
        this.totalLen = src.totalLen;
        this.identification = src.identification;
        this.flags_FragmentOffset = src.flags_FragmentOffset;
        this.ttl = src.ttl;
        this.proto = src.proto;
        this.ipCheckSum = src.ipCheckSum;
        this.srcAddr = src.srcAddr;
        this.dstAddr = src.dstAddr;
    }

    public IPHeader(Pointer p) {
        super(p);
    }

    //0xffff会把数字转成高16位都是0的int,因为要达到的目的是转成unsigned short
    //(short)会只保留低16位，最高位依然是符号位
    public short ipChecksum() {
        byte[] p = super.getPointer().getByteArray(0, 20);
        return PacketUtil.checkSum(p);
    }

    @SneakyThrows
    public byte[] getIpCheckSumArray() {
        List<Byte> list = new ArrayList<>();
        list.add(verion_ihl);
        list.add(tos);
        list.addAll(PacketUtil.short2Bytes(totalLen));
        list.addAll(PacketUtil.short2Bytes(identification));
        list.add(flags_FragmentOffset[0]);
        list.add(flags_FragmentOffset[1]);
        list.add(ttl);
        list.add(proto);

        byte[] rs = new byte[list.size() + 8];
        for (int i = 0; i < list.size(); i++) {
            rs[i] = list.get(i);
        }
        System.arraycopy(srcAddr, 0, rs, rs.length - 4, srcAddr.length);
        System.arraycopy(dstAddr, 0, rs, rs.length - 8, dstAddr.length);
        return rs;
    }
}
