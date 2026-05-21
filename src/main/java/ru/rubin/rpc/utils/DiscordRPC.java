package ru.rubin.rpc.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface DiscordRPC extends Library {
    DiscordRPC INSTANCE = Native.load("discord-rpc", DiscordRPC.class);

    void Discord_Initialize(String applicationId, DiscordEventHandlers handlers, boolean autoRegister, String optionalSteamId);
    void Discord_Shutdown();
    void Discord_RunCallbacks();
    void Discord_UpdatePresence(DiscordRichPresence presence);
    void Discord_ClearPresence();
    void Discord_Respond(String userId, int reply);
    void Discord_UpdateConnection();
    void Discord_UpdateHandlers(DiscordEventHandlers handlers);
    void Discord_Register(String applicationId, String command);
    void Discord_RegisterSteamGame(String applicationId, String steamId);
}
