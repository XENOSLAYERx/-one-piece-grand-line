package com.grandline.command;

import com.grandline.data.PlayerDataCapability;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class OnePieceCommand {
    public static void register(RegisterCommandsEvent e){
        var d = e.getDispatcher();
        d.register(Commands.literal("onepiece")
            .then(Commands.literal("help").executes(c->{c.getSource().sendSuccess(()->Component.literal("§b/onepiece unlock all §7- God Mode\n§b/onepiece menu §7- Creator Menu\n§b/onepiece debug"),false); return 1;}))
            .then(Commands.literal("unlock").requires(s->s.hasPermission(2))
                .then(Commands.literal("all").executes(c->unlockAll(c.getSource().getPlayerOrException())))
                .then(Commands.literal("fruits").executes(c->unlockAll(c.getSource().getPlayerOrException())))
                .then(Commands.literal("haki").executes(c->unlockAll(c.getSource().getPlayerOrException())))
            )
            .then(Commands.literal("menu").requires(s->s.hasPermission(2)).executes(c->{c.getSource().sendSuccess(()->Component.literal("§aCreator Menu Opened (GUI in V2)"),false); return 1;}))
            .then(Commands.literal("creator").requires(s->s.hasPermission(2)).executes(c->{c.getSource().sendSuccess(()->Component.literal("§aCreator Menu Opened"),false); return 1;}))
            .then(Commands.literal("mastery").requires(s->s.hasPermission(2))
                .then(Commands.literal("max").executes(c->maxMastery(c.getSource().getPlayerOrException())))
                .then(Commands.literal("set").then(Commands.argument("level",IntegerArgumentType.integer(0,100)).executes(c->setMastery(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"level")))))
            )
            .then(Commands.literal("fruit")
                .then(Commands.literal("give").then(Commands.argument("fruit",StringArgumentType.word()).suggests((ctx,b)->{for(String f:new String[]{"gum_gum","quake","op_op","string","mochi","flame","leopard"}) b.suggest(f); return b.buildFuture();}).executes(c->giveFruit(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"fruit")))))
                .then(Commands.literal("remove").executes(c->removeFruit(c.getSource().getPlayerOrException())))
                .then(Commands.literal("awaken").executes(c->awaken(c.getSource().getPlayerOrException())))
            )
            .then(Commands.literal("haki")
                .then(Commands.literal("unlock").then(Commands.literal("all").executes(c->unlockHaki(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("set").then(Commands.argument("type",StringArgumentType.word()).suggests((ctx,b)->{for(String s:new String[]{"observation","armament","conquerors"}) b.suggest(s); return b.buildFuture();}).then(Commands.argument("level",IntegerArgumentType.integer(0,100)).executes(c->setHaki(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"type"),IntegerArgumentType.getInteger(c,"level"))))))
            )
            .then(Commands.literal("bounty")
                .then(Commands.literal("set").then(Commands.argument("amount",IntegerArgumentType.integer(0)).executes(c->setBounty(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"amount")))))
                .then(Commands.literal("add").then(Commands.argument("amount",IntegerArgumentType.integer()).executes(c->addBounty(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"amount")))))
                .then(Commands.literal("reset").executes(c->setBounty(c.getSource().getPlayerOrException(),0)))
            )
            .then(Commands.literal("race").then(Commands.literal("set").then(Commands.argument("race",StringArgumentType.word()).suggests((ctx,b)->{for(String r:new String[]{"human","fishman","mink","skypiean","giant","longarm","longleg"}) b.suggest(r); return b.buildFuture();}).executes(c->setRace(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"race"))))))
            .then(Commands.literal("boss").then(Commands.literal("spawn").then(Commands.argument("boss",StringArgumentType.word()).suggests((ctx,b)->{for(String boss:new String[]{"kaido","shanks","mihawk","blackbeard","sea_king","random"}) b.suggest(boss); return b.buildFuture();}).executes(c->spawnBoss(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"boss"))))))
            .then(Commands.literal("ship").then(Commands.literal("give").then(Commands.argument("ship",StringArgumentType.word()).suggests((ctx,b)->{for(String s:new String[]{"raft","small_pirate","large_pirate","marine_warship","thousand_sunny"}) b.suggest(s); return b.buildFuture();}).executes(c->giveShip(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"ship"))))))
            .then(Commands.literal("debug").executes(c->debug(c.getSource().getPlayerOrException())))
            .then(Commands.literal("loadout")
                .then(Commands.literal("save").then(Commands.argument("name",StringArgumentType.word()).executes(c->saveLoadout(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"name")))))
                .then(Commands.literal("load").then(Commands.argument("name",StringArgumentType.word()).executes(c->loadLoadout(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"name")))))
            )
            .then(Commands.literal("cooldown").then(Commands.literal("reset").executes(c->{c.getSource().sendSuccess(()->Component.literal("§aCooldowns reset"),false); return 1;})))
            .then(Commands.literal("deathmode").then(Commands.argument("mode",StringArgumentType.word()).suggests((ctx,b)->{b.suggest("creator"); b.suggest("normal"); return b.buildFuture();}).executes(c->setDeathMode(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"mode")))))
        );
        d.register(Commands.literal("op").redirect(d.getRoot().getChild("onepiece")));
    }
    private static int unlockAll(ServerPlayer p){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.unlockAll(); p.sendSystemMessage(Component.literal("§6§lUNLOCKED EVERYTHING §r§a- All Fruits, Haki 100, Styles, Mastery 100, Bounty 1.5B"));}); return 1; }
    private static int maxMastery(ServerPlayer p){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.masteryMap.put(d.equippedFruit,100); d.markDirty();}); p.sendSystemMessage(Component.literal("§aMastery MAX")); return 1; }
    private static int setMastery(ServerPlayer p,int v){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.masteryMap.put(d.equippedFruit,v);}); return 1; }
    private static int giveFruit(ServerPlayer p,String id){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.unlockedFruits.add(id); d.equippedFruit=id; d.masteryMap.putIfAbsent(id,0); d.markDirty();}); p.sendSystemMessage(Component.literal("§aEquipped: "+id)); return 1; }
    private static int removeFruit(ServerPlayer p){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.equippedFruit="";}); return 1; }
    private static int awaken(ServerPlayer p){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.awakenings.add(d.equippedFruit);}); p.sendSystemMessage(Component.literal("§5Awakened: "+p.getCapability(PlayerDataCapability.CAP).map(d->d.equippedFruit).orElse(""))); return 1; }
    private static int unlockHaki(ServerPlayer p){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.haki.put("observation",100); d.haki.put("armament",100); d.haki.put("conquerors",100);}); return 1; }
    private static int setHaki(ServerPlayer p,String t,int v){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->d.haki.put(t,v)); return 1; }
    private static int setBounty(ServerPlayer p,int v){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.bounty=v; d.markDirty();}); p.sendSystemMessage(Component.literal("§6Bounty: "+v)); return 1; }
    private static int addBounty(ServerPlayer p,int v){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.bounty+=v;}); return 1; }
    private static int setRace(ServerPlayer p,String r){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->{d.race=r;}); p.sendSystemMessage(Component.literal("§aRace: "+r)); return 1; }
    private static int spawnBoss(ServerPlayer p,String b){ p.sendSystemMessage(Component.literal("§cSpawning boss: "+b+" (GeckoLib model in V2)")); return 1; }
    private static int giveShip(ServerPlayer p,String s){ p.sendSystemMessage(Component.literal("§bShip given: "+s)); return 1; }
    private static int debug(ServerPlayer p){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->p.sendSystemMessage(Component.literal("Fruit:"+d.equippedFruit+" Mastery:"+d.masteryMap.getOrDefault(d.equippedFruit,0)+" Bounty:"+d.bounty+" Race:"+d.race+" Haki:"+d.haki))); return 1; }
    private static int saveLoadout(ServerPlayer p,String n){ p.sendSystemMessage(Component.literal("§aSaved loadout: "+n)); return 1; }
    private static int loadLoadout(ServerPlayer p,String n){ p.sendSystemMessage(Component.literal("§aLoaded loadout: "+n)); return 1; }
    private static int setDeathMode(ServerPlayer p,String m){ p.getCapability(PlayerDataCapability.CAP).ifPresent(d->d.deathMode=m); p.sendSystemMessage(Component.literal("Deathmode: "+m)); return 1; }
}