package ru.rubin.rpc.utils;

import com.sun.jna.Structure;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordRichPresence extends Structure {
    public String state;
    public String details;
    public long startTimestamp;
    public long endTimestamp;
    public String largeImageKey;
    public String largeImageText;
    public String smallImageKey;
    public String smallImageText;
    public String partyId;
    public int partySize;
    public int partyMax;
    public String partyPrivacy;
    public String matchSecret;
    public String joinSecret;
    public String spectateSecret;
    public String button_label_1;
    public String button_url_1;
    public String button_label_2;
    public String button_url_2;
    public int instance;

    public DiscordRichPresence() {
        this.setStringEncoding("UTF-8");
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "state", "details", "startTimestamp", "endTimestamp",
                "largeImageKey", "largeImageText", "smallImageKey", "smallImageText",
                "partyId", "partySize", "partyMax", "partyPrivacy",
                "matchSecret", "joinSecret", "spectateSecret",
                "button_label_1", "button_url_1", "button_label_2", "button_url_2",
                "instance"
        );
    }

    public static class Builder {
        private final DiscordRichPresence richPresence = new DiscordRichPresence();

        public Builder setDetails(String details) {
            if (details != null && !details.isEmpty()) {
                richPresence.details = details.substring(0, Math.min(details.length(), 128));
            }
            return this;
        }

        public Builder setState(String state) {
            if (state != null && !state.isEmpty()) {
                richPresence.state = state.substring(0, Math.min(state.length(), 128));
            }
            return this;
        }

        public Builder setLargeImage(String imageKey, String imageText) {
            richPresence.largeImageKey = imageKey;
            richPresence.largeImageText = imageText;
            return this;
        }

        public Builder setLargeImage(String imageKey) {
            return setLargeImage(imageKey, "");
        }

        public Builder setSmallImage(String imageKey, String imageText) {
            richPresence.smallImageKey = imageKey;
            richPresence.smallImageText = imageText;
            return this;
        }

        public Builder setSmallImage(String imageKey) {
            return setSmallImage(imageKey, "");
        }

        public Builder setStartTimestamp(long timestamp) {
            richPresence.startTimestamp = timestamp;
            return this;
        }

        public Builder setStartTimestamp(OffsetDateTime dateTime) {
            richPresence.startTimestamp = dateTime.toEpochSecond();
            return this;
        }

        public Builder setEndTimestamp(long timestamp) {
            richPresence.endTimestamp = timestamp;
            return this;
        }

        public Builder setEndTimestamp(OffsetDateTime dateTime) {
            richPresence.endTimestamp = dateTime.toEpochSecond();
            return this;
        }

        public Builder setButtons(RPCButton first, RPCButton second) {
            setButtons(Arrays.asList(first, second));
            return this;
        }

        public Builder setButtons(RPCButton button) {
            return setButtons(Collections.singletonList(button));
        }

        public Builder setButtons(List<RPCButton> buttons) {
            if (buttons != null && !buttons.isEmpty()) {
                int size = Math.min(buttons.size(), 2);
                richPresence.button_label_1 = buttons.get(0).getLabel();
                richPresence.button_url_1 = buttons.get(0).getUrl();
                if (size == 2) {
                    richPresence.button_label_2 = buttons.get(1).getLabel();
                    richPresence.button_url_2 = buttons.get(1).getUrl();
                }
            }
            return this;
        }

        public Builder setInstance(boolean instance) {
            richPresence.instance = instance ? 1 : 0;
            return this;
        }

        public Builder setSecrets(String match, String join, String spectate) {
            richPresence.matchSecret = match;
            richPresence.joinSecret = join;
            richPresence.spectateSecret = spectate;
            return this;
        }

        public DiscordRichPresence build() {
            return richPresence;
        }
    }
}
