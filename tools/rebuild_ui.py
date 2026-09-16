from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=ROOT/'mod/src/main/java/cn/duskrain/ClientUI.java'
s=p.read_text(encoding='utf-8')
a=s.index('    static final class DRScreen')
s=s[:a]+r'''
    static final int INK=0xFF253B3D,TEAL=0xFF376C6D,GOLD=0xFFB29A65,PAPER=0xFFF0ECDf,MUTED=0xFF697E7A;
    static final class DRScreen extends Screen {
        final Network.Menu menu;int page,left,top,pw,ph,nav,cx,cw,rows,cols,size,body;
        final String[][] tabs={{"仙途首页","menu"},{"修炼境界","profile"},{"六章纪事","quests"},{"丹器集市","shop view material"},{"云渡山河","warp"},{"结伴问劫","trial"},{"显示设置","sidebar"}};
        boolean journal,profile,shop,skill;
        DRScreen(Network.Menu menu){super(Component.literal(menu.title()));this.menu=menu;journal=menu.title().startsWith("烟雨纪事");profile=menu.title().startsWith("修炼功法");shop=menu.title().startsWith("灵石商店")||menu.title().startsWith("商品");skill=menu.title().startsWith("流派技能");if(journal&&snapshot!=null)page=Math.min(5,snapshot.main()/4);}
        @Override protected void init(){
            pw=Math.min(width-12,740);ph=Math.min(height-12,414);left=(width-pw)/2;top=(height-ph)/2;nav=pw>460?94:68;cx=left+nav+18;cw=pw-nav-34;
            body=top+134;cols=cw>=285?2:1;rows=Math.max(1,(ph-178)/49);size=cols*rows;
            if(journal){cols=2;rows=2;size=4;}
            if(shop&&cw>=440){cols=3;size=rows*cols;}
            page=Math.max(0,Math.min(page,(Math.max(1,menu.entries().size())-1)/size));
            int navGap=Math.min(32,(ph-82)/7);
            for(int i=0;i<tabs.length;i++){final String command=tabs[i][1];addRenderableWidget(new Tile(left+9,top+67+i*navGap,nav-12,navGap-3,new Network.Entry(tabs[i][0],"dr "+command),()->Network.action("dr "+command),true));}
            for(int i=page*size;i<Math.min(menu.entries().size(),(page+1)*size);i++){var e=menu.entries().get(i);int j=i-page*size,tw=(cw-(cols-1)*9)/cols;addRenderableWidget(new Tile(cx+j%cols*(tw+9),body+j/cols*49,tw,43,e,()->Network.action(e.action()),false));}
            if(page>0)addRenderableWidget(new Tile(cx,top+ph-29,57,20,new Network.Entry("← 上一页",""),()->{page--;rebuildWidgets();},true));
            if((page+1)*size<menu.entries().size())addRenderableWidget(new Tile(cx+63,top+ph-29,57,20,new Network.Entry("下一页 →",""),()->{page++;rebuildWidgets();},true));
            addRenderableWidget(new Tile(left+pw-54,top+12,37,18,new Network.Entry("收卷",""),this::onClose,true));
            if(journal)for(int i=0;i<6;i++){final int chapter=i;addRenderableWidget(new Tile(cx+i*cw/6,top+66,cw/6-3,20,new Network.Entry(new String[]{"一·入山","二·问心","三·凝丹","四·游神","五·证道","六·问劫"}[i],""),()->{page=chapter;rebuildWidgets();},true));}
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0x8A0C2029);g.fill(left-2,top+4,left+pw+3,top+ph+4,0xA0000000);g.fill(left,top,left+pw,top+ph,PAPER);
            // An original ink landscape is drawn beneath the paper, independent of shaders.
            for(int xx=0;xx<pw;xx++){double ridge=8*Math.sin(xx*.032)+6*Math.sin(xx*.079);int yy=top+ph-16-(int)ridge;g.fill(left+xx,yy,left+xx+1,top+ph,0x12396969);}
            for(int yy=top+7;yy<top+ph;yy+=7)g.fill(left+2,yy,left+pw-2,yy+1,0x04735236);
            g.fill(left,top,left+nav,top+ph,0xFF233F45);g.fill(left,top,left+pw,top+2,GOLD);g.fill(left+pw-2,top,left+pw,top+ph,GOLD);
            g.drawString(font,"DuskRain",left+10,top+17,0xF2DCAB,false);g.drawString(font,"烟 雨 仙 途",left+10,top+35,0xBDD8D0,false);
            g.fill(left+10,top+51,left+nav-10,top+52,0xFF6B827C);g.drawString(font,"205255670",left+11,top+ph-22,0xD6C596,false);
            g.drawString(font,font.plainSubstrByWidth(menu.title(),cw-53),cx,top+17,INK,false);
            if(snapshot!=null){var r=snapshot;g.drawString(font,font.plainSubstrByWidth(r.player()+"  ·  "+Rules.realm(r.stage())+"  ·  灵石 "+r.money(),cw),cx,top+34,TEAL,false);}
            g.fill(cx,top+52,cx+cw,top+53,0xFFCEC6AD);
            if(profile)realm(g);else if(skill)school(g);else if(!journal){g.drawString(font,shop?"丹 · 器 · 材      物有所用，道有所成":"山川有路，仙途有归",cx,top+66,TEAL,false);}
            int yy=journal?top+96:profile||skill?top+102:top+88;
            int maxLines=profile||skill||journal?2:3;
            for(var line:font.split(Component.literal(menu.description()),cw-4)){if(maxLines--<=0)break;g.drawString(font,line,cx,yy,MUTED,false);yy+=10;}
            if(mx>=cx&&mx<=cx+cw&&my>=top+88&&my<body-4)g.renderTooltip(font,font.split(Component.literal(menu.description()),Math.min(360,width-40)),mx,my);
            int total=Math.max(1,(menu.entries().size()+size-1)/size);g.drawString(font,(journal?"卷":"页")+" "+(page+1)+" / "+total,cx+cw-65,top+ph-22,MUTED,false);
            if(System.currentTimeMillis()<messageUntil){g.fill(cx-2,top+ph-47,cx+cw,top+ph-33,0xFFE3D8B9);g.drawString(font,font.plainSubstrByWidth(message,cw-5),cx,top+ph-44,INK,false);}
            super.render(g,mx,my,partial);
        }
        void realm(GuiGraphics g){int active=snapshot==null?0:snapshot.stage()/3;int step=cw/6;
            g.fill(cx+step/2,top+76,cx+cw-step/2,top+77,0xFFBFC2AC);
            for(int i=0;i<6;i++){int x=cx+i*step+step/2;g.fill(x-3,top+73,x+4,top+80,i<=active?TEAL:0xFFB6BCA9);g.drawCenteredString(font,Rules.current.realms[i],x,top+86,i==active?0xFFE0AD53:0xFF526D65);}
        }
        void school(GuiGraphics g){int school=snapshot==null?0:snapshot.school();String[] mottos={"择一道而行","剑修 · 剑距 / 剑心 / 剑意","术修 · 灵海 / 灵泉 / 法脉","体修 · 御风步 / 纵云跃 / 金刚骨"};g.drawString(font,mottos[Math.max(0,Math.min(3,school))],cx,top+69,TEAL,false);g.drawString(font,"主动技能： Z / X / C     境界达到后解锁",cx,top+85,MUTED,false);}
        @Override public boolean mouseScrolled(double x,double y,double delta){int max=(Math.max(1,menu.entries().size())-1)/size;int next=Math.max(0,Math.min(max,page+(delta<0?1:-1)));if(next!=page){page=next;rebuildWidgets();return true;}return super.mouseScrolled(x,y,delta);}
        @Override public boolean isPauseScreen(){return false;}
    }
    static final class Tile extends Button {
        final Network.Entry entry;final boolean small;final net.minecraft.world.item.ItemStack icon;
        Tile(int x,int y,int w,int h,Network.Entry e,Runnable action,boolean small){super(x,y,w,h,Component.literal(e.label()),b->action.run(),DEFAULT_NARRATION);entry=e;this.small=small;active=e.enabled();
            String id=e.icon().isEmpty()?defaultIcon(e.action()):e.icon();var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));icon=new net.minecraft.world.item.ItemStack(item==null?net.minecraft.world.item.Items.BOOK:item);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(e.label()+(e.detail().isBlank()?"":"\n"+e.detail()))));}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){Font f=Minecraft.getInstance().font;int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean hover=isHoveredOrFocused();
            if(small){g.fill(x,y,x+w,y+h,hover?0xFF4B706F:0xFF304F53);g.fill(x,y,x+2,y+h,hover?GOLD:0xFF527572);g.drawCenteredString(f,getMessage(),x+w/2,y+(h-8)/2,0xE5E1CD);return;}
            g.fill(x,y,x+w,y+h,active?(hover?0xFFE6DDBC:0xFFF7F3E7):0xFFDCDDD0);g.fill(x,y,x+2,y+h,active?GOLD:0xFFB7BEAD);g.fill(x+2,y+h-1,x+w,y+h,0xFFCDC7B0);
            g.fill(x+7,y+9,x+31,y+33,active?0xFFDFE5D7:0xFFCDD5C7);g.renderItem(icon,x+11,y+13);int tx=x+38;
            g.drawString(f,f.plainSubstrByWidth(entry.label(),w-44),tx,y+8,active?INK:0xFF788477,false);
            String desc=entry.detail().isEmpty()?"展开此卷":entry.detail();int yy=y+21,n=2;for(var line:f.split(Component.literal(desc),w-44)){if(n--==0)break;g.drawString(f,line,tx,yy,active?MUTED:0xFF899185,false);yy+=10;}
        }
        static String defaultIcon(String a){if(a.contains("shop"))return "minecraft:emerald";if(a.contains("quests")||a.contains("daily"))return "minecraft:writable_book";if(a.contains("warp")||a.contains("spawn"))return "minecraft:ender_pearl";if(a.contains("skills")||a.contains("trial"))return "minecraft:diamond_sword";if(a.contains("claim")||a.contains("home"))return "minecraft:cherry_door";if(a.contains("sidebar"))return "minecraft:painting";return "minecraft:enchanted_book";}
    }
}
'''
# Local jumping predicts the server's configured baseline; authoritative correction remains server-side.
s=s.replace('@SubscribeEvent public static void logout(','''@SubscribeEvent public static void jump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent e){if(e.getEntity()==Minecraft.getInstance().player&&snapshot!=null&&snapshot.school()==3){var p=e.getEntity();var v=p.getDeltaMovement();p.setDeltaMovement(v.x,Math.max(v.y,.58),v.z);}}
        @SubscribeEvent public static void logout(''')
s=s.replace('g.pose().popPose();\n    });','''g.pose().popPose();
        if(r.school()>0){int bx=w/2-72,by=h-65;int[] cds={r.cooldown0(),r.cooldown1(),r.cooldown2()};for(int i=0;i<3;i++){g.fill(bx+i*49,by,bx+i*49+45,by+17,0xD019262C);g.drawCenteredString(f,new String[]{"Z","X","C"}[i]+"  "+(r.stage()<i*3?"未解锁":cds[i]>0?cds[i]+"秒":"就绪"),bx+i*49+22,by+5,cds[i]>0?0xA8BEB7:0xE8D19A);}}
    });''')
p.write_text(s,encoding='utf-8')
print('Scroll-style RPG UI generated.')
