package com.lan.scanner;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网段扫描引擎（全并行版）：
 * 每个候选 IP 一个任务，任务内：UDP 探测唤醒 ARP → 查 ARP 表 → TCP 确认。
 * 128 线程并发，/24 网段 10 秒内扫完。
 */
public class NetworkScanner {

    public interface ScanListener {
        void onDeviceFound(DeviceInfo device);
        void onProgress(int scanned, int total);
        void onScanFinished(int found);
    }

    /** 单台设备信息 */
    public static class DeviceInfo {
        public final String ip;
        public String mac;
        public String hostname = "";
        public int latency = -1;
        public boolean isLocal = false;

        public DeviceInfo(String ip, String mac) {
            this.ip = ip;
            this.mac = mac;
        }
    }

    private static final int[] UDP_PROBE_PORTS = {137, 139};
    private static final int[] TCP_PROBE_PORTS = {80, 443, 8080, 5555};

    private volatile boolean stopped = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(128);

    public void stop() {
        stopped = true;
        executor.shutdownNow();
    }

    public boolean isStopped() {
        return stopped;
    }

    /**
     * 扫描一个子网。
     *
     * @param baseIp 例如 "192.168.43.0"
     * @param prefixLength 例如 24
     */
    public void scan(String baseIp, int prefixLength, int timeoutMs, String localIp, ScanListener listener) {
        stopped = false;
        long address = ipToLong(baseIp);
        long mask = prefixLength <= 0 || prefixLength >= 32
                ? 0xFFFFFF00L : (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
        long network = address & mask;
        long broadcast = network | (~mask & 0xFFFFFFFFL);
        int total = (int) (broadcast - network - 1);
        if (total <= 0 || total > 1024) {
            listener.onScanFinished(0);
            return;
        }

        ExecutorCompletionService<Void> completion =
                new ExecutorCompletionService<>(executor);
        List<Future<Void>> futures = new ArrayList<>();
        AtomicInteger done = new AtomicInteger(0);
        AtomicInteger found = new AtomicInteger(0);

        for (long host = network + 1; host < broadcast; host++) {
            final String ip = longToIp(host);
            futures.add(completion.submit((Callable<Void>) () -> {
                if (stopped) return null;
                try {
                    // 本机：直接标记，不走探测（自己不会出现在 ARP 表里）
                    if (localIp != null && localIp.equals(ip)) {
                        DeviceInfo local = new DeviceInfo(ip, "local");
                        local.hostname = getLocalHostname();
                        local.isLocal = true;
                        local.latency = 0;
                        found.incrementAndGet();
                        listener.onDeviceFound(local);
                        int dl = done.incrementAndGet();
                        listener.onProgress(dl, total);
                        return null;
                    }
                    // 1. UDP 探测：触发内核对本 IP 做 ARP 解析
                    udpProbe(ip, timeoutMs);
                    // 2. 查 ARP：有 MAC 直接算在线
                    String mac = ArpTableReader.find(ip);
                    boolean online = mac != null;
                    long latency = -1;
                    // 3. ARP 没有则 TCP 确认；确认在线后再查一次 ARP（条目可能刚出现）
                    if (!online) {
                        long t0 = System.currentTimeMillis();
                        online = tcpProbe(ip, timeoutMs);
                        if (online) {
                            latency = System.currentTimeMillis() - t0;
                            if (mac == null) {
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException ie) {
                                    return null;
                                }
                                mac = ArpTableReader.find(ip);
                            }
                        }
                    }
                    if (online) {
                        DeviceInfo info = new DeviceInfo(ip, mac != null ? mac : "unknown");
                        info.latency = (int) latency;
                        // NetBIOS 查询（UDP 137）：直接问设备要名字和 MAC
                        String[] nb = NbnsQuery.query(ip, 300);
                        if (nb != null) {
                            if (nb[1] != null && isValidMac(nb[1])) {
                                // NBNS 回报的 MAC 更权威，覆盖 ARP 的（安卓本地地址随机化时用）
                                info.mac = nb[1].toLowerCase(Locale.ROOT);
                            }
                            if (nb[0] != null && !nb[0].isEmpty()) {
                                info.hostname = nb[0];
                            }
                        }
                        // 主机名兜底：DNS 反解
                        if (info.hostname.isEmpty()) {
                            info.hostname = resolveHostname(ip);
                        }
                        found.incrementAndGet();
                        listener.onDeviceFound(info);
                    }
                } catch (Throwable ignored) {
                }
                int d = done.incrementAndGet();
                listener.onProgress(d, total);
                return null;
            }));
        }

        // 等全部任务结束
        for (Future<Void> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
            }
        }
        listener.onScanFinished(found.get());
    }

    /** 本机主机名 */
    private String getLocalHostname() {
        try {
            String name = InetAddress.getLocalHost().getHostName();
            if (name != null && !name.isEmpty()) return name;
        } catch (Exception ignored) {
        }
        return "本机（热点发起方）";
    }

    /** 主机名解析：DNS 反解（安卓热点下对部分设备有效）；失败返回空串 */
    private String resolveHostname(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            String name = addr.getHostName();
            if (name != null && !name.equals(ip)) {
                return name;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** UDP 探测：发空包触发对端 ARP/ICMP 回应 */
    private void udpProbe(String ip, int timeoutMs) {
        for (int port : UDP_PROBE_PORTS) {
            if (stopped) return;
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(timeoutMs);
                socket.connect(InetAddress.getByName(ip), port);
                socket.send(new DatagramPacket(new byte[0], 0));
                socket.receive(new DatagramPacket(new byte[64], 64));
                return; // 有回应即可
            } catch (Exception ignored) {
            }
        }
    }

    /** TCP connect 探测：连接成功或被拒绝 (RST) 都代表主机在线 */
    private boolean tcpProbe(String ip, int timeoutMs) {
        for (int port : TCP_PROBE_PORTS) {
            if (stopped) return false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), timeoutMs);
                return true;
            } catch (Exception e) {
                if (e instanceof java.net.ConnectException) {
                    return true; // RST：主机在线但端口关闭
                }
                // 超时/不可达：继续下一端口
            }
        }
        return false;
    }

    private static boolean isValidMac(String mac) {
        return mac != null && mac.matches("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
    }

    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Long.parseLong(parts[i]) & 0xFF);
        }
        return result;
    }

    public static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
    }
}
