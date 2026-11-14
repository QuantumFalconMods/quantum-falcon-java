package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.breeding.BreedingService;
import com.matt.quantumfalcon.network.CodexPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** The Falconer's Codex: right-click to open the in-game guide (falcons, breeding, eggs, nests, realm blocks, controls). */
public class QuantumCodexItem extends Item {
    public QuantumCodexItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            List<String> found = new ArrayList<>(BreedingService.discovered(sp));
            if (found.size() > 512) found = found.subList(0, 512);
            found = new ArrayList<>(found); found.addAll(com.matt.quantumfalcon.codex.RiftCodex.codexLines(sp));   // A1 Rift Codex tab
            PacketDistributor.sendToPlayer(sp, new CodexPayload(found, 0, 0));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Codex on a falcon: its genome and pedigree. Works on any falcon so you can judge wild ones before taming. */
    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
        if (!(target instanceof com.matt.quantumfalcon.entity.QuantumFalconEntity f)) return net.minecraft.world.InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            var g = f.getGenome();
            PacketDistributor.sendToPlayer(sp, new com.matt.quantumfalcon.network.PedigreePayload(g.save(), "falcon", f.getName().getString(), BreedingService.pity(sp, g.tier)));
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    /** Codex on an egg block: the unborn chick's genome and pedigree. */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof com.matt.quantumfalcon.blockentity.FalconEggBlockEntity be)) return net.minecraft.world.InteractionResult.PASS;
        var g = be.getChild();
        if (g == null) return net.minecraft.world.InteractionResult.PASS;
        if (ctx.getPlayer() instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new com.matt.quantumfalcon.network.PedigreePayload(g.save(), "egg", "", BreedingService.pity(sp, g.tier)));
        return net.minecraft.world.InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.quantumfalcon.quantum_codex.tooltip").withStyle(ChatFormatting.DARK_AQUA));
    }
}
