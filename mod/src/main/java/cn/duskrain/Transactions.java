package cn.duskrain;

import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Write-ahead after-images bind escrow, balances and player inventory to one revision. */
public final class Transactions {
    static CompoundTag ledger=new CompoundTag();
    static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/duskrain_transactions.dat");}
    static CompoundTag inventory(ServerPlayer p){
        CompoundTag n=new CompoundTag();n.put("items",p.getInventory().save(new ListTag()));n.put("carried",p.containerMenu.getCarried().save(new CompoundTag()));
        n.putInt("level",p.experienceLevel);n.putInt("total",p.totalExperience);n.putFloat("progress",p.experienceProgress);n.putLong("revision",p.getPersistentData().getLong("DuskRainTransaction"));return n;
    }
    static void restore(ServerPlayer p,CompoundTag n){p.getInventory().load(n.getList("items",Tag.TAG_COMPOUND));p.experienceLevel=n.getInt("level");p.totalExperience=n.getInt("total");p.experienceProgress=n.getFloat("progress");p.getPersistentData().putLong("DuskRainTransaction",n.getLong("revision"));p.getInventory().setChanged();}
    static void replace(Store target,CompoundTag tag){Store x=Store.load(tag);target.players.clear();target.players.putAll(x.players);target.claims.clear();target.claims.putAll(x.claims);target.market.clear();target.market.putAll(x.market);target.wildSpawn=x.wildSpawn;target.transactionRevision=x.transactionRevision;target.setDirty();}
    public static boolean commit(ServerPlayer p,String kind,Runnable mutate){
        Store store=Store.get(p.server);CompoundTag before=store.save(new CompoundTag()),inv=inventory(p);long revision=Math.max(store.transactionRevision,ledger.getLong("revision"))+1;
        try{
            mutate.run();store.transactionRevision=revision;p.getPersistentData().putLong("DuskRainTransaction",revision);
            CompoundTag next=ledger.copy();next.putLong("revision",revision);next.putString("kind",kind);next.put("state",store.save(new CompoundTag()));
            CompoundTag players=next.getCompound("players");players.put(p.getUUID().toString(),inventory(p));next.put("players",players);
            Path dest=file(p.server),tmp=dest.resolveSibling(dest.getFileName()+".new");Files.createDirectories(dest.getParent());NbtIo.writeCompressed(next,tmp.toFile());
            try(var channel=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(tmp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);ledger=next;store.setDirty();
        }catch(Exception e){replace(store,before);restore(p,inv);p.containerMenu.setCarried(net.minecraft.world.item.ItemStack.of(inv.getCompound("carried")));DuskRain.LOG.error("Transaction {} aborted before publication",kind,e);Gameplay.say(p,"交易未提交，资产已恢复，请稍后再试。");return false;}
        try{p.containerMenu.broadcastChanges();}catch(Exception e){DuskRain.LOG.warn("Committed transaction notification failed",e);}return true;
    }
    public static void recover(MinecraftServer s){ledger=new CompoundTag();Path f=file(s);if(!Files.exists(f))return;try{ledger=NbtIo.readCompressed(f.toFile());Store store=Store.get(s);if(ledger.getLong("revision")>store.transactionRevision){replace(store,ledger.getCompound("state"));DuskRain.LOG.info("Recovered DuskRain transaction revision {}",store.transactionRevision);}}catch(Exception e){throw new IllegalStateException("Transaction recovery failed; do not accept players",e);}}
    public static void login(ServerPlayer p){CompoundTag n=ledger.getCompound("players").getCompound(p.getUUID().toString());if(n.getLong("revision")>p.getPersistentData().getLong("DuskRainTransaction")){
        restore(p,n);var carried=net.minecraft.world.item.ItemStack.of(n.getCompound("carried"));if(!carried.isEmpty())Gameplay.giveOrRetain(p,carried);Gameplay.say(p,"已恢复上次完成的交易与背包。");
    }}
}
