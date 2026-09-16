package cn.duskrain;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(DuskRain.ID) @PrefixGameTestTemplate(false)
public final class CatalogTests {
    @GameTest(template="empty") public static void paging_actions_execute_through_brigadier(GameTestHelper h){
        var p=IntegrationTests.servicePlayer(h,"DRPageButtons","artifacts");
        for(int page=0;page<12;page++){
            var action=Actions.parse("dr shop page forge:all:-1:all "+page);
            String command=Actions.command(action,p);IntegrationTests.command(p,command);
            var opened=ShopCatalog.OPEN.get(p.getUUID());
            h.assertTrue(opened!=null&&opened.page()==page,"Actual button action survives Brigadier including colon-delimited filter on page "+page);
        }
        for(String filter:List.of("forge:robe:-1:all","forge:robe:5:helmet","forge:artifact:0:all")){
            IntegrationTests.command(p,Actions.command(Actions.parse("dr shop page "+filter+" 0"),p));
            h.assertTrue(ShopCatalog.OPEN.get(p.getUUID()).key().equals(filter),"Filter action survives command parsing");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void ninety_products_roundtrip_every_server_page(GameTestHelper h){
        Set<String> all=new HashSet<>();var initial=ShopCatalog.menu(new ShopCatalog.Query("forge","all",-1,"all",0));
        h.assertTrue(initial.catalog().total()==105,"The real 21 artifacts + 84 armor pieces are present");
        for(int page=0;page<initial.catalog().pages();page++){
            var sent=ShopCatalog.menu(new ShopCatalog.Query("forge","all",-1,"all",page));var buf=new FriendlyByteBuf(Unpooled.buffer());
            try{Network.writeMenu(sent,buf);var got=Network.readMenu(buf);h.assertTrue(sent.equals(got)&&got.entries().size()<=8,"Each page survives the actual packet codec");got.entries().forEach(e->h.assertTrue(all.add(e.action()),"No duplicate product across pages"));}finally{buf.release();}
        }
        h.assertTrue(all.size()==105,"Every product is reachable");
        var filtered=ShopCatalog.menu(new ShopCatalog.Query("forge","robe",5,"helmet",0));
        h.assertTrue(filtered.entries().size()==3&&filtered.entries().stream().allMatch(e->e.action().endsWith("helmet")),"Realm and armor slot filtering");h.succeed();
    }
    @GameTest(template="empty") public static void oversized_menu_rejected_before_encoding(GameTestHelper h){
        var buf=new FriendlyByteBuf(Unpooled.buffer());boolean rejected=false;
        try{try{Network.writeMenu(new Network.Menu("test","",Collections.nCopies(61,new Network.Entry("x",""))),buf);}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected&&buf.writerIndex()==0,"Oversized menu is refused before any bytes are emitted");
            buf.writeUtf("x",128);buf.writeUtf("",4096);buf.writeVarInt(61);rejected=false;try{Network.readMenu(buf);}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected,"Decoder enforces the same limit");
        }finally{buf.release();}h.succeed();
    }
}
