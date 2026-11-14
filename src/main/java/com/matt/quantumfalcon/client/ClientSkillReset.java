package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.ClientSkillData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Clears the client's skill copy on logout so another world's points never show (skill tree audit B1). */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class ClientSkillReset {
    private ClientSkillReset() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSkillData.reset();
    }
}
