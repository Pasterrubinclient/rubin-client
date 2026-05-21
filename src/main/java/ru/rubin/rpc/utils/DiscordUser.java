package ru.rubin.rpc.utils;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@Structure.FieldOrder({"userId", "username", "discriminator", "avatar"})
public class DiscordUser extends Structure {
    public String userId;
    public String username;
    public String discriminator;
    public String avatar;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("userId", "username", "discriminator", "avatar");
    }
}
