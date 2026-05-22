package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;

import java.util.*;

@IModule(name = "Potion Tracker", description = "Отслеживает зелья у ближайших игроков", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class PotionTracker extends Module {

    public static SliderSetting radius = new SliderSetting("Радиус", 16.0F, 4.0F, 64.0F, 1.0F, false);
    public static BooleanSetting showSelf = new BooleanSetting("Показывать свои", false);

    private final Map<UUID, Set<String>> trackedEffects = new HashMap<>();

    public PotionTracker() {
        this.addSettings(new Setting[]{radius, showSelf});
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trackedEffects.clear();
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) return;

        double rad = radius.get();
        double radSq = rad * rad;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !showSelf.get()) continue;
            if (mc.player.squaredDistanceTo(player) > radSq) continue;

            UUID uuid = player.getUuid();
            Set<String> known = trackedEffects.computeIfAbsent(uuid, k -> new HashSet<>());

            for (StatusEffectInstance effectInstance : player.getStatusEffects()) {
                StatusEffect effect = effectInstance.getEffectType().value();
                String key = effect.getTranslationKey() + "_" + effectInstance.getAmplifier();

                if (known.contains(key)) continue;
                known.add(key);

                // Build chat message
                MutableText playerName = player.getDisplayName().copy();
                Formatting color = getEffectColor(effect);

                MutableText effectName = Text.translatable(effect.getTranslationKey()).formatted(color);
                if (effectInstance.getAmplifier() > 0) {
                    effectName.append(Text.literal(" " + (effectInstance.getAmplifier() + 1)).formatted(color));
                }

                String durationStr = formatDuration(effectInstance.getDuration());

                MutableText message = Text.literal("")
                        .append(playerName)
                        .append(Text.literal(": получил: ").formatted(Formatting.GRAY))
                        .append(effectName)
                        .append(Text.literal(" - ").formatted(Formatting.GRAY))
                        .append(Text.literal(durationStr).formatted(color));

                mc.inGameHud.getChatHud().addMessage(message);
            }
        }

        // Cleanup players that are no longer nearby
        trackedEffects.entrySet().removeIf(entry -> {
            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                if (player.getUuid().equals(entry.getKey()) && mc.player.squaredDistanceTo(player) <= radSq) {
                    return false;
                }
            }
            return true;
        });
    }

    private Formatting getEffectColor(StatusEffect effect) {
        StatusEffectCategory category = effect.getCategory();
        return switch (category) {
            case BENEFICIAL -> Formatting.GREEN;
            case HARMFUL -> Formatting.RED;
            default -> Formatting.GRAY;
        };
    }

    private String formatDuration(int ticks) {
        if (ticks <= 0) return "0:00";
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
