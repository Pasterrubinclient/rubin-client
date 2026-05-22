package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AuctionHelper - подсвечивает самый дешёвый лот на аукционе.
 * Парсит цены из лора/тултипов предметов и отправляет в чат информацию.
 */
@IModule(name = "Auction Helper", description = "Подсвечивает дешёвые лоты на аукционе", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class AuctionHelper extends Module {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d[\\d\\s.,]*)");
    private static final Set<String> PRICE_KEYWORDS = Set.of(
            "цена", "price", "стоим", "руб", "монет", "coins", "коин", "buy", "куп", "$", "₽"
    );
    private static final Set<String> AUCTION_KEYWORDS = Set.of(
            "аукцион", "аук", "поиск", "рынок", "лот", "auction", "search", "market", "trade", "/ah", " ah"
    );

    private Slot cheapestSlot;
    private Slot alternativeSlot;
    private long lastScanTime;

    public AuctionHelper() {
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cheapestSlot = null;
        alternativeSlot = null;
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;

        Screen screen = mc.currentScreen;
        if (!(screen instanceof GenericContainerScreen containerScreen)) {
            cheapestSlot = null;
            alternativeSlot = null;
            return;
        }

        if (!isAuctionScreen(containerScreen)) return;

        long now = System.currentTimeMillis();
        if (now - lastScanTime < 500) return;
        lastScanTime = now;

        scanAuction(containerScreen);
    }

    private boolean isAuctionScreen(GenericContainerScreen screen) {
        String title = stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
        return AUCTION_KEYWORDS.stream().anyMatch(title::contains);
    }

    private void scanAuction(GenericContainerScreen screen) {
        List<SlotPrice> prices = new ArrayList<>();

        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot == null || slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            int price = parsePrice(stack);
            if (price > 0) {
                int count = Math.max(1, stack.getCount());
                prices.add(new SlotPrice(slot, price, count));
            }
        }

        if (prices.isEmpty()) {
            cheapestSlot = null;
            alternativeSlot = null;
            return;
        }

        // Find cheapest total price
        prices.sort(Comparator.comparingInt(SlotPrice::totalPrice));
        cheapestSlot = prices.get(0).slot();

        // Find best price per item (alternative)
        alternativeSlot = null;
        double bestPricePerItem = Double.MAX_VALUE;
        for (SlotPrice sp : prices) {
            if (sp.slot() == cheapestSlot) continue;
            double perItem = (double) sp.totalPrice() / sp.count();
            if (perItem < bestPricePerItem) {
                bestPricePerItem = perItem;
                alternativeSlot = sp.slot();
            }
        }

        // Notify in chat
        SlotPrice best = prices.get(0);
        String itemName = stripFormatting(best.slot().getStack().getName().getString());
        mc.inGameHud.getChatHud().addMessage(
                Text.literal("§a[AuctionHelper] §fСамый дешёвый: §e" + itemName +
                        " §7- §6" + formatPrice(best.totalPrice()) + " монет" +
                        (best.count() > 1 ? " §7(" + formatPrice(best.totalPrice() / best.count()) + "/шт)" : ""))
        );
    }

    private int parsePrice(ItemStack stack) {
        List<String> lines = getLoreLines(stack);
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            boolean hasPriceKeyword = PRICE_KEYWORDS.stream().anyMatch(lower::contains);
            if (!hasPriceKeyword) continue;

            Matcher matcher = PRICE_PATTERN.matcher(line);
            while (matcher.find()) {
                String numStr = matcher.group(1).replaceAll("[\\s.,]", "");
                try {
                    long value = Long.parseLong(numStr);
                    // Check for multiplier suffix (k, kk, m)
                    int endIdx = matcher.end(1);
                    long multiplier = getMultiplier(lower, endIdx);
                    value *= multiplier;
                    if (value > 0 && value <= Integer.MAX_VALUE) {
                        return (int) value;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    private long getMultiplier(String text, int afterNumber) {
        if (afterNumber >= text.length()) return 1;
        char c = text.charAt(afterNumber);
        char c2 = afterNumber + 1 < text.length() ? text.charAt(afterNumber + 1) : 0;
        if (c == 'k' || c == 'к') {
            if (c2 == 'k' || c2 == 'к') return 1_000_000;
            return 1_000;
        }
        if (c == 'm' || c == 'м') return 1_000_000;
        return 1;
    }

    private List<String> getLoreLines(ItemStack stack) {
        List<String> result = new ArrayList<>();
        result.add(stripFormatting(stack.getName().getString()));

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                result.add(stripFormatting(line.getString()));
            }
        }
        return result;
    }

    private String stripFormatting(String input) {
        if (input == null) return "";
        String stripped = Formatting.strip(input);
        return stripped != null ? stripped : input;
    }

    private String formatPrice(int price) {
        if (price < 1000) return String.valueOf(price);
        StringBuilder sb = new StringBuilder(String.valueOf(price));
        for (int i = sb.length() - 3; i > 0; i -= 3) {
            sb.insert(i, '.');
        }
        return sb.toString();
    }

    public Slot getCheapestSlot() { return cheapestSlot; }
    public Slot getAlternativeSlot() { return alternativeSlot; }

    private record SlotPrice(Slot slot, int totalPrice, int count) {}
}
