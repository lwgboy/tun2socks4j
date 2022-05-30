import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import dll.DllLib;
import dll.DllLib.Wintun;
import entity.TCPHeader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import util.PacketUtil;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

import static dll.DllLib.Wintun.lib;


//接收和发送报文
@Slf4j
public class WintunAdapter {


    public Pointer session;
    public Pointer adapter;
    public final int ADDR_WINTUN = DllLib.WinSock.lib.htonl((192 << 24) | (168 << 16) | (0 << 8) | (111 << 0));

    public TunPacket reciveGamePacket() {
        TunPacket tunPacket = new TunPacket();
        IntByReference sizePointer = new IntByReference();

        Pointer packet;
        while (true) {
            if ((packet = lib.WintunReceivePacket(session, sizePointer)) == null
                    || !isToMxdServer(packet)) {
                continue;
            }
            break;
        }

        tunPacket.setPacket(packet);
        tunPacket.setSize(sizePointer.getValue());
        tunPacket.setIpVersion(PacketUtil.getIpVersion(packet));
        tunPacket.setIpProto(PacketUtil.getIpProto(packet));

        tunPacket.setSrcPort(PacketUtil.getSrcPort(packet));
        tunPacket.setSrcAddr(PacketUtil.getSrcAddr(packet));
        tunPacket.setSrcLen(PacketUtil.getTcpPayloadLen(packet));
        tunPacket.setSrcSeq(PacketUtil.getTcpSeq(packet));
        tunPacket.setSrcAck(PacketUtil.getTcpAck(packet));

        tunPacket.setDstPort(PacketUtil.getDstPort(packet));
        tunPacket.setDstAddr(PacketUtil.getDstAddr(packet));
        return tunPacket;
    }


    @SneakyThrows
    public void forwardMxdPacketToS5(TunPacket tunPacket) {
        Socket socketToSocks5 = Main.socks5Map.get(tunPacket.getSrcPort());
        socketToSocks5.getOutputStream().write(PacketUtil.getTcpPayload(tunPacket.getPacket()));
    }


    private void printLastestError() {
        int error = Kernel32.INSTANCE.GetLastError();
        if (error == 259) {
            return;
        }
        log.error("dll exec error {}", error);
    }

    public boolean initSession() {
        //创建adaptor
        UUID uuid = UUID.randomUUID();
        adapter = Wintun.lib.WintunCreateAdapter("1", "2", uuid.toString());//todo 乱码可能是因为为了玩冒险岛调成了繁体
        if (adapter == null) {
            throw new RuntimeException("adapter初始化异常,code:" + Kernel32.INSTANCE.GetLastError());
        }

        //创建单播ip的数据结构
        MIB_UNICASTIPADDRESS_ROW AddressRow = Structure.newInstance(MIB_UNICASTIPADDRESS_ROW.class);
        AddressRow.Address.setType(NativeMappings.sockaddr_in.class);
        AddressRow.InterfaceLuid.setType(Long.class);
        DllLib.Iphlpapi.lib.InitializeUnicastIpAddressEntry(AddressRow.getPointer());
        AddressRow.read();


        //WintunLib.INSTANCE.WintunGetAdapterLuid(adapter,AddressRow.InterfaceLuid);
        int flag = 0x200 | 0x800;
        WinDef.HMODULE h = Kernel32.INSTANCE.LoadLibraryEx("wintun.dll", null, flag);
        Function function = Function.getFunction(Kernel32.INSTANCE.GetProcAddress(h, 6));
        log.debug("function:" + function);
        AddressRow.InterfaceLuid.setType(MIB_UNICASTIPADDRESS_ROW.NET_LUID_UNION.class);
        function.invoke(new Object[]{adapter, AddressRow.InterfaceLuid.getPointer()});
        AddressRow.read();
        log.debug("1.5 error=" + Kernel32.INSTANCE.GetLastError());

        AddressRow.Address.Ipv4.sin_family = 2;//1862314176 01101111   00000000   10101000    11000000
        AddressRow.Address.Ipv4.sin_addr.S_un.setType(Integer.class);
        AddressRow.Address.Ipv4.sin_addr.S_un.S_addr = ADDR_WINTUN;
        AddressRow.OnLinkPrefixLength = 24;
        AddressRow.DadState = 4;//IpDadStatePreferred
        AddressRow.write();
        Integer rs = DllLib.Iphlpapi.lib.CreateUnicastIpAddressEntry(AddressRow.getPointer());
        AddressRow.read();
        //session开始
        session = Wintun.lib.WintunStartSession(adapter, 0x400000);
        CreateAppForwardEntry(AddressRow.InterfaceLuid.getPointer());
        log.debug("1.6 error=" + Kernel32.INSTANCE.GetLastError());
        return true;
    }


    public void fin() {
    }


    @SneakyThrows
    private Boolean isToMxdServer(Pointer packet) {
        if (PacketUtil.getIpTotlen(packet) < 20 || PacketUtil.getIpVersion(packet) != 4) {
            return false;
        }

        //from https://github.com/FQrabbit/SSTap-Rule/blob/master/rules/TMS.rules
        String[] split = getGameServerIpArray();
        return Arrays.stream(split)
                .anyMatch(x -> TunSocket.isInRange(PacketUtil.getDstAddr(packet).getHostAddress(), x));
    }

    private String[] getGameServerIpArray() throws IOException {
        return FileUtils.readFileToString(new File("src/main/resources/gameServerIp"), Charset.defaultCharset()).replaceAll("\\r|\\n", "")
                .split(",");
    }


    @SneakyThrows
    private void CreateAppForwardEntry(Pointer ifLuid) {
        // DllLib.Iphlpapi._MIB_IPFORWARDROW route = new DllLib.Iphlpapi._MIB_IPFORWARDROW();

        IntByReference ifIndex = new IntByReference();
        DllLib.Iphlpapi.lib.ConvertInterfaceLuidToIndex(ifLuid, ifIndex.getPointer());
        for (String ip : getGameServerIpArray()) {
            String command = "cmd.exe /c start " + "route add " + ip.split("/")[0] + " MASK " + getMask(ip) + " 192.168.0.111 METRIC 3 IF " + ifIndex.getValue();
            log.debug(command);
            Runtime.getRuntime().exec(command);
        }
        //Runtime.getRuntime().exec("cmd.exe /c start " + "route add 34.120.37.0 MASK 255.255.255.0 192.168.0.111 METRIC 3 IF " + ifIndex.getValue());
       /* Runtime.getRuntime().exec("cmd.exe /c start " + "route add 54.248.197.129 MASK 255.255.255.255 192.168.0.111 METRIC 3 IF " + ifIndex.getValue());
        Runtime.getRuntime().exec("cmd.exe /c start " + "route add 13.114.20.204 MASK 255.255.255.255 192.168.0.111 METRIC 3 IF " + ifIndex.getValue());

        Runtime.getRuntime().exec("cmd.exe /c start " + "route add 202.80.104.29 MASK 255.255.255.255 192.168.0.111 METRIC 3 IF " + ifIndex.getValue());
        Runtime.getRuntime().exec("cmd.exe /c start " + "route add 34.120.37.3 MASK 255.255.255.255 192.168.0.111 METRIC 3 IF " + ifIndex.getValue());*/


        /* //route.setDwForwardDest(new DWORD(DllLib.WinSock.lib.htonl((202 << 24) | (80 << 16) | (104 << 8) | (24 << 0))));
        route.setDwForwardIfIndex(ifIndex.getValue());
        route.setDwForwardMask(new DWORD(DllLib.WinSock.lib.htonl((255 << 24) | (255 << 16) | (255 << 8) | (255 << 0))));
     //   route.setDwForwardNextHop(new DWORD(ADDR_WINTUN));
        route.setDwForwardMetric1(new DWORD(2));
        route.u2.setType(DWORD.class);
        route.u1.setType(DWORD.class);
        route.u1.setDwForwardType(new DWORD(3));
        route.u2.setDwForwardProto(new DWORD(3));
        route.write();
        Integer rs = DllLib.Iphlpapi.lib.CreateIpForwardEntry(route.getPointer());
        System.out.println(rs);*/
    }

    private String getMask(String ip) {
        int zeroCount = (32 - Integer.parseInt(ip.split("/")[1])) / 8;
        int tffCount = 4 - zeroCount;
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, tffCount).forEach(x -> sb.append("255."));
        IntStream.range(0, zeroCount).forEach(x -> sb.append("0."));
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    public void releasePacket(TunPacket tunPacket) {
        lib.WintunReleaseReceivePacket(session, tunPacket.getPacket());
    }

    public void sendPacket(TCPHeader packet) {
        //DllLib.Msvcrt.lib.memset(outgoingPacket, 0, totLen);
        packet.write();
        lib.WintunSendPacket(session, packet.getPointer());
    }
}
