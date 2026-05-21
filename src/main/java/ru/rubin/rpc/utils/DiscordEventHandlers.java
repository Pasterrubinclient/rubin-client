package ru.rubin.rpc.utils;

import com.sun.jna.Callback;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@Structure.FieldOrder({
        "ready",
        "disconnected",
        "errored",
        "joinGame",
        "spectateGame",
        "joinRequest"
})
public class DiscordEventHandlers extends Structure {
    public OnReady ready;
    public OnStatus disconnected;
    public OnStatus errored;
    public OnGameUpdate joinGame;
    public OnGameUpdate spectateGame;
    public OnJoinRequest joinRequest;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
    }

    public interface OnReady extends Callback {
        void accept(DiscordUser user);
    }

    public interface OnStatus extends Callback {
        void accept(int code, String message);
    }

    public interface OnGameUpdate extends Callback {
        void accept(String secret);
    }

    public interface OnJoinRequest extends Callback {
        void accept(DiscordUser request);
    }

    public static class Builder {
        private final DiscordEventHandlers handlers = new DiscordEventHandlers();

        public Builder ready(OnReady ready) {
            handlers.ready = ready;
            return this;
        }

        public Builder disconnected(OnStatus disconnected) {
            handlers.disconnected = disconnected;
            return this;
        }

        public Builder errored(OnStatus errored) {
            handlers.errored = errored;
            return this;
        }

        public Builder joinGame(OnGameUpdate joinGame) {
            handlers.joinGame = joinGame;
            return this;
        }

        public Builder spectateGame(OnGameUpdate spectateGame) {
            handlers.spectateGame = spectateGame;
            return this;
        }

        public Builder joinRequest(OnJoinRequest joinRequest) {
            handlers.joinRequest = joinRequest;
            return this;
        }

        public DiscordEventHandlers build() {
            return handlers;
        }
    }
}
