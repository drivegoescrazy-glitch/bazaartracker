package com.example.bazaartracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BazaarTrackerClient implements ClientModInitializer {
    private static final String API_URL = "https://api.hypixel.net/skyblock/bazaar";
    private static final double BUDGET_COINS = 50_000_000.0;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "bazaartracker-fetcher");
        thread.setDaemon(true);
        return thread;
    });

    private volatile List<FlipCandidate> topFlips = List.of();
    private volatile String statusLine = "Loading Hypixel Bazaar data...";

    @Override
    public void onInitializeClient() {
        refreshData();
        scheduler.scheduleAtFixedRate(this::refreshData, 30, 30, TimeUnit.SECONDS);

        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void refreshData() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "BazaarTrackerMod/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                statusLine = "Bazaar API error: HTTP " + response.statusCode();
                return;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                statusLine = "Bazaar API returned unsuccessful response.";
                return;
            }

            JsonObject products = root.getAsJsonObject("products");
            if (products == null) {
                statusLine = "Bazaar API missing products.";
                return;
            }

            List<FlipCandidate> candidates = new ArrayList<>();

            for (String productId : products.keySet()) {
                JsonObject product = products.getAsJsonObject(productId);
                if (product == null || !product.has("quick_status")) {
                    continue;
                }

                JsonObject quick = product.getAsJsonObject("quick_status");
                double buyPrice = getDouble(quick.get("buyPrice"));
                double sellPrice = getDouble(quick.get("sellPrice"));
                double buyMovingWeek = getDouble(quick.get("buyMovingWeek"));

                if (buyPrice <= 0 || sellPrice <= 0 || buyMovingWeek <= 0) {
                    continue;
                }

                double margin = sellPrice - buyPrice;
                if (margin <= 0) {
                    continue;
                }

                double buysPerHour = buyMovingWeek / 168.0;
                double affordableUnits = BUDGET_COINS / buyPrice;
                double tradableUnitsPerHour = Math.min(affordableUnits, buysPerHour);
                double hourlyProfit = tradableUnitsPerHour * margin;

                candidates.add(new FlipCandidate(productId, buyPrice, sellPrice, margin, buysPerHour, hourlyProfit));
            }

            candidates.sort(Comparator.comparingDouble(FlipCandidate::hourlyProfit).reversed());
            topFlips = candidates.stream().limit(5).toList();
            statusLine = topFlips.isEmpty()
                    ? "No positive-margin flips found."
                    : "Top flips updated (budget: 50,000,000 coins).";
        } catch (InterruptedException e) {
            statusLine = "Failed to fetch bazaar data: InterruptedException";
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            statusLine = "Failed to fetch bazaar data: IOException";
        } catch (Exception e) {
            statusLine = "Unexpected error: " + e.getClass().getSimpleName();
        }
    }

    private static double getDouble(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return 0.0;
        }
        return element.getAsDouble();
    }

    private void renderHud(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null || client.player == null || client.currentScreen != null && client.currentScreen.shouldPause()) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        int x = 8;
        int y = 8;

        context.drawText(tr, Text.literal("Bazaar Flips (50M budget)"), x, y, 0x00FFFF, true);
        y += 12;
        context.drawText(tr, Text.literal(statusLine), x, y, 0xFFFFFF, true);
        y += 12;

        int index = 1;
        for (FlipCandidate flip : topFlips) {
            String line = String.format(Locale.US,
                    "%d) %s | buys/h: %.0f | buy: %.1f | sell: %.1f | margin: %.1f",
                    index++, flip.productId(), flip.buysPerHour(), flip.buyPrice(), flip.sellPrice(), flip.margin());
            context.drawText(tr, Text.literal(line), x, y, 0x55FF55, true);
            y += 10;
        }
    }

    private record FlipCandidate(
            String productId,
            double buyPrice,
            double sellPrice,
            double margin,
            double buysPerHour,
            double hourlyProfit
    ) {
    }
}
