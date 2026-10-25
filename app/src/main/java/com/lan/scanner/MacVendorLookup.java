package com.lan.scanner;

import java.util.HashMap;
import java.util.Map;

/** 根据 MAC 前缀查询厂商名称（常见厂商内置，离线可用） */
public class MacVendorLookup {

    private static final Map<String, String> OUI = new HashMap<>();

    static {
        put("00:1A:11", "Google"); put("00:1A:A0", "Google"); put("00:08:22", "Google");
        put("F4:F5:E8", "Google"); put("D8:04:F7", "Google"); put("F8:8F:CA", "Google");
        put("3C:5A:B4", "Google"); put("6C:AD:F8", "Google"); put("98:B6:E9", "Google");
        put("F4:09:B8:F8", "Google-Fi");

        // Apple
        put("00:11:32", "Apple"); put("00:1B:63", "Apple"); put("00:17:F2", "Apple");
        put("00:1E:52", "Apple"); put("00:1E:C2", "Apple"); put("00:21:E9", "Apple");
        put("00:23:12", "Apple"); put("00:23:32", "Apple"); put("00:23:DF", "Apple");
        put("00:24:36", "Apple"); put("00:25:00", "Apple"); put("00:25:4B", "Apple");
        put("00:25:BC", "Apple"); put("00:26:08", "Apple"); put("00:26:BB", "Apple");
        put("00:26:B0", "Apple"); put("00:1F:F3", "Apple"); put("28:6A:BA", "Apple");
        put("30:10:B4", "Apple"); put("34:C0:59", "Apple"); put("44:00:49", "Apple");
        put("58:55:CA", "Apple"); put("60:D9:C7", "Apple"); put("6C:40:08", "Apple");
        put("70:DE:E2", "Apple"); put("80:E6:50", "Apple"); put("8C:29:37", "Apple");
        put("98:01:A7", "Apple"); put("9C:20:7B", "Apple"); put("A4:5E:60", "Apple");
        put("A8:88:08", "Apple"); put("AC:87:A3", "Apple"); put("B0:34:95", "Apple");
        put("C0:63:94", "Apple"); put("C8:69:CD", "Apple"); put("CC:08:E0", "Apple");
        put("D0:E1:40", "Apple"); put("D4:9A:20", "Apple"); put("E4:25:E7", "Apple");
        put("E4:CE:8F", "Apple"); put("F0:B4:79", "Apple"); put("C8:3C:85", "Apple");
        put("B4:18:D1", "Apple"); put("74:E1:B6", "Apple"); put("AC:BC:32", "Apple");

        // Samsung
        put("00:1E:E3", "Samsung"); put("00:16:32", "Samsung"); put("00:21:19", "Samsung");
        put("00:23:39", "Samsung"); put("00:23:99", "Samsung"); put("00:24:54", "Samsung");
        put("00:24:90", "Samsung"); put("00:25:66", "Samsung"); put("00:25:67", "Samsung");
        put("00:07:AB", "Samsung"); put("34:C3:AC", "Samsung"); put("38:01:97", "Samsung");
        put("40:0E:85", "Samsung"); put("4C:BC:A6", "Samsung"); put("58:C2:32", "Samsung");
        put("84:25:DB", "Samsung"); put("8C:77:12", "Samsung"); put("B0:72:BF", "Samsung");
        put("B0:EC:71", "Samsung"); put("D8:57:EF", "Samsung"); put("E8:03:9A", "Samsung");
        put("F8:04:2E", "Samsung"); put("FC:A1:3E", "Samsung"); put("08:00:07", "Samsung");

        // Huawei
        put("5C:F9:38", "Huawei"); put("18:C5:8A", "Huawei"); put("28:3C:E4", "Huawei");
        put("34:6B:D3", "Huawei"); put("3C:47:11", "Huawei"); put("4C:1F:CC", "Huawei");
        put("4C:B1:6C", "Huawei"); put("58:1F:28", "Huawei"); put("58:2A:F7", "Huawei");
        put("78:1D:BA", "Huawei"); put("10:1B:54", "Huawei"); put("04:33:89", "Huawei");
        put("04:B0:E7", "Huawei"); put("04:C0:6F", "Huawei"); put("04:F9:77", "Huawei");
        put("08:19:A6", "Huawei"); put("08:63:61", "Huawei"); put("08:7A:4C", "Huawei");
        put("0C:37:DC", "Huawei"); put("0C:45:BA", "Huawei"); put("10:4B:54", "Huawei");
        put("18:8A:A8", "Huawei"); put("20:0B:C7", "Huawei"); put("20:2B:C1", "Huawei");
        put("24:09:95", "Huawei"); put("24:44:27", "Huawei"); put("24:69:A5", "Huawei");
        put("28:5F:DB", "Huawei"); put("2C:AB:00", "Huawei"); put("30:87:30", "Huawei");
        put("30:87:D9", "Huawei"); put("34:00:A3", "Huawei"); put("40:4D:8E", "Huawei");
        put("40:CB:A8", "Huawei"); put("44:55:B1", "Huawei"); put("48:00:31", "Huawei");
        put("48:46:FB", "Huawei"); put("48:62:76", "Huawei"); put("4C:54:99", "Huawei");
        put("54:25:EA", "Huawei"); put("54:A5:1B", "Huawei"); put("58:60:5F", "Huawei");
        put("5C:C0:97", "Huawei"); put("64:16:F0", "Huawei"); put("64:3E:8C", "Huawei");
        put("64:5D:86", "Huawei"); put("64:9A:BE", "Huawei"); put("70:72:3C", "Huawei");
        put("74:9D:6E", "Huawei"); put("78:6A:89", "Huawei"); put("7C:11:CB", "Huawei");
        put("7C:60:97", "Huawei"); put("80:96:1F", "Huawei"); put("84:46:FE", "Huawei");
        put("88:28:B3", "Huawei"); put("88:86:03", "Huawei"); put("8C:34:FD", "Huawei");
        put("8C:BE:BE", "Huawei"); put("90:67:1C", "Huawei"); put("94:04:9C", "Huawei");
        put("94:77:2B", "Huawei"); put("98:9C:57", "Huawei"); put("9C:28:EF", "Huawei");
        put("A4:71:74", "Huawei"); put("A8:C8:3A", "Huawei"); put("AC:4E:91", "Huawei");
        put("AC:CF:85", "Huawei"); put("B0:5B:67", "Huawei"); put("B4:15:13", "Huawei");
        put("B8:08:D7", "Huawei"); put("BC:25:E0", "Huawei"); put("BC:62:0F", "Huawei");
        put("C0:BF:C0", "Huawei"); put("C4:07:71", "Huawei"); put("C8:14:79", "Huawei");
        put("C8:51:95", "Huawei"); put("CC:63:AD", "Huawei"); put("CC:A2:23", "Huawei");
        put("D4:61:2E", "Huawei"); put("D4:6A:A8", "Huawei"); put("D4:B1:10", "Huawei");
        put("D4:6A:6E", "Huawei"); put("E0:19:1D", "Huawei"); put("E4:35:C8", "Huawei");
        put("E8:08:8B", "Huawei"); put("E8:68:19", "Huawei"); put("EC:23:3D", "Huawei");
        put("F0:43:47", "Huawei"); put("F4:4C:7F", "Huawei"); put("F4:55:9C", "Huawei");
        put("F4:C7:14", "Huawei"); put("F8:01:13", "Huawei"); put("F8:4A:BF", "Huawei");
        put("F8:98:B9", "Huawei"); put("FC:48:EF", "Huawei");

        // Xiaomi
        put("28:6C:07", "Xiaomi"); put("50:8F:4C", "Xiaomi"); put("64:09:80", "Xiaomi");
        put("64:CC:2E", "Xiaomi"); put("98:FA:E3", "Xiaomi"); put("9C:99:A0", "Xiaomi");
        put("C4:0B:CB", "Xiaomi"); put("F8:9E:19", "Xiaomi"); put("00:9E:C8", "Xiaomi");
        put("04:CF:8C", "Xiaomi"); put("04:D4:C4", "Xiaomi"); put("04:D6:AA", "Xiaomi");
        put("0C:1D:AF", "Xiaomi"); put("10:2A:B3", "Xiaomi"); put("14:F6:5A", "Xiaomi");
        put("18:59:36", "Xiaomi"); put("1C:56:FE", "Xiaomi"); put("20:82:C0", "Xiaomi");
        put("24:49:12", "Xiaomi"); put("2C:AB:44", "Xiaomi"); put("34:80:79", "Xiaomi");
        put("34:CE:00", "Xiaomi"); put("38:A4:ED", "Xiaomi"); put("3C:BD:D8", "Xiaomi");
        put("40:31:3C", "Xiaomi"); put("44:23:7C", "Xiaomi"); put("48:59:29", "Xiaomi");
        put("48:BF:6B", "Xiaomi"); put("4C:49:E3", "Xiaomi"); put("50:64:2B", "Xiaomi");
        put("58:44:98", "Xiaomi"); put("5C:B3:2F", "Xiaomi"); put("60:AB:67", "Xiaomi");
        put("64:16:66", "Xiaomi"); put("68:37:E9", "Xiaomi"); put("6C:2F:2C", "Xiaomi");
        put("74:51:BA", "Xiaomi"); put("78:02:F8", "Xiaomi"); put("78:11:DC", "Xiaomi");
        put("7C:1C:4E", "Xiaomi"); put("88:c3:97", "Xiaomi"); put("88:53:D4", "Xiaomi");
        put("98:7F:5F", "Xiaomi"); put("A4:45:19", "Xiaomi"); put("AC:C7:8E", "Xiaomi");
        put("B0:E2:35", "Xiaomi"); put("CC:8E:37", "Xiaomi"); put("D4:97:0B", "Xiaomi");
        put("F0:B4:29", "Xiaomi"); put("F8:2A:1A", "Xiaomi"); put("F8:C3:97", "Xiaomi");
        put("E4:46:DA", "Xiaomi"); put("E4:7E:66", "Xiaomi"); put("F4:F5:DB", "Xiaomi");
        put("F8:1F:5A", "Xiaomi"); put("F8:E4:0F", "Xiaomi"); put("18:D6:C7", "Xiaomi");
        put("6C:C7:EC", "Xiaomi"); put("7C:49:EB", "Xiaomi"); put("D4:6E:5C", "Xiaomi");
        put("D4:2C:67", "Xiaomi");

        // OPPO / vivo / OnePlus
        put("08:00:6E", "OPPO"); put("8C:BE:BE", "OPPO"); put("A4:50:46", "OPPO");
        put("08:19:A6", "OPPO"); put("24:09:95", "OPPO"); put("8C:CD:F8", "OPPO");
        put("94:01:C2", "OPPO"); put("B4:CD:27", "OPPO"); put("C0:EE:FB", "OPPO");
        put("D8:D4:2C", "OPPO"); put("F4:09:B8", "OPPO"); put("F8:E4:0F", "OPPO");
        put("A4:77:33", "OPPO"); put("74:EF:88", "OPPO"); put("88:C9:D0", "OPPO");
        put("5C:B3:2F", "vivo"); put("D4:61:2E", "vivo"); put("14:B6:57", "vivo");
        put("8C:8E:F2", "vivo"); put("E4:67:5D", "vivo"); put("3C:BD:D8", "vivo");
        put("38:A4:ED", "vivo"); put("40:4E:36", "vivo"); put("54:3D:37", "vivo");
        put("88:53:92", "vivo"); put("94:77:2B", "vivo"); put("B0:E2:35", "vivo");
        put("50:02:91", "vivo"); put("58:44:98", "vivo"); put("70:72:3C", "vivo");
        put("8C:77:12", "vivo"); put("AC:9E:17", "vivo"); put("B4:1D:90", "vivo");
        put("10:6F:3F", "OnePlus"); put("64:BC:0C", "OnePlus"); put("84:38:38", "OnePlus");
        put("38:C3:53", "OnePlus"); put("74:23:44", "OnePlus"); put("94:01:C2", "OnePlus");
        put("9C:99:A0", "OnePlus"); put("AC:9E:17", "OnePlus"); put("F8:1F:5A", "OnePlus");
        put("F4:F5:DB", "OnePlus"); put("C8:69:CD", "OnePlus"); put("18:82:9E", "OnePlus");

        // Lenovo / HP / Dell
        put("00:50:E2", "Lenovo"); put("00:16:EA", "Lenovo"); put("00:59:07", "Lenovo");
        put("54:EE:75", "Lenovo"); put("60:D9:C7", "Lenovo"); put("88:AE:1D", "Lenovo");
        put("A0:36:9F", "Lenovo"); put("F4:06:69", "Lenovo"); put("38:F3:AB", "Lenovo");
        put("14:18:77", "Lenovo"); put("20:99:80", "Lenovo"); put("68:3E:34", "Lenovo");
        put("00:1F:29", "Dell"); put("00:21:70", "Dell"); put("00:23:AE", "Dell");
        put("00:24:E8", "Dell"); put("00:25:64", "Dell"); put("F8:DB:88", "Dell");
        put("B0:83:70", "Dell"); put("D4:BE:D9", "Dell"); put("E8:39:35", "Dell");
        put("F8:34:41", "Dell"); put("02:97:1A", "Dell"); put("00:14:22", "Dell");
        put("00:1E:0B", "HP"); put("00:1F:29", "HP"); put("00:21:5A", "HP");
        put("00:23:7D", "HP"); put("00:24:81", "HP"); put("00:25:B3", "HP");
        put("10:1F:74", "HP"); put("10:60:4B", "HP"); put("14:02:EC", "HP");
        put("28:80:23", "HP"); put("2C:41:38", "HP"); put("2C:44:FD", "HP");
        put("2C:59:E5", "HP"); put("30:8D:99", "HP"); put("34:64:A9", "HP");
        put("38:63:BB", "HP"); put("3C:4A:92", "HP"); put("3C:D9:2B", "HP");
        put("40:B0:34", "HP"); put("48:15:0B", "HP"); put("58:20:B1", "HP");
        put("84:34:97", "HP"); put("94:57:A5", "HP"); put("A0:1D:48", "HP");
        put("B0:5A:DA", "HP"); put("C4:34:6B", "HP"); put("C8:CB:B8", "HP");
        put("D4:C9:EF", "HP"); put("E4:11:5B", "HP"); put("EC:8E:B5", "HP");
        put("F0:92:1C", "HP"); put("1C:C1:DE", "HP");

        // 路由器/网络设备
        put("00:20:7B", "D-Link"); put("00:05:5D", "D-Link"); put("00:0D:88", "D-Link");
        put("00:14:78", "D-Link"); put("00:15:E9", "D-Link"); put("00:17:9A", "D-Link");
        put("00:1C:F0", "D-Link"); put("00:21:91", "D-Link"); put("00:22:B0", "D-Link");
        put("00:24:B2", "D-Link"); put("00:26:5A", "D-Link"); put("14:D6:4D", "D-Link");
        put("1C:7E:E5", "D-Link"); put("34:08:04", "D-Link"); put("C0:A0:BB", "D-Link");
        put("00:0C:29", "VMware"); put("00:50:56", "VMware"); put("00:05:69", "VMware");
        put("52:54:00", "QEMU/KVM"); put("0A:58:0A", "Docker");
        put("DC:9F:7A", "Ruijie"); put("0C:DA:7C", "Ruijie"); put("24:69:11", "Ruijie");
        put("58:69:6C", "Ruijie"); put("88:3F:D3", "Ruijie"); put("9C:06:1C", "Ruijie");
        put("F8:40:42", "Ruijie");
        put("00:26:18", "ASUS"); put("04:D9:F5", "ASUS"); put("08:60:6E", "ASUS");
        put("10:BF:48", "ASUS"); put("14:DA:E9", "ASUS"); put("20:CF:30", "ASUS");
        put("24:4B:FE", "ASUS"); put("2C:4D:54", "ASUS"); put("2C:56:DC", "ASUS");
        put("30:5A:3A", "ASUS"); put("30:B4:9E", "ASUS"); put("38:2C:4A", "ASUS");
        put("40:B0:76", "ASUS"); put("50:46:5D", "ASUS"); put("54:04:A6", "ASUS");
        put("54:A0:50", "ASUS"); put("60:45:CB", "ASUS"); put("6C:B0:CE", "ASUS");
        put("74:D0:2B", "ASUS"); put("AC:9E:17", "ASUS"); put("B0:6E:BF", "ASUS");
        put("BC:AE:C5", "ASUS"); put("BC:EE:7B", "ASUS"); put("C8:60:85", "ASUS");
        put("D4:5D:64", "ASUS"); put("E0:3F:49", "ASUS"); put("F0:79:59", "ASUS");
        put("F4:6D:04", "ASUS"); put("F8:32:E4", "ASUS"); put("74:88:44", "ASUS");
        put("00:0A:EB", "TP-Link"); put("00:18:4D", "TP-Link"); put("00:1D:0F", "TP-Link");
        put("00:1E:E2", "TP-Link"); put("00:21:27", "TP-Link"); put("00:22:93", "TP-Link");
        put("00:23:CD", "TP-Link"); put("00:27:19", "TP-Link"); put("00:23:89", "TP-Link");
        put("14:CC:20", "TP-Link"); put("50:C7:BF", "TP-Link"); put("54:C8:0F", "TP-Link");
        put("64:66:B3", "TP-Link"); put("A4:2B:B0", "TP-Link"); put("AC:84:C6", "TP-Link");
        put("B0:4E:26", "TP-Link"); put("C0:25:E9", "TP-Link"); put("E8:DE:27", "TP-Link");
        put("F4:EC:38", "TP-Link"); put("F4:F2:6D", "TP-Link"); put("98:DA:C4", "TP-Link");
        put("A8:57:4E", "TP-Link"); put("D4:6E:0C", "TP-Link"); put("D8:03:9C", "TP-Link");
        put("50:2E:5C", "TP-Link"); put("D8:CE:3B", "TP-Link"); put("5C:E9:31", "TP-Link");
        put("C4:6A:B7", "TP-Link"); put("94:D9:B3", "TP-Link"); put("EC:23:3D", "TP-Link");
        put("24:69:68", "TP-Link");

        // 网卡/芯片
        put("00:E0:4C", "Realtek"); put("00:1B:44", "Realtek"); put("00:13:F7", "Realtek");
        put("52:54:00", "Realtek"); put("00:87:30", "Realtek");
        put("00:1B:21", "Intel"); put("00:1C:BF", "Intel"); put("00:1D:E0", "Intel");
        put("00:1E:64", "Intel"); put("00:1F:3B", "Intel"); put("00:1F:3F", "Intel");
        put("00:22:FA", "Intel"); put("00:22:FB", "Intel"); put("00:24:D6", "Intel");
        put("00:24:D7", "Intel"); put("00:27:10", "Intel"); put("A0:36:9F", "Intel");
        put("3C:97:0E", "Intel"); put("34:02:86", "Intel"); put("60:67:20", "Intel");
        put("00:00:0C", "Cisco"); put("00:1B:54", "Cisco"); put("00:1C:58", "Cisco");
        put("00:1D:70", "Cisco"); put("00:21:A0", "Cisco"); put("00:21:D1", "Cisco");
        put("00:22:BD", "Cisco"); put("00:22:BF", "Cisco"); put("00:23:04", "Cisco");
        put("00:23:5E", "Cisco"); put("00:24:97", "Cisco"); put("00:25:45", "Cisco");
        put("00:1A:A1", "Cisco"); put("00:40:96", "Cisco");
        put("00:1E:58", "D-Link");
        put("00:0B:82", "Tenda"); put("C8:3A:35", "Tenda"); put("50:2E:8C", "Tenda");
        put("00:26:5A", "Tenda"); put("14:E6:E4", "Tenda"); put("18:82:7C", "Tenda");
        put("D8:15:0D", "Tenda"); put("04:95:E6", "Tenda"); put("C0:06:C3", "Tenda");
        put("50:2E:5C", "Tenda"); put("A4:52:6C", "Tenda"); put("E4:6A:9C", "Tenda");
    }

    private static void put(String prefix, String vendor) {
        OUI.put(prefix.toUpperCase(), vendor);
    }

    /** 查询 MAC 对应厂商，未知返回 null */
    public static String lookup(String mac) {
        if (mac == null || mac.length() < 8) return null;
        return OUI.get(mac.substring(0, 8).toUpperCase());
    }
}
