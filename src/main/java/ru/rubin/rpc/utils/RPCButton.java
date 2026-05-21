package ru.rubin.rpc.utils;

public class RPCButton {
    private final String url;
    private final String label;

    public static RPCButton create(String label, String url) {
        String safeLabel = label == null ? "" : label.substring(0, Math.min(label.length(), 31));
        return new RPCButton(safeLabel, url);
    }

    public RPCButton(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }
}
