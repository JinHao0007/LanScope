package com.lan.scanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 邻居表（ARP 表）读取。
 *
 * Android 10 起应用读 /proc/net/arp 通常拿不到内容（回环限制），
 * 所以优先用 `ip neigh` 命令读取内核邻居表，/proc/net/arp 作为兜底。
 */
public class ArpTableReader {

    /**
     * 查询单个 IP 的 MAC。每次实时读取，保证拿到最新条目。
     *
     * @return 小写 MAC，未找到返回 null
     */
    public static String find(String ip) {
        String mac = findByIpNeigh(ip);
        if (mac == null) mac = findByProcArp(ip);
        return mac;
    }

    /** 优先方案：ip neigh show（不受 /proc 限制） */
    private static String findByIpNeigh(String ip) {
        Process process = null;
        try {
            process = new ProcessBuilder("ip", "neigh", "show").start();
            java.util.Scanner sc = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String out = sc.hasNext() ? sc.next() : "";
            for (String line : out.split("\n")) {
                // 格式: 10.195.129.181 dev swlan0 lladdr c6:2f:de:e0:5d:a5 REACHABLE
                if (!line.contains(ip)) continue;
                int idx = line.indexOf("lladdr");
                if (idx < 0) continue;
                String rest = line.substring(idx + 6).trim();
                String m = rest.split("\\s+")[0];
                if (isValidMac(m)) return m.toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        } finally {
            if (process != null) process.destroy();
        }
        return null;
    }

    /** 兜底方案：/proc/net/arp（旧版本 Android 有效） */
    private static String findByProcArp(String ip) {
        try (DataInputStream in = new DataInputStream(new FileInputStream("/proc/net/arp"));
             java.util.Scanner sc = new java.util.Scanner(in)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.startsWith("IP address")) continue;
                String[] cols = line.split("\\s+");
                if (cols.length < 4 || !cols[0].equals(ip)) continue;
                String mac = cols[3];
                if ("00:00:00:00:00:00".equals(mac) || !cols[2].contains("2")) return null;
                if (isValidMac(mac)) return mac.toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 读取全表（{ip, mac} 列表），ip neigh 优先 */
    public static List<String[]> read() {
        List<String[]> result = new ArrayList<>();
        Process process = null;
        try {
            process = new ProcessBuilder("ip", "neigh", "show").start();
            java.util.Scanner sc = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String out = sc.hasNext() ? sc.next() : "";
            for (String line : out.split("\n")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 3) continue;
                String ip = parts[0];
                String mac = null;
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("lladdr".equals(parts[i])) {
                        mac = parts[i + 1];
                        break;
                    }
                }
                if (ip != null && isValidMac(mac)) result.add(new String[]{ip, mac.toLowerCase(Locale.ROOT)});
            }
        } catch (Exception ignored) {
        } finally {
            if (process != null) process.destroy();
        }
        if (result.isEmpty()) {
            try (java.util.Scanner sc = new java.util.Scanner(new FileInputStream("/proc/net/arp"))) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    if (line.startsWith("IP address")) continue;
                    String[] cols = line.split("\\s+");
                    if (cols.length < 4) continue;
                    if ("00:00:00:00:00:00".equals(cols[3]) || !cols[2].contains("2")) continue;
                    if (isValidMac(cols[3])) result.add(new String[]{cols[0], cols[3].toLowerCase(Locale.ROOT)});
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static boolean isValidMac(String mac) {
        if (mac == null) return false;
        return mac.matches("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
    }
}
