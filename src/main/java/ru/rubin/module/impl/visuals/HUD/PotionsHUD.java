package ru.rubin.module.impl.visuals.HUD;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import ru.rubin.module.impl.visuals.Hud;
import ru.rubin.util.render.core.Renderer2D;
import ru.rubin.util.render.math.ScaledResolution;
import ru.rubin.util.render.math.animation.AnimationMath;
import ru.rubin.util.render.math.animation.anim.util.Animation2;
import ru.rubin.util.render.math.animation.anim.util.Easings;
import ru.rubin.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class PotionsHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private static final Map<RegistryEntry<StatusEffect>, Integer> maxDurations = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedWidths = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedLineX = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedY = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Animation2> animatedAlphas = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> cachedEffects = new HashMap<>();

   public static void potions(Renderer2D r2, DrawContext drawContext) {
      new ScaledResolution(mc);
      if (mc.player != null) {
         Set<RegistryEntry<StatusEffect>> activeEffects = mc.player
               .getStatusEffects()
               .stream()
               .<RegistryEntry<StatusEffect>>map(StatusEffectInstance::getEffectType)
               .collect(Collectors.toSet());

         for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            cachedEffects.put(effect.getEffectType(), effect);
            RegistryEntry<StatusEffect> effectType = effect.getEffectType();
            if (!animatedAlphas.containsKey(effectType)) {
               Animation2 newAnim = new Animation2();
               newAnim.set(0.0);
               animatedAlphas.put(effectType, newAnim);
            }
         }

         animatedAlphas.forEach((effectTypex, anim) -> {
            double targetValue = activeEffects.contains(effectTypex) ? 1.0 : 0.0;
            anim.run(targetValue, 0.6, Easings.QUART_OUT, true);
            anim.update();
         });
         Set<RegistryEntry<StatusEffect>> effectsToRender = new HashSet<>(activeEffects);
         animatedAlphas.forEach((effectTypex, anim) -> {
            if (anim.get() > 0.01F) {
               effectsToRender.add(effectTypex);
            }
         });
         float x = 20.0F;
         float y = 474.0F;
         float offset = 0.0F;
         float offsetTexture = 0.0F;
         List<RegistryEntry<StatusEffect>> sortedEffects = new ArrayList<>(effectsToRender);
         sortedEffects.sort((a, b) -> {
            boolean aActive = activeEffects.contains(a);
            boolean bActive = activeEffects.contains(b);
            if (aActive != bActive) {
               return aActive ? -1 : 1;
            } else {
               return 0;
            }
         });
         int effectIndex = 0;

         for (RegistryEntry<StatusEffect> effectType : sortedEffects) {
            StatusEffectInstance effectx = cachedEffects.get(effectType);
            if (effectx != null) {
               int currentDuration = activeEffects.contains(effectType) ? effectx.getDuration() : 0;
               Animation2 alphaAnim = animatedAlphas.get(effectType);
               if (alphaAnim != null) {
                  float currentAlpha = alphaAnim.get();
                  float x3 = -80.0F + 80.0F * alphaAnim.get();
                  float targetY = y + offset;
                  float currentAnimatedY = animatedY.getOrDefault(effectType, targetY);
                  currentAnimatedY = AnimationMath.animation(currentAnimatedY, targetY, 0.1F);
                  animatedY.put(effectType, currentAnimatedY);
                  float y3 = currentAnimatedY - targetY;
                  if (currentAlpha <= 0.01F) {
                     offsetTexture += 22.5F * currentAlpha;
                     offset += 45.0F * currentAlpha;
                     effectIndex++;
                  } else {
                     r2.pushAlpha(currentAlpha);
                     if (!maxDurations.containsKey(effectType) || currentDuration > maxDurations.get(effectType)) {
                        maxDurations.put(effectType, currentDuration);
                     }

                     String effectText = effectx.getTranslationKey().replace("effect.minecraft.", "");
                     String text = effectText.substring(0, 1).toUpperCase()
                           + effectText.substring(1).replace("_", " ")
                           + " "
                           + String.valueOf(effectx.getAmplifier() + 1).replace("1", "");
                     float textWidth = r2.measureText(FontRegistry.INTER_MEDIUM, text, 28.0F).width;
                     String timeText = formatDuration(effectx.getDuration());
                     float timeWidth = r2.measureText(FontRegistry.INTER_MEDIUM, timeText, 22.0F).width;
                     float timeBoxWidth = timeWidth + 8.0F;
                     float timeBoxHeight = 16.03F;
                     float mainRectWidth = 100.0F + textWidth;
                     Hud.drawClientRect(r2, x + x3, currentAnimatedY, mainRectWidth, 40.64F, 13.0F, 1.0F, 1.0F);
                     int maxDuration = maxDurations.get(effectType);
                     float progress = maxDuration > 0 ? (float) currentDuration / maxDuration : 0.0F;
                     float targetWidth = mainRectWidth * progress;
                     float currentAnimatedWidth = animatedWidths.getOrDefault(effectType, targetWidth);
                     currentAnimatedWidth = AnimationMath.animation(currentAnimatedWidth, targetWidth, 0.1F);
                     animatedWidths.put(effectType, currentAnimatedWidth);
                     int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 0);
                     int color2 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 28);
                     if (currentAnimatedWidth > 2.0F) {
                        float gradientWidth = currentAnimatedWidth - 2.0F;
                        float targetLineX = x + gradientWidth + x3;
                        float currentLineX = animatedLineX.getOrDefault(effectType, targetLineX);
                        currentLineX = AnimationMath.animation(currentLineX, targetLineX, 0.1F);
                        animatedLineX.put(effectType, currentLineX);
                        r2.horizontalGradient(x, currentAnimatedY, gradientWidth, 40.64F, 3.0F, 0.0F, 0.0F, 3.0F, color,
                              color2);
                        r2.rect(currentLineX, currentAnimatedY, 2.0F, 40.64F, Renderer2D.ColorUtil.getMainColor(1, 1));
                     }

                     Identifier effectTexture = getEffectTexture(effectx.getEffectType());
                     float textureY = y + offsetTexture + y3 - 231.5F;
                     drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, effectTexture, (int) (x - 4.0F + x3),
                           (int) textureY, 10, 10);
                     r2.rect(
                           x + 43.0F - 5.5F + x3,
                           currentAnimatedY + 15.0F,
                           2.0F,
                           11.0F,
                           4.0F,
                           Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 51));
                     boolean isBeneficial = effectx.getEffectType().value().isBeneficial();
                     int textColor = isBeneficial ? Renderer2D.ColorUtil.getTextColor(1, 1)
                           : new Color(16734547).getRGB();
                     r2.text(FontRegistry.INTER_MEDIUM, x + 50.0F - 3.0F + x3, currentAnimatedY + 25.5F, 28.0F, text,
                           textColor);
                     float timeBoxX = x + 58.0F - 3.0F + textWidth + x3;
                     float timeBoxY = currentAnimatedY + 13.0F;
                     r2.rectOutline(
                           timeBoxX,
                           timeBoxY,
                           timeBoxWidth,
                           timeBoxHeight,
                           3.0F,
                           Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 25),
                           1.0F);
                     r2.rect(timeBoxX, timeBoxY, timeBoxWidth, timeBoxHeight, 3.0F,
                           Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 25));
                     float timeTextX = timeBoxX + (timeBoxWidth - timeWidth) / 2.0F;
                     r2.text(FontRegistry.INTER_MEDIUM, timeTextX, currentAnimatedY + 25.5F, 22.0F, timeText,
                           Renderer2D.ColorUtil.getMainColor(1, 1));
                     r2.popAlpha();
                     offsetTexture += 22.5F * currentAlpha;
                     offset += 45.0F * currentAlpha;
                     effectIndex++;
                  }
               }
            }
         }

         maxDurations.keySet()
               .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
         animatedWidths.keySet()
               .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
         animatedLineX.keySet()
               .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
         animatedY.keySet()
               .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
         cachedEffects.keySet().removeIf(key -> !activeEffects.contains(key)
               && (animatedAlphas.get(key) == null || animatedAlphas.get(key).get() <= 0.01F));
         animatedAlphas.keySet().removeIf(key -> {
            Animation2 anim = animatedAlphas.get(key);
            return anim == null || anim.get() <= 0.01F && !activeEffects.contains(key);
         });
      }
   }

   private static String formatDuration(int ticks) {
      int seconds = ticks / 20;
      int minutes = seconds / 60;
      int remainingSeconds = seconds % 60;
      return String.format("%02d:%02d", minutes, remainingSeconds);
   }

   private static Identifier getEffectTexture(RegistryEntry<StatusEffect> effect) {
      return effect.getKey().<Identifier>map(RegistryKey::getValue).map(id -> id.withPrefixedPath("mob_effect/"))
            .orElseGet(MissingSprite::getMissingSpriteId);
   }
}
