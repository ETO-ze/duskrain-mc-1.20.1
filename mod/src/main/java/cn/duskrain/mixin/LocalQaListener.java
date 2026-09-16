package cn.duskrain.mixin;
import java.net.InetAddress;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
/** The opt-in development harness never exposes its unauthenticated test accounts to the LAN. */
@Mixin(value=ServerConnectionListener.class,remap=false)
public abstract class LocalQaListener {
 @ModifyVariable(method={"startTcpServerListener","m_9711_"},remap=false,at=@At("HEAD"),argsOnly=true,ordinal=0)
 private InetAddress localOnly(InetAddress address){return Boolean.getBoolean("duskrain.director")?InetAddress.getLoopbackAddress():address;}
}
