package com.github.mikumiku.addon.impl.v1211;

import net.minecraft.item.*;

public class ItemUtil implements com.github.mikumiku.addon.util.ItemUtil {
    @Override
    public boolean isArmorItem(Item item) {
        return item instanceof ArmorItem;
    }

    @Override
    public boolean isPickaxeItem(Item item) {
        return item instanceof PickaxeItem;
    }

    @Override
    public boolean isTool(Item item) {
        return item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem;
    }

    @Override
    public boolean isSword(Item item) {
        return item instanceof SwordItem;
    }
}
