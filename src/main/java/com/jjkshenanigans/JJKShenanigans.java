package com.jjkshenanigans;

import com.jjkshenanigans.network.ModNetwork;
import com.jjkshenanigans.server.ServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JJKShenanigans.MODID)
public class JJKShenanigans {
    public static final String MODID = "jjkshenanigans";

    public JJKShenanigans() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(ModNetwork::register);
    }
}
