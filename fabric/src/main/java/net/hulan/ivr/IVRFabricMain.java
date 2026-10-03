package net.hulan.ivr;

import mtr.CreativeModeTabs;
import mtr.RegistryObject;
import mtr.item.ItemBlockEnchanted;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.FabricRegistryUtilities;
import mtr.mappings.RegistryUtilities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.hulan.ivr.platform.NPCPlatform;
import net.hulan.ivr.entity.NPC;
import net.hulan.ivr.item.ItemNPCSpawnEgg;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

import static net.hulan.ivr.IVR.MOD_ID;

public class IVRFabricMain implements ModInitializer {

    @Override
    public void onInitialize() {
        IVR.init(
                IVRFabricMain::registerItem,
                IVRFabricMain::registerBlock,
                IVRFabricMain::registerBlockItem,
                IVRFabricMain::registerBlockEntityType,
                IVRFabricMain::registerEnchantedBlockItem);
        NPCPlatform.getInstance().registerEntities(IVRFabricMain::registerEntity);
        NPCPlatform.getInstance().registerAttributes();
    }

    private static void registerItem(String path, RegistryObject<Item> item) {
        Item itemObject = item.get();
        Registry.register(RegistryUtilities.registryGetItem(), new ResourceLocation(MOD_ID, path), itemObject);
        if (itemObject instanceof ItemWithCreativeTabBase) {
            FabricRegistryUtilities.registerCreativeModeTab(((ItemWithCreativeTabBase)itemObject).creativeModeTab.get(), itemObject);
        } else if (itemObject instanceof ItemWithCreativeTabBase.ItemPlaceOnWater) {
            FabricRegistryUtilities.registerCreativeModeTab(((ItemWithCreativeTabBase.ItemPlaceOnWater)itemObject).creativeModeTab.get(), itemObject);
        } else if (itemObject instanceof ItemNPCSpawnEgg npcSpawnEgg) {
            FabricRegistryUtilities.registerCreativeModeTab(npcSpawnEgg.creativeModeTab.get(), itemObject);
        }
    }

    private static void registerBlock(String path, RegistryObject<Block> block) {
        Registry.register(RegistryUtilities.registryGetBlock(), new ResourceLocation(MOD_ID, path), block.get());
    }

    private static void registerBlockItem(String path, RegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTab) {
        registerBlock(path, block);
        final BlockItem blockItem = new BlockItem(block.get(), RegistryUtilities.createItemProperties(creativeModeTab::get));
        Registry.register(RegistryUtilities.registryGetItem(), new ResourceLocation(MOD_ID, path), blockItem);
        FabricRegistryUtilities.registerCreativeModeTab(creativeModeTab.get(), blockItem);
    }

    private static void registerEnchantedBlockItem(String path, RegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTab) {
        registerBlock(path, block);
        Objects.requireNonNull(creativeModeTab);
        ItemBlockEnchanted itemBlockEnchanted = new ItemBlockEnchanted(block.get(), RegistryUtilities.createItemProperties(creativeModeTab::get));
        Registry.register(RegistryUtilities.registryGetItem(), new ResourceLocation(MOD_ID, path), itemBlockEnchanted);
        FabricRegistryUtilities.registerCreativeModeTab(creativeModeTab.get(), itemBlockEnchanted);
    }

    private static void registerBlockEntityType(String path, RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>> blockEntityType) {
        Registry.register(RegistryUtilities.registryGetBlockEntityType(), new ResourceLocation(MOD_ID, path), (BlockEntityType<? extends BlockEntityMapper>)blockEntityType.get());
    }

    private static void registerEntity(String path, RegistryObject<? extends EntityType<? extends NPC>> entity) {
        ResourceLocation id = new ResourceLocation(MOD_ID, path);
        Registry.register(RegistryUtilities.registryGetEntityType(), id, entity.get());
        IVR.LOGGER.info("Registered NPC entity {}", id);
    }
}
