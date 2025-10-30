package com.github.mikumiku.addon.util;

import net.minecraft.item.Item;

public interface ItemUtil {
    boolean isArmorItem(Item item);
    boolean isPickaxeItem(Item item);
    boolean isTool(Item item);
    boolean isSword(Item item);
}
