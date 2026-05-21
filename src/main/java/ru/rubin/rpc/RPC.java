package ru.rubin.rpc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
public class RPC implements IMinecraft {
   public static DiscordRichPresence presence = new DiscordRichPresence();
   public static boolean started;
   private static Thread thread;

   public void startRpc() {
      if (DiscordRPC.Loader.isAvailable()) {
         DiscordRPC rpc = DiscordRPC.Loader.getInstance();
         if (!started) {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1396524858464145521", handlers, true, "");
            presence.startTimestamp = System.currentTimeMillis() / 1000L;
            presence.largeImageText = "Rubin DLC - 1.21.8";
            rpc.Discord_UpdatePresence(presence);
            thread = new Thread(() -> {
               while (!Thread.currentThread().isInterrupted()) {
                  rpc.Discord_RunCallbacks();
                  presence.details = "Build 1.0.0 [Dev]";
                  presence.state = "Update to soon";
                  presence.button_label_1 = "Telegram";
                  presence.button_url_1 = "https://t.me/Night_DLCC";
                  presence.button_label_2 = "Discord";
                  presence.button_url_2 = "https://discord.com/invite/nightdlc";
                  presence.largeImageKey = "https://i.ibb.co/7xjn4G93/Rubin-pfp.png";
                  rpc.Discord_UpdatePresence(presence);

                  try {
                     Thread.sleep(2000L);
                  } catch (InterruptedException var2x) {
                  }
               }
            }, "TH-RPC-Handler");
            thread.start();
         }
      }
   }
}
