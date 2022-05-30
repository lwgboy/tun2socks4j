import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import lombok.Data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MIB_UNICASTIPADDRESS_ROW extends Structure {
    public SOCKADDR_INET Address;//28
    public NET_LUID InterfaceLuid; // todo
    public int InterfaceIndex;  //todo 大小
    public int PrefixOrigin;
    public int SuffixOrigin;
    public int ValidLifetime;
    public int PreferredLifetime;
    public byte OnLinkPrefixLength;
    public Boolean SkipAsSource;
    public Integer DadState;
    public int ScopeId;
    public long CreationTimeStamp;

    @Override
    protected List<String> getFieldOrder() {
        List<String> list = new ArrayList<String>();

        list.add("Address");
        //list.add("paddingtest");

        list.add("InterfaceLuid");
        list.add("InterfaceIndex");

        list.add("PrefixOrigin");
        list.add("SuffixOrigin");


        list.add("ValidLifetime");
        list.add("PreferredLifetime");
        list.add("OnLinkPrefixLength");
        list.add("SkipAsSource");
        list.add("DadState");
        list.add("ScopeId");
        list.add("CreationTimeStamp");
        return list;
    }


    public static class SOCKADDR_INET extends Union {
        public NativeMappings.sockaddr_in Ipv4;
        public NativeMappings.sockaddr_in6 Ipv6;
        public NativeLong si_family;

        @Override
        protected List<String> getFieldOrder() {
            List<String> list = new ArrayList<String>();
            list.add("Ipv4");
            list.add("Ipv6");
            list.add("si_family");
            return list;
        }
    }

    @FieldOrder({"Value", "Info"})
    public static class NET_LUID extends Union {
        public Long Value;
        public NET_LUID_UNION Info;
    }

    @FieldOrder({ "Reserved","NetLuidIndex", "IfType"})
    public static class NET_LUID_UNION extends Structure {
        public byte[] Reserved = new byte[3]; //24
        public byte[] NetLuidIndex = new byte[3]; //24
        public short IfType; //16
    }


}
