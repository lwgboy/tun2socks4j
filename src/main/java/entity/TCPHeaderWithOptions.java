package entity;

import com.sun.jna.Pointer;
import com.sun.jna.Structure.FieldOrder;
import lombok.Builder;
import lombok.NoArgsConstructor;

@FieldOrder({"tcpOptions"})
@NoArgsConstructor
public class TCPHeaderWithOptions extends TCPHeader {
    public byte[] tcpOptions = new byte[1];


    public TCPHeaderWithOptions(Pointer p, TCPHeader src) {
        super(p, src);
        this.tcpOptions = src.getTcpOptions();
    }


    public void checkSum(int tcpLength) {
        this.setTcpCheckSum(super.tcpChecksum(tcpOptions, tcpLength));
    }

    @Override
    public byte[] getTcpOptions() {
        return tcpOptions;
    }

    @Override
    public void setTcpOptions(byte[] tcpOptions) {
        this.tcpOptions = tcpOptions;
    }
}
