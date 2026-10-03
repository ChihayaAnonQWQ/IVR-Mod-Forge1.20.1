package net.hulan.ivr;

import mtr.RegistryObject;
import net.hulan.ivr.entity.NPCs;
import net.hulan.ivr.item.ItemKCRPSDAPGBase;
import net.hulan.ivr.item.ItemNPCSpawnEgg;
import net.minecraft.world.item.Item;

public interface IVRItems {
    RegistryObject<Item> KCR_APG_DOOR = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_DOOR, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.APG));
    RegistryObject<Item> KCR_APG_GLASS = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_GLASS, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.APG));
    RegistryObject<Item> KCR_APG_GLASS_END = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_GLASS_END, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.APG));
    RegistryObject<Item> KCR_PSD_DOOR_1 = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_DOOR, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.PSD_1));
    RegistryObject<Item> KCR_PSD_GLASS_1 = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_GLASS, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.PSD_1));
    RegistryObject<Item> KCR_PSD_GLASS_END_1 = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_GLASS_END, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.PSD_1));
    RegistryObject<Item> KCR_PSD_DOOR_2 = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_DOOR, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.PSD_2));
    RegistryObject<Item> KCR_PSD_GLASS_2 = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_GLASS, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.PSD_2));
    RegistryObject<Item> KCR_PSD_GLASS_END_2 = new RegistryObject<>(() -> new ItemKCRPSDAPGBase(ItemKCRPSDAPGBase.EnumKCRPSDAPGItem.PSD_APG_GLASS_END, ItemKCRPSDAPGBase.EnumKCRPSDAPGType.PSD_2));
    RegistryObject<Item> TICKETS_SPAWN_EGG = new RegistryObject<>(() -> new ItemNPCSpawnEgg(NPCs.TICKETS, 0x2B3D8F, 0xE7C44F, IVRCreativeModTabs.IVR_));
    RegistryObject<Item> FA_SPAWN_EGG = new RegistryObject<>(() -> new ItemNPCSpawnEgg(NPCs.FARE_ADJUSTMENTS, 0x3B3B3B, 0xD9D9D9, IVRCreativeModTabs.IVR_));
}
