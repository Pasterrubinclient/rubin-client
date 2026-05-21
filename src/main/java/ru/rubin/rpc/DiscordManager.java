package ru.rubin.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rubin.rpc.utils.*;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("RubinDiscordRPC");
    private static final DiscordManager INSTANCE = new DiscordManager();

    private static final String DEFAULT_APPLICATION_ID = "1426977740967514333";
    private static final String GIF_URL = "https://s14.gifyu.com/images/bw1q4.gif";
    private static final String DLL_DIR = "src/main/resources/win32-x86-64";

    private DiscordDaemonThread discordDaemonThread;
    private DiscordEventHandlers handlers;
    private volatile boolean running;
    private volatile DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private volatile String currentApplicationId = "";
    private volatile long startedAt;
    private volatile boolean ready;
    private volatile boolean updateConnectionSupported = true;
    private final AtomicBoolean readyLogged = new AtomicBoolean(false);

    private DiscordManager() {}

    public static DiscordManager getInstance() {
        return INSTANCE;
    }

    public synchronized void init() {
        startRPC(DEFAULT_APPLICATION_ID);
    }

    public synchronized void startRPC(String applicationId) {
        String appId = applicationId == null || applicationId.isBlank() ? DEFAULT_APPLICATION_ID : applicationId;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) return;

        if (running) {
            if (appId.equals(currentApplicationId)) return;
            stopRPC();
        }

        running = true;
        ready = false;
        currentApplicationId = appId;
        startedAt = System.currentTimeMillis() / 1000L;
        updateConnectionSupported = true;
        readyLogged.set(false);
        configureNativePath();

        handlers = new DiscordEventHandlers.Builder()
                .ready(user -> {
                    ready = true;
                    setInfo(new DiscordInfo(
                            user.username,
                            "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png",
                            user.userId
                    ));
                    if (readyLogged.compareAndSet(false, true)) {
                        LOGGER.info("Discord RPC ready: {} ({})", user.username, user.userId);
                    }
                    pushPresence();
                })
                .disconnected((code, message) -> {
                    ready = false;
                    LOGGER.warn("Discord RPC disconnected. code={}, message={}", code, message);
                })
                .errored((code, message) -> LOGGER.error("Discord RPC error. code={}, message={}", code, message))
                .build();

        try {
            DiscordRPC.INSTANCE.Discord_Initialize(appId, handlers, true, "");
        } catch (Throwable throwable) {
            running = false;
            ready = false;
            currentApplicationId = "";
            LOGGER.error("Discord_Initialize failed for appId={}", appId, throwable);
            return;
        }

        try {
            DiscordRPC.INSTANCE.Discord_RunCallbacks();
        } catch (Throwable throwable) {
            LOGGER.warn("Initial Discord callbacks run failed", throwable);
        }

        discordDaemonThread = new DiscordDaemonThread();
        discordDaemonThread.start();
    }

    public synchronized void stopRPC() {
        DiscordDaemonThread thread = discordDaemonThread;
        running = false;
        ready = false;
        currentApplicationId = "";
        readyLogged.set(false);
        try { DiscordRPC.INSTANCE.Discord_ClearPresence(); } catch (Throwable ignored) {}
        try { DiscordRPC.INSTANCE.Discord_Shutdown(); } catch (Throwable t) { LOGGER.warn("Discord_Shutdown failed", t); }
        if (thread != null) {
            thread.requestStop();
            thread.interrupt();
            discordDaemonThread = null;
        }
    }

    public boolean isRunning() { return running; }
    public String getCurrentApplicationId() { return currentApplicationId; }
    public DiscordInfo getInfo() { return info; }
    public void setInfo(DiscordInfo info) { this.info = info; }

    private void pushPresence() {
        if (!running || !ready) return;

        DiscordRichPresence richPresence = new DiscordRichPresence.Builder()
                .setStartTimestamp(startedAt)
                .setDetails("")
                .setState("")
                .setLargeImage(GIF_URL, "rubinclient")
                .setSmallImage(info.avatarUrl(), "rubinclient")
                .setButtons(
                        RPCButton.create("Telegram", "https://discord.gg/kYEAmH2Uw6"),
                        RPCButton.create("Discord", "https://discord.gg/kYEAmH2Uw6")
                )
                .build();

        try {
            DiscordRPC.INSTANCE.Discord_UpdatePresence(richPresence);
        } catch (Throwable throwable) {
            LOGGER.error("Discord_UpdatePresence failed", throwable);
        }
    }

    private void configureNativePath() {
        Path absoluteDllDir = Path.of(DLL_DIR).toAbsolutePath().normalize();
        String dir = absoluteDllDir.toString();
        String current = System.getProperty("jna.library.path");
        if (current == null || current.isBlank()) {
            System.setProperty("jna.library.path", dir);
        } else if (!current.contains(dir)) {
            System.setProperty("jna.library.path", dir + ";" + current);
        }
    }

    private class DiscordDaemonThread extends Thread {
        private volatile boolean stopping;

        private DiscordDaemonThread() { setDaemon(true); }

        private void requestStop() { this.stopping = true; }

        @Override
        public void run() {
            setName("Discord-RPC");
            try {
                while (DiscordManager.this.isRunning()) {
                    if (updateConnectionSupported) {
                        try {
                            DiscordRPC.INSTANCE.Discord_UpdateConnection();
                        } catch (UnsatisfiedLinkError ignored) {
                            updateConnectionSupported = false;
                            LOGGER.warn("Discord_UpdateConnection not supported, continuing without it");
                        }
                    }
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    pushPresence();
                    Thread.sleep(ready ? 1000L : 150L);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception exception) {
                if (!stopping && running) {
                    LOGGER.error("Discord thread crashed", exception);
                    stopRPC();
                }
            }
        }
    }

    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}
