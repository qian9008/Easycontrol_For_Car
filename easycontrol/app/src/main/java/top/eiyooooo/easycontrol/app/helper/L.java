package top.eiyooooo.easycontrol.app.helper;

import android.text.format.DateFormat;
import android.util.Log;
import top.eiyooooo.easycontrol.app.entity.Device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class L {
    private static final int MAX_LOG_LENGTH = 200 * 1024;
    private static final Map<String, StringBuilder> logs = new HashMap<>();

    public static synchronized void log(String uuid, String log) {
        StringBuilder logBuilder = logs.get(uuid);
        if (logBuilder == null) {
            logBuilder = new StringBuilder();
            logs.put(uuid, logBuilder);
        }
        logBuilder.append("[").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("] ");
        logBuilder.append(log).append("\n");
        trimLog(logBuilder);
    }

    public static synchronized void log(String uuid, Throwable throwable) {
        String log = Log.getStackTraceString(throwable);
        log(uuid, log);
    }

    public static synchronized void logWithoutTime(String uuid, String log) {
        StringBuilder logBuilder = logs.get(uuid);
        if (logBuilder == null) {
            logBuilder = new StringBuilder();
            logs.put(uuid, logBuilder);
        }
        logBuilder.append(log).append("\n");
        trimLog(logBuilder);
    }

    public static synchronized void logWithoutTime(String uuid, Throwable throwable) {
        String log = Log.getStackTraceString(throwable);
        logWithoutTime(uuid, log);
    }

    public static synchronized String getLogs() {
        StringBuilder logBuilder = new StringBuilder();
        ArrayList<String> uuids = new ArrayList<>();
        for (Device device : DeviceListAdapter.devicesList) {
            uuids.add(device.uuid);
        }
        for (Map.Entry<String, StringBuilder> entry : logs.entrySet()) {
            if (!uuids.contains(entry.getKey())) {
                logBuilder.append(entry.getValue());
            }
        }
        if (logBuilder.length() > 0) {
            return logBuilder.toString();
        }
        return "no log found";
    }

    public static synchronized void clearLogs() {
        ArrayList<String> uuids = new ArrayList<>();
        for (Device device : DeviceListAdapter.devicesList) {
            uuids.add(device.uuid);
        }
        Iterator<Map.Entry<String, StringBuilder>> iterator = logs.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!uuids.contains(iterator.next().getKey())) iterator.remove();
        }
    }

    public static synchronized String getLogs(String uuid) {
        StringBuilder logBuilder = logs.get(uuid);
        if (logBuilder != null) {
            return logBuilder.toString();
        }
        return "no log found";
    }

    public static synchronized void clearLogs(String uuid) {
        logs.remove(uuid);
    }

    private static void trimLog(StringBuilder logBuilder) {
        if (logBuilder.length() <= MAX_LOG_LENGTH) return;
        int deleteEnd = logBuilder.length() - MAX_LOG_LENGTH;
        int nextLine = logBuilder.indexOf("\n", deleteEnd);
        if (nextLine >= 0) deleteEnd = nextLine + 1;
        logBuilder.delete(0, deleteEnd);
        logBuilder.insert(0, "[日志过长，已仅保留最近内容]\n");
    }
}
