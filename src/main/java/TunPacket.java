import com.sun.jna.Pointer;
import lombok.Data;
import util.PacketUtil;

import java.net.InetAddress;


@Data
public class TunPacket {
    private Pointer packet;

    private Integer size;
    private Byte ipVersion;
    private Byte ipProto;

    private Integer srcPort;
    private InetAddress srcAddr;
    private int srcLen;
    private int srcSeq;
    private int srcAck;

    private InetAddress dstAddr;
    private Integer dstPort;


    public Integer getSocketKey() {
        return srcPort;
    }

    public boolean isOnlyAck() {
        return PacketUtil.getTcpFlag(packet) == Constant.TCP_FLAG_ACK;
    }

    public long getUnsignedSrcLen() {
        return Integer.toUnsignedLong(srcLen);
    }

    public long getUnsignedSrcSeq() {
        return Integer.toUnsignedLong(srcSeq);
    }

    public long getUnsignedSrcAck() {
        return Integer.toUnsignedLong(srcAck);
    }
}