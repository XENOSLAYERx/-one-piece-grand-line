package com.grandline.data;

import net.minecraft.nbt.CompoundTag;
import java.util.*;

public class PlayerData {
    public Set<String> unlockedFruits = new HashSet<>();
    public String equippedFruit = "";
    public Map<String, Integer> masteryMap = new HashMap<>();
    public int bounty = 0;
    public String race = "human";
    public String faction = "pirate";
    public Map<String, Integer> haki = new HashMap<>() {{ put("observation",0); put("armament",0); put("conquerors",0); }};
    public Set<String> fightingStyles = new HashSet<>();
    public Set<String> awakenings = new HashSet<>();
    public boolean isSuppressed = false;
    public String deathMode = "normal";
    public Map<String, Object> loadouts = new HashMap<>();
    public boolean creatorMode = false;
    private boolean dirty = false;
    public void markDirty(){ dirty=true; }
    public boolean isDirty(){ return dirty; }
    public void clearDirty(){ dirty=false; }

    public void unlockAll(){
        unlockedFruits.addAll(List.of("gum_gum","quake","op_op","string","mochi","barrier","paw","magnet","gravity","soul","shadow","flame","leopard"));
        haki.put("observation",100); haki.put("armament",100); haki.put("conquerors",100);
        fightingStyles.addAll(List.of("black_leg","fishman_karate","rokushiki_all","three_sword","electro"));
        awakenings.addAll(unlockedFruits);
        masteryMap.replaceAll((k,v)->100);
        for(String f: unlockedFruits) masteryMap.put(f,100);
        bounty = 1500000000;
        race = "human";
        markDirty();
    }

    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putString("equippedFruit", equippedFruit);
        tag.putInt("bounty", bounty);
        tag.putString("race", race);
        tag.putString("faction", faction);
        tag.putString("deathMode", deathMode);
        CompoundTag hakiTag = new CompoundTag();
        haki.forEach(hakiTag::putInt);
        tag.put("haki", hakiTag);
        tag.putInt("mastery", masteryMap.getOrDefault(equippedFruit,0));
        return tag;
    }
    public void load(CompoundTag tag){
        equippedFruit = tag.getString("equippedFruit");
        bounty = tag.getInt("bounty");
        race = tag.getString("race");
        if(tag.contains("haki")){
            CompoundTag ht = tag.getCompound("haki");
            for(String k: ht.getAllKeys()) haki.put(k, ht.getInt(k));
        }
    }
}