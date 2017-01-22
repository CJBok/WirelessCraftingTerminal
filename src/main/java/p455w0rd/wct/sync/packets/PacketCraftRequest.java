package p455w0rd.wct.sync.packets;

import java.util.concurrent.Future;

import appeng.api.networking.*;
import appeng.api.networking.crafting.*;
import io.netty.buffer.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import p455w0rd.wct.container.*;
import p455w0rd.wct.handlers.GuiHandler;
import p455w0rd.wct.sync.WCTPacket;
import p455w0rd.wct.sync.network.INetworkInfo;

public class PacketCraftRequest extends WCTPacket {

	private final long amount;
	private final boolean heldShift;

	public PacketCraftRequest(final ByteBuf stream) {
		heldShift = stream.readBoolean();
		amount = stream.readLong();
	}

	public PacketCraftRequest(final int craftAmt, final boolean shift) {
		amount = craftAmt;
		heldShift = shift;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());
		data.writeBoolean(shift);
		data.writeLong(amount);

		configureWrite(data);
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WCTPacket packet, final EntityPlayer player) {
		if (player.openContainer instanceof ContainerCraftAmount) {
			final ContainerCraftAmount cca = (ContainerCraftAmount) player.openContainer;
			final Object target = cca.getTarget();
			if (target instanceof IGridHost) {
				final IGrid g = cca.obj2.getTargetGrid();
				if (g == null || cca.getItemToCraft() == null) {
					return;
				}

				cca.getItemToCraft().setStackSize(amount);

				Future<ICraftingJob> futureJob = null;
				try {
					final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
					futureJob = cg.beginCraftingJob(cca.getWorld(), cca.getGrid(), cca.getActionSrc(), cca.getItemToCraft(), null);

					int x = (int) player.posX;
					int y = (int) player.posY;
					int z = (int) player.posZ;

					GuiHandler.open(GuiHandler.GUI_CRAFT_CONFIRM, player, player.worldObj, new BlockPos(x, y, z));

					if (player.openContainer instanceof ContainerCraftConfirm) {
						final ContainerCraftConfirm ccc = (ContainerCraftConfirm) player.openContainer;
						ccc.setAutoStart(heldShift);
						ccc.setJob(futureJob);
						cca.detectAndSendChanges();
					}
				}
				catch (final Throwable e) {
					if (futureJob != null) {
						futureJob.cancel(true);
					}
				}
			}
		}
	}
}