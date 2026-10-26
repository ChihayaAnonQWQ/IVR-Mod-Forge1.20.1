package net.hulan.ksd.mixin;

import mtr.data.Platform;
import mtr.data.SavedRailBase;
import mtr.data.TransportMode;
import mtr.screen.DashboardScreen;
import mtr.screen.PlatformScreen;
import mtr.screen.SavedRailScreenBase;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.hulan.ksd.packet.KSDPacket.KSD_PACKET_UPDATE_PLATFORM;

@Mixin(PlatformScreen.class)
public class PlatformScreenMixin extends SavedRailScreenBase<Platform> {

    public PlatformScreenMixin(SavedRailBase savedRailBase, TransportMode transportMode, DashboardScreen dashboardScreen, Component... additionalTexts) {
        super((Platform) savedRailBase, transportMode, dashboardScreen, additionalTexts);
    }

    @Inject(method = "onClose", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        int minutes = this.sliderDwellTimeMin.getIntValue();
        float second = (float) this.sliderDwellTimeSec.getIntValue() / 2.0F;
        DataUtilities.execute(KSDClientData.PLATFORMS, p -> p.id == savedRailBase.id, platform -> {
            platform.name = savedRailBase.name;
            platform.setDwellTime((int) ((second + (float) (minutes * 60)) * 2.0F), (packet) -> KSDPacketClient.sendUpdate(KSD_PACKET_UPDATE_PLATFORM, packet));
        });
    }

    @Override
    protected String getNumberStringKey() {
        return "gui.mtr.platform_number";
    }

    @Override
    protected ResourceLocation getPacketIdentifier() {
        return null;
    }
}
