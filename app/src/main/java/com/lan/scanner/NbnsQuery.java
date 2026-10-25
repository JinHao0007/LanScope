package com.lan.scanner;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * NetBIOS Name Service 节点状态查询（UDP 137）：
 * 直接询问对端设备"你是谁"，能拿到 NetBIOS 主机名和网卡 MAC。
 * 部分设备（尤其手机）不响应，属正常情况，返回 null。
 */
public final class NbnsQuery {

    private NbnsQuery() {
    }

    /**
     * 查询设备名称和 MAC。
     *
     * @return String[2] {name, mac}，无响应或解析失败返回 null
     */
    public static String[] query(String ip, int timeoutMs) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMs);
            socket.connect(InetAddress.getByName(ip), 137);
            byte[] packet = buildPacket();
            socket.send(new DatagramPacket(packet, packet.length));
            byte[] buf = new byte[1024];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            socket.receive(resp);
            return parse(buf, resp.getLength());
        } catch (Exception e) {
            return null; // 超时/拒绝：设备不响应 NBNS
        }
    }

    /** 构造 NBNS Node Status Request，查询通配名 '*' */
    private static byte[] buildPacket() {
        // '*' 编码：0x2A 高 nibble=2 -> 'C'，低 nibble=10 -> 'K'，其余补 'A'
        byte[] encodedName = new byte[32];
        encodedName[0] = 'C';
        encodedName[1] = 'K';
        for (int i = 2; i < 32; i++) encodedName[i] = 'A';

        byte[] p = new byte[12 + 1 + 32 + 1 + 4];
        p[0] = (byte) new Random().nextInt(256); // Transaction ID
        p[5] = 1;                                // QDCOUNT = 1
        int o = 12;
        p[o++] = 32;                             // label 长度
        System.arraycopy(encodedName, 0, p, o, 32);
        o += 32;
        p[o++] = 0;                              // 名字结束
        p[o++] = 0;
        p[o++] = 0x21;                           // QTYPE = NBSTAT (0x21)
        p[o++] = 0;
        p[o] = 1;                                // QCLASS = IN
        return p;
    }

    private static String[] parse(byte[] d, int len) {
        try {
            int o = 12; // 跳过 12 字节头
            // 跳过 question 名字（指针或标签链）
            o = skipName(d, o, len);
            o += 4; // QTYPE + QCLASS
            // 跳过 answer 名字
            o = skipName(d, o, len);
            o += 8; // TYPE + CLASS + TTL
            if (o + 2 > len) return null;
            int rdlen = ((d[o] & 0xFF) << 8) | (d[o + 1] & 0xFF);
            o += 2;
            int end = Math.min(o + rdlen, len);
            if (o >= end) return null;

            int count = d[o] & 0xFF;
            o += 1;
            String name = null;
            for (int i = 0; i < count && o + 18 <= end; i++) {
                byte[] nb = new byte[15];
                System.arraycopy(d, o, nb, 0, 15);
                o += 16; // 15 字节名 + 1 字节类型
                o += 2;  // 2 字节 flags
                if (name == null) name = sanitize(nb);
            }
            String mac = null;
            if (o + 6 <= end) {
                mac = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                        d[o] & 0xFF, d[o + 1] & 0xFF, d[o + 2] & 0xFF,
                        d[o + 3] & 0xFF, d[o + 4] & 0xFF, d[o + 5] & 0xFF);
            }
            if (name == null && mac == null) return null;
            return new String[]{name, mac};
        } catch (Exception e) {
            return null;
        }
    }

    private static int skipName(byte[] d, int o, int len) {
        if (o >= len) return o;
        if ((d[o] & 0xC0) != 0) return o + 2; // 压缩指针
        while (o < len && d[o] != 0) o += (d[o] & 0xFF) + 1;
        return o + 1;
    }

    /** 清洗 NetBIOS 名：去空白、只保留可打印 ASCII、过滤 <IS> 域名条目 */
    private static String sanitize(byte[] nb) {
        StringBuilder sb = new StringBuilder();
        for (byte b : nb) {
            char c = (char) (b & 0xFF);
            if (c == 0) break;
            if (c >= 0x20 && c < 0x7F) sb.append(c);
            else sb.append(' ');
        }
        String s = sb.toString().trim();
        if (s.isEmpty() || s.startsWith("<")) return null;
        return s;
    }
}
