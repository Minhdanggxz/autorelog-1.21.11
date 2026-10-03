package com.example.autology;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

public class AutoLogYClient implements ClientModInitializer {
    private static final double LOG_Y = -5.0;
    private static final int RECONNECT_DELAY_TICKS = 5; // 5 ticks = 0.25s

    private static ServerInfo pendingServer = null;
    private static int ticksLeft = 0;
    private static boolean armed = true;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingServer != null) {
                if (--ticksLeft <= 0) {
                    ServerInfo info = pendingServer;
                    pendingServer = null;
                    ConnectScreen.connect(
                            new MultiplayerScreen(new TitleScreen()),
                            client,
                            ServerAddress.parse(info.address),
                            info,
                            false,
                            null);
                }
                return;
            }

            if (client.player == null || client.world == null) {
                return;
            }

            double y = client.player.getY();
            if (y > LOG_Y) {
                armed = true;
            }

            if (armed && y <= LOG_Y) {
                ServerInfo info = client.getCurrentServerEntry();
                ClientPlayNetworkHandler handler = client.getNetworkHandler();
                if (info == null || handler == null || !handler.getConnection().isOpen()) {
                    return;
                }
                armed = false;
                pendingServer = info;
                ticksLeft = RECONNECT_DELAY_TICKS;
                handler.getConnection().disconnect(
                        Text.literal("AutoLogY: relogging at Y = " + (int) LOG_Y));
            }
        });
    }
}
