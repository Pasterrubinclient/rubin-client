package ru.rubin.rpc;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.rpc.callbacks.DisconnectedCallback;
import ru.rubin.rpc.callbacks.ErroredCallback;
import ru.rubin.rpc.callbacks.JoinGameCallback;
import ru.rubin.rpc.callbacks.JoinRequestCallback;
import ru.rubin.rpc.callbacks.ReadyCallback;
import ru.rubin.rpc.callbacks.SpectateGameCallback;

@Environment(EnvType.CLIENT)
public class DiscordEventHandlers extends Structure {
   public DisconnectedCallback disconnected;
   public JoinRequestCallback joinRequest;
   public SpectateGameCallback spectateGame;
   public ReadyCallback ready;
   public ErroredCallback errored;
   public JoinGameCallback joinGame;

   protected List<String> getFieldOrder() {
      return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }
}
