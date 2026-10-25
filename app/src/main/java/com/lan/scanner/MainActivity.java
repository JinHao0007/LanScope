package com.lan.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NetworkScanner.ScanListener {

    private static final int REQUEST_PERMISSIONS = 100;

    /** 热点名读取重试延迟（毫秒）：热点刚开启时配置尚未同步，需要多次重试 */
    private static final long[] SSID_RETRY_DELAYS = {400L, 1200L, 2500L};

    private int ssidRetryStage = 0;
    private final java.util.concurrent.atomic.AtomicBoolean ssidRetryScheduled =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    /** 缓存上次读到的热点名，避免重试期间界面闪回 "--" */
    private String lastKnownApSsid = null;

    private TextView tvSsid, tvLocalIp, tvGateway, tvNetmask, tvDeviceCount, statusBadge;
    private Button btnScan;
    private View emptyState;
    private com.google.android.material.progressindicator.LinearProgressIndicator scanProgress;
    private RecyclerView deviceList;
    private DeviceAdapter adapter;

    private WifiManager wifiManager;
    private Thread scanThread;
    private volatile NetworkScanner scanner;
    private int foundCount = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSsid = findViewById(R.id.tvSsid);
        tvLocalIp = findViewById(R.id.tvLocalIp);
        tvGateway = findViewById(R.id.tvGateway);
        tvNetmask = findViewById(R.id.tvNetmask);
        tvDeviceCount = findViewById(R.id.tvDeviceCount);
        statusBadge = findViewById(R.id.statusBadge);
        btnScan = findViewById(R.id.btnScan);
        emptyState = findViewById(R.id.emptyState);
        scanProgress = findViewById(R.id.scanProgress);
        deviceList = findViewById(R.id.deviceList);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        adapter = new DeviceAdapter();
        deviceList.setLayoutManager(new LinearLayoutManager(this));
        deviceList.setAdapter(adapter);

        btnScan.setOnClickListener(v -> toggleScan());

        if (!hasPermissions()) {
            requestPermissions();
        }
        refreshNetworkInfo();
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasPermissions()) {
                refreshNetworkInfo();
            } else {
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNetworkInfo();
    }

    /** 读取当前网络（热点或 Wi-Fi）信息并更新界面 */
    @SuppressLint("MissingPermission")
    private void refreshNetworkInfo() {
        // 每次刷新都重置重试进度，允许重新尝试读取热点名
        ssidRetryStage = 0;
        ssidRetryScheduled.set(false);

        // 本机 IP：优先遍历网络接口，找到热点(ap/swlan0)或 wifi(wlan0) 接口
        String localIp = null;
        String hotspotInterface = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<NetworkInterface> list = new ArrayList<>();
            while (interfaces.hasMoreElements()) list.add(interfaces.nextElement());

            // 热点接口优先（ap0 / swlan0 / wlan1 常见于热点模式）
            for (NetworkInterface nif : list) {
                String name = nif.getName();
                if (name != null && (name.startsWith("ap") || name.startsWith("swlan"))) {
                    hotspotInterface = nif.getName();
                    localIp = getInterfaceIp(nif);
                    break;
                }
            }
            // 其次找 wlan 接口（连接 Wi-Fi 时）
            if (localIp == null) {
                for (NetworkInterface nif : list) {
                    String name = nif.getName();
                    if (name != null && name.startsWith("wlan")) {
                        hotspotInterface = nif.getName();
                        localIp = getInterfaceIp(nif);
                        break;
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        // 网关和掩码：DhcpInfo 对热点模式同样适用（热点本机即网关）
        DhcpInfo dhcp = wifiManager != null ? wifiManager.getDhcpInfo() : null;

        // 热点接口信息（ap*/swlan*）无论反射检测结果如何都保存，供兜底判定使用
        boolean hasApIface = (hotspotInterface != null);
        final String fIp = localIp != null ? localIp : "--";
        String gateway = "--";
        String netmask = "--";
        String ssid = "--";
        boolean hotspotOn = false;
        if (dhcp != null && dhcp.netmask != 0) {
            // 热点模式下 gateway 通常为本机 IP；连接 Wi-Fi 时为路由器 IP
            gateway = Formatter.formatIpAddress(dhcp.gateway);
            netmask = Formatter.formatIpAddress(dhcp.netmask);
        } else if (localIp != null) {
            // 热点模式 DhcpInfo 常返回 0，用接口前缀长度推掩码
            int p = 24;
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                for (NetworkInterface nif : Collections.list(interfaces)) {
                    if (nif.getName() != null && (nif.getName().startsWith("ap") || nif.getName().startsWith("swlan"))) {
                        if (!nif.getInterfaceAddresses().isEmpty()) {
                            short pl = nif.getInterfaceAddresses().get(0).getNetworkPrefixLength();
                            if (pl > 0 && pl <= 32) p = pl;
                        }
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
            long m = p == 0 ? 0 : (0xFFFFFFFFL << (32 - p)) & 0xFFFFFFFFL;
            netmask = NetworkScanner.longToIp(m);
            gateway = localIp; // 热点模式下本机即网关
        }

        // 热点是否开启的判定：WifiManager.isWifiApEnabled() 反射不可靠，
        // 部分机型上恒返回 false（即使热点已开启）——用户实测"重启应用才识别到"
        // 正是这个原因。这里多路判定：反射 + 热点网络接口 + 特征 IP 段。
        try {
            java.lang.reflect.Method method = WifiManager.class.getMethod("isWifiApEnabled");
            method.setAccessible(true);
            hotspotOn = (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            hotspotOn = false;
        }
        // 兜底1：ap*/swlan* 热点接口存在且本机有 IP → 热点开启（该接口只有热点模式才会创建）
        if (!hotspotOn && hasApIface && localIp != null) {
            hotspotOn = true;
        }
        // 兜底2：本机 IP 是热点网段（Android 默认热点 192.168.43.x，运营商 NAT 10.x）
        if (!hotspotOn && localIp != null && localIp.startsWith("192.168.43.")) {
            hotspotOn = true;
        }

        if (hotspotOn) {
            // 热点开启时本机就是网关
            if (localIp != null) gateway = localIp;
            statusBadge.setText(R.string.enabled);
            statusBadge.setTextColor(ContextCompat.getColor(this, R.color.secondary));
            ssid = readApSsid();
            // 读不到热点名时不写死"--"：先留空，稍后延迟重试（见 scheduleSsidRetry）
            if (ssid == null) {
                ssid = lastKnownApSsid;
                scheduleSsidRetry();
            } else {
                lastKnownApSsid = ssid;
            }
        } else {
            statusBadge.setText(R.string.not_enabled);
            statusBadge.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            // 连接 Wi-Fi 时尝试读 SSID（需要位置权限）
            if (hasPermissions() && wifiManager != null) {
                try {
                    String info = wifiManager.getConnectionInfo().getSSID();
                    if (info != null && !"<unknown ssid>".equalsIgnoreCase(info)) {
                        ssid = info.replace("\"", "");
                    }
                } catch (Exception ignored) {
                }
            }
        }

        final String fGateway = gateway, fNetmask = netmask, fSsid = ssid;
        mainHandler.post(() -> {
            tvLocalIp.setText(fIp);
            tvGateway.setText(fGateway);
            tvNetmask.setText(fNetmask);
            tvSsid.setText(fSsid == null || fSsid.isEmpty() ? "--" : fSsid);
        });
    }

    /**
     * 读取本机热点名称（SSID）。
     *
     * 用户实测：刚开启热点时常常读不到，重启应用才正常 —— 这是因为热点配置在
     * 刚开启的瞬间还没完全同步到 WifiManager。所以这里做多路兜底 + 延迟重试。
     */
    private String readApSsid() {
        // 1) API 30+ 的 getSoftApConfiguration（@SystemApi，公开 SDK 里没有，必须反射）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                java.lang.reflect.Method m = WifiManager.class.getMethod("getSoftApConfiguration");
                m.setAccessible(true);
                Object config = m.invoke(wifiManager);
                if (config != null) {
                    java.lang.reflect.Method getSsid = config.getClass().getMethod("getSsid");
                    getSsid.setAccessible(true);
                    Object ssid = getSsid.invoke(config);
                    if (ssid instanceof String && !((String) ssid).isEmpty()) {
                        return (String) ssid;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // 2) 旧接口 getWifiApConfiguration 反射
        try {
            java.lang.reflect.Method m = WifiManager.class.getMethod("getWifiApConfiguration");
            m.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) m.invoke(wifiManager);
            if (config != null && config.SSID != null && !config.SSID.isEmpty()) {
                return config.SSID;
            }
        } catch (Exception ignored) {
        }
        // 3) 部分 ROM 在热点模式下把热点 SSID 透出到 getConnectionInfo()
        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null) {
                String s = info.getSSID();
                if (s != null && !s.isEmpty() && !"<unknown ssid>".equalsIgnoreCase(s)) {
                    return s.replace("\"", "");
                }
            }
        } catch (Exception ignored) {
        }
        // 4) 都失败返回 null，由调用方安排延迟重试
        return null;
    }

    /**
     * 热点名延迟重试：热点刚开启时 SoftApConfiguration 常常还没就绪，
     * 用户实测「重启应用就正常」正是这个原因。这里在 0.4s / 1.2s / 2.5s
     * 各重试一次，全程不需要重启应用。
     */
    private void scheduleSsidRetry() {
        if (ssidRetryStage >= SSID_RETRY_DELAYS.length) return;
        if (!ssidRetryScheduled.compareAndSet(false, true)) return;
        long delay = SSID_RETRY_DELAYS[ssidRetryStage++];
        mainHandler.postDelayed(() -> {
            ssidRetryScheduled.set(false);
            String retry = readApSsid();
            if (retry != null && !retry.isEmpty()) {
                lastKnownApSsid = retry;
                tvSsid.setText(retry);
            } else if (ssidRetryStage < SSID_RETRY_DELAYS.length) {
                scheduleSsidRetry(); // 还读不到，继续下一次
            }
        }, delay);
    }

    private String getInterfaceIp(NetworkInterface nif) {
        try {
            Enumeration<InetAddress> addrs = nif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                    return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 开始 / 停止扫描 */
    private void toggleScan() {
        if (scanThread != null && scanThread.isAlive()) {
            stopScan();
            return;
        }
        startScan();
    }

    private void startScan() {
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }

        // 根据本机 IP 与掩码计算网段；拿不到就用热点默认网段 192.168.43.0/24
        String localIp = null;
        int prefix = 24;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            outer:
            for (NetworkInterface nif : Collections.list(interfaces)) {
                String name = nif.getName();
                if (name == null || !(name.startsWith("ap") || name.startsWith("swlan") || name.startsWith("wlan"))) continue;
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        localIp = addr.getHostAddress();
                        short p = nif.getInterfaceAddresses().isEmpty() ? 24
                                : nif.getInterfaceAddresses().get(0).getNetworkPrefixLength();
                        if (p > 0 && p <= 32) prefix = p;
                        break outer;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        final String baseIp;
        final int prefixLen = prefix;
        if (localIp == null) {
            baseIp = "192.168.43.0"; // Android 热点默认网段
        } else {
            long addr = NetworkScanner.ipToLong(localIp);
            long mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            baseIp = NetworkScanner.longToIp(addr & mask);
        }

        foundCount = 0;
        adapter.clear();
        emptyState.setVisibility(View.VISIBLE);
        scanProgress.setVisibility(View.VISIBLE);
        btnScan.setText(R.string.stop_scan);
        btnScan.setBackgroundResource(R.drawable.button_stop);
        mainHandler.post(() -> Toast.makeText(this, R.string.scanning, Toast.LENGTH_SHORT).show());

        scanner = new NetworkScanner();
        final String fLocalIp = localIp;
        scanThread = new Thread(() -> scanner.scan(baseIp, prefixLen, 200, fLocalIp, this));
        scanThread.start();
    }

    private void stopScan() {
        if (scanner != null) scanner.stop();
        scanProgress.setVisibility(View.GONE);
        btnScan.setText(R.string.scan_network);
        btnScan.setBackgroundResource(R.drawable.button_primary);
    }

    // ===== ScanListener 回调（工作线程）=====

    @Override
    public void onDeviceFound(NetworkScanner.DeviceInfo device) {
        mainHandler.post(() -> {
            foundCount++;
            adapter.add(device);
            emptyState.setVisibility(View.GONE);
            tvDeviceCount.setText(String.valueOf(foundCount));
        });
    }

    @Override
    public void onProgress(int scanned, int total) {
        mainHandler.post(() -> {
            scanProgress.setMax(total);
            scanProgress.setIndeterminate(false);
            scanProgress.setProgressCompat(scanned, true);
        });
    }

    @Override
    public void onScanFinished(int found) {
        mainHandler.post(() -> {
            scanProgress.setVisibility(View.GONE);
            btnScan.setText(R.string.scan_network);
            btnScan.setBackgroundResource(R.drawable.button_primary);
            String msg = getString(R.string.scan_complete, foundCount);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.stop();
    }
}
