package com.lan.scanner;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private final List<NetworkScanner.DeviceInfo> devices = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NetworkScanner.DeviceInfo device = devices.get(position);

        // 名称优先级：本机 > 主机名 > 厂商 > 未知设备
        String name;
        if (device.isLocal) {
            name = device.hostname != null && !device.hostname.isEmpty()
                    ? device.hostname + "（本机）" : "本机（热点发起方）";
        } else if (device.hostname != null && !device.hostname.isEmpty()) {
            name = device.hostname;
        } else {
            String vendor = MacVendorLookup.lookup(device.mac);
            name = vendor != null ? vendor : "未知设备";
        }
        String suffix = "";
        if (device.latency >= 0 && !device.isLocal) {
            suffix = String.format(" · %d ms", device.latency);
        }
        holder.tvName.setText(name + suffix);

        holder.tvIp.setText(device.ip);

        // 副标题：MAC + 厂商；拿不到 MAC 时明确说明原因（不是扫描失败）
        String vendor = device.isLocal ? "本机热点接口"
                : MacVendorLookup.lookup(device.mac);
        boolean macKnown = device.isLocal
                || (device.mac != null && !"unknown".equals(device.mac) && !"local".equals(device.mac));
        if (macKnown) {
            holder.tvMac.setText(vendor != null ? device.mac + " · " + vendor : device.mac);
            holder.tvMac.setTextColor(0xFF6B7A99);
        } else {
            holder.tvMac.setText("MAC 已随机化 / 设备不回应 · 仅 IP 可识别");
            holder.tvMac.setTextColor(0xFF8A94AD);
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void add(NetworkScanner.DeviceInfo device) {
        // 按 IP 排序插入
        int index = 0;
        long ipValue = NetworkScanner.ipToLong(device.ip);
        for (int i = 0; i < devices.size(); i++) {
            if (NetworkScanner.ipToLong(devices.get(i).ip) < ipValue) index = i + 1;
            else break;
        }
        devices.add(index, device);
        notifyItemInserted(index);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clear() {
        devices.clear();
        notifyDataSetChanged();
        // 重置计数显示
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvIp;
        final TextView tvMac;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvIp = itemView.findViewById(R.id.tvDeviceIp);
            tvMac = itemView.findViewById(R.id.tvDeviceMac);
        }
    }
}
