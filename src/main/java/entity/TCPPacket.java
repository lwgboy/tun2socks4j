package entity;

import com.sun.jna.Pointer;
import com.sun.jna.Structure.FieldOrder;
import lombok.NoArgsConstructor;

@FieldOrder({"data"})
@NoArgsConstructor
public class TCPPacket extends TCPHeader {
    public byte[] data = new byte[1];


    public TCPPacket(Pointer p, TCPHeader src) {
        super(p, src);
        this.data = src.getData();
    }

    public void checkSum(int tcpLength) {
        this.setTcpCheckSum(super.tcpChecksum(data, tcpLength));
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public void setData(byte[] data) {
        this.data = data;
    }
}
