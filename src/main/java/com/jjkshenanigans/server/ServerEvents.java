package com.jjkshenanigans.server;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerEvents {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) Tasks.tick();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.side.isServer() && e.player instanceof ServerPlayer sp) {
            GojoAbilities.tickPlayer(sp);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer sp) {
            GojoAbilities.onDealDamage(sp, e.getAmount());
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) GojoAbilities.onDeath(sp);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) GojoAbilities.remove(sp);
    }

    @SubscribeEvent
    public void onStop(ServerStoppingEvent e) {
        Tasks.clear();
        GojoAbilities.clearAll();
    }

    /** /jjk gojo  -> te conviertes en Gojo | /jjk none -> sales | /jjk fill -> llena Six Eyes (para probar) */
    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("jjk")
                .then(Commands.literal("gojo").executes(c -> {
                    GojoAbilities.activate(c.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("none").executes(c -> {
                    GojoAbilities.activate(c.getSource().getPlayerOrException(), false);
                    return 1;
                }))
                .then(Commands.literal("fill").executes(c -> {
                    GojoAbilities.fillSixEyes(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }
}
