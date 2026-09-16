package cn.duskrain;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A server-paged counter; scrolling never requests or caches the full catalogue. */
public final class MerchantScreen extends Screen {
    final Network.Menu menu;
    int left, top, pw, ph, page, cols, size, scrollRow, rows;
    MerchantScreen(Network.Menu menu) { super(Component.literal(menu.title())); this.menu=menu; }
    void button(int x,int y,int w,String label,Runnable action,boolean active) {
        var b=new ClientUI.Tile(x,y,w,20,new Network.Entry(label,""),action,true);
        b.active=active; addRenderableWidget(b);
    }
    String key(String kind,int realm,String part) { return menu.catalog().category()+":"+kind+":"+realm+":"+part; }
    String filter() { var q=menu.catalog(); return key(q.kind(),q.realm(),q.part()); }
    void request(String key,int page) { Network.action("dr shop page "+key+" "+page); }
    @Override protected void init() {
        pw=Math.min(width-16,620); ph=Math.min(height-16,374); left=(width-pw)/2; top=(height-ph)/2;
        cols=pw>=300?2:1; boolean catalog=menu.catalog()!=null; int body=catalog?115:87;
        rows=Math.max(1,(ph-body-43)/49); size=cols*rows; int first;
        if(catalog) {
            var q=menu.catalog(); int fw=(pw-40)/3;
            String kind=switch(q.kind()){case "artifact"->"法器";case "robe"->"法衣";default->"全部类别";};
            button(left+16,top+82,fw,kind,()->request(key(switch(q.kind()){case "all"->"artifact";case "artifact"->"robe";default->"all";},q.realm(),"all"),0),q.category().equals("forge"));
            button(left+20+fw,top+82,fw,q.realm()<0?"全部境界":Rules.current.realms[q.realm()],()->request(key(q.kind(),q.realm()==6?-1:q.realm()+1,q.part()),0),true);
            String[] parts={"all","helmet","chestplate","leggings","boots"},labels={"全部部位","发冠","上衣","下裳","鞋履"};
            int part=java.util.Arrays.asList(parts).indexOf(q.part()), next=(part+1)%parts.length;
            button(left+24+fw*2,top+82,fw,labels[Math.max(0,part)],()->request(key("robe",q.realm(),parts[next]),0),q.category().equals("forge"));
            scrollRow=Math.max(0,Math.min(scrollRow,Math.max(0,(menu.entries().size()+cols-1)/cols-rows)));
            first=scrollRow*cols;
            button(left+16,top+ph-30,57,"← 上页",()->request(filter(),q.page()-1),q.page()>0);
            button(left+78,top+ph-30,57,"下页 →",()->request(filter(),q.page()+1),q.page()+1<q.pages());
        } else {
            page=Math.max(0,Math.min(page,(Math.max(1,menu.entries().size())-1)/size)); first=page*size;
            button(left+16,top+ph-30,57,"← 上页",()->{page--;rebuildWidgets();},page>0);
            button(left+78,top+ph-30,57,"下页 →",()->{page++;rebuildWidgets();},(page+1)*size<menu.entries().size());
        }
        int tw=(pw-32-(cols-1)*8)/cols;
        for(int i=first;i<Math.min(menu.entries().size(),first+size);i++) {
            var e=menu.entries().get(i); int n=i-first;
            addRenderableWidget(new ClientUI.Tile(left+16+n%cols*(tw+8),top+body+n/cols*49,tw,43,e,()->ClientUI.activate(e.action()),false));
        }
        button(left+pw-59,top+ph-30,43,"收卷",this::onClose,true);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        g.fill(0,0,width,height,0x6A0C2029); InkStyle.sheet(g,left,top,pw,ph);
        g.fill(left+3,top+3,left+pw-3,top+42,0xFF233F45);
        g.drawString(font,font.plainSubstrByWidth(menu.title(),pw-30),left+16,top+11,0xF2DCAB,false);
        String balance=ClientUI.snapshot==null?"DuskRain · 烟雨仙途":"灵石 "+ClientUI.snapshot.money()+"  ·  "+Rules.realm(ClientUI.snapshot.stage());
        g.drawString(font,balance,left+16,top+26,0xBDD8D0,false);
        int y=top+50,lines=3;
        for(var line:font.split(Component.literal(menu.description()),pw-32)){if(lines--<=0)break;g.drawString(font,line,left+16,y,InkStyle.MUTED,false);y+=10;}
        if(menu.catalog()!=null&&menu.entries().size()>size)g.drawString(font,"滚轮查看本页其余商品",left+16,top+104,InkStyle.MUTED,false);
        var q=menu.catalog(); int current=q==null?page:q.page(), count=q==null?Math.max(1,(menu.entries().size()+size-1)/size):q.pages();
        if(pw>=320)g.drawCenteredString(font,"卷 "+(current+1)+" / "+count,left+pw/2,top+ph-23,InkStyle.INK);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(delta==0)return false;
        if(menu.catalog()!=null){scrollRow+=delta>0?-1:1;rebuildWidgets();return true;}
        page+=delta>0?-1:1;rebuildWidgets();return true;
    }
    @Override public boolean isPauseScreen(){return false;}
}
