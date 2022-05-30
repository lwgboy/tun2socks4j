package dll;

import com.sun.jna.*;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import lombok.Data;

public class DllLib {
    public interface Msvcrt extends Library {
        Msvcrt lib = Native.load("msvcrt", Msvcrt.class);

        void memcpy(Object... arg);

        void memset(Object... arg);
    }

    public interface Wintun extends Library {
        Wintun lib = Native.load("wintun", Wintun.class);

        Pointer WintunCreateAdapter(Object... arg);

        Pointer WintunStartSession(Object... arg);

        Pointer WintunAllocateSendPacket(Object... arg);

        void WintunSendPacket(Object... arg);

        void WintunGetAdapterLuid(Object... arg);

        Pointer WintunReceivePacket(Object... arg);

        void WintunReleaseReceivePacket(Object... arg);

        void WintunEndSession(Object... arg);

        void WintunCloseAdapter(Object... arg);
    }

    public interface Iphlpapi extends Library {
        Iphlpapi lib = Native.load("Iphlpapi", Iphlpapi.class);

        void InitializeUnicastIpAddressEntry(Object... arg);

        Integer CreateUnicastIpAddressEntry(Object... arg);

        Integer CreateIpForwardEntry(Object... arg);

        Integer ConvertInterfaceLuidToIndex(Object... arg);

        @Data
        @FieldOrder({"dwForwardDest",
                "dwForwardMask",
                "dwForwardPolicy",
                "dwForwardNextHop",
                "dwForwardIfIndex",
                "u1",
                "u2",
                "dwForwardAge",
                "dwForwardNextHopAS",
                "dwForwardMetric1",
                "dwForwardMetric2",
                "dwForwardMetric3",
                "dwForwardMetric4",
                "dwForwardMetric5"})
        class _MIB_IPFORWARDROW extends Structure {
            public DWORD dwForwardDest;
            public DWORD dwForwardMask;
            public DWORD dwForwardPolicy;
            public DWORD dwForwardNextHop;
            public int dwForwardIfIndex;
            public U1 u1;
            public U2 u2;
            public DWORD dwForwardAge;
            public DWORD dwForwardNextHopAS;
            public DWORD dwForwardMetric1;
            public DWORD dwForwardMetric2;
            public DWORD dwForwardMetric3;
            public DWORD dwForwardMetric4;
            public DWORD dwForwardMetric5;

            public _MIB_IPFORWARDROW() {
            }

        }
        @Data
        @FieldOrder({"dwForwardType","ForwardType"})
        class U1 extends Union {
            public U1() {
            }

            public DWORD dwForwardType;
            public int ForwardType;
            /**
             * Value	Meaning
             * MIB_IPROUTE_TYPE_OTHER
             * 1
             * Some other type not specified in RFC 1354.
             * MIB_IPROUTE_TYPE_INVALID
             * 2
             * An invalid route. This value can result from a route added by an ICMP redirect.
             * MIB_IPROUTE_TYPE_DIRECT
             * 3
             * A local route where the next hop is the final destination (a local interface).
             * MIB_IPROUTE_TYPE_INDIRECT
             * 4
             * The remote route where the next hop is not the final destination (a remote destination).
             */
        }
        @Data
        @FieldOrder({"dwForwardProto","ForwardProto"})
        class U2 extends Union {
            public DWORD dwForwardProto;
            public int ForwardProto;

            public U2() {
            }
            /**
             * typedef enum
             *  {
             *    MIB_IPPROTO_OTHER = 1,
             *    MIB_IPPROTO_LOCAL = 2,
             *    MIB_IPPROTO_NETMGMT = 3,
             *    MIB_IPPROTO_ICMP = 4,
             *    MIB_IPPROTO_EGP = 5,
             *    MIB_IPPROTO_GGP = 6,
             *    MIB_IPPROTO_HELLO = 7,
             *    MIB_IPPROTO_RIP = 8,
             *    MIB_IPPROTO_IS_IS = 9,
             *    MIB_IPPROTO_ES_IS = 10,
             *    MIB_IPPROTO_CISCO = 11,
             *    MIB_IPPROTO_BBN = 12,
             *    MIB_IPPROTO_OSPF = 13,
             *    MIB_IPPROTO_BGP = 14,
             *    MIB_IPPROTO_NT_AUTOSTATIC = 10002,
             *    MIB_IPPROTO_NT_STATIC = 10006,
             *    MIB_IPPROTO_NT_STATIC_NON_DOD = 10007,
             *  } MIB_IPFORWARD_PROTO;
             */
        }

    }

    public interface NtDll extends Library {
        NtDll lib = Native.load("NtDll", NtDll.class);

        void RtlIpv4AddressToStringW(Object... args);

    }

    public interface WinSock extends Library {
        WinSock lib = Native.load("Ws2_32", WinSock.class);

        int htonl(Object... arg);

        short htons(Object... arg);
    }
}
