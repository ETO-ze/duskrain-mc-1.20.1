from pathlib import Path
R=Path(__file__).resolve().parents[1]/'mod/src/main/java/cn/duskrain'
p=R/'Rules.java';s=p.read_text(encoding='utf-8');a=s.index('        String[] places =');b=s.index('        String[][] ds=',a)
s=s[:a]+'''        String[][] story={
            {"初入烟雨·山门留名","visit","spawn","1"},{"初入烟雨·拜见松玄","talk","master","1"},{"初入烟雨·试剑听声","practice","dummy","3"},{"初入烟雨·竹林初试","trial","1","1"},
            {"筑道问心·丹师委托","talk","pills","1"},{"筑道问心·亲炼回灵","craft","duskrain:pill_0","1"},{"筑道问心·循声破阵","puzzle","1","1"},{"筑道问心·守心归来","trial","1","1"},
            {"金丹凝华·器师访谈","talk","artifacts","1"},{"金丹凝华·玄铁备炉","submit","minecraft:iron_ingot","16"},{"金丹凝华·机枢解疑","puzzle","2","1"},{"金丹凝华·破甲见心","trial","2","1"},
            {"元婴游神·藏经寻迹","visit","library","1"},{"元婴游神·闻溪旧事","investigate","rumor","1"},{"元婴游神·山外除妖","kill","hostile","30"},{"元婴游神·再问机枢","trial","2","1"},
            {"化神证道·登云问天","visit","palace","1"},{"化神证道·灵珠凝阵","submit","minecraft:ender_pearl","12"},{"化神证道·引雷入柱","puzzle","3","1"},{"化神证道·雷蛟现身","trial","3","1"},
            {"渡劫归真·师门问道","talk","master","1"},{"渡劫归真·淬炼道心","practice","dummy","12"},{"渡劫归真·百战余生","kill","hostile","60"},{"渡劫归真·烟雨长明","trial","3","1"}};
        for(int i=0;i<24;i++){int chapter=i/4;String[] q=story[i];r.main.add(new Quest("main_"+i,q[0],q[1],q[2],Integer.parseInt(q[3]),chapter*3,120*(chapter+1)*(i%4+1),100*(chapter+1)));}
'''+s[b:]
s=s.replace('{"商街巡游","visit","market","1"},{"丹坊问药","visit","alchemy","1"},{"仙宫礼道","visit","palace","1"}', '{"商街问候","talk","materials","1"},{"丹坊问药","talk","pills","1"},{"演武调息","practice","dummy","6"}')
s=s.replace('public String serverName','public int contentVersion=2;\n    public String serverName')
s=s.replace('            Rules candidate=JSON.fromJson(Files.readString(f,StandardCharsets.UTF_8),Rules.class);','''            String raw=Files.readString(f,StandardCharsets.UTF_8);
            Rules candidate=JSON.fromJson(raw,Rules.class);
            if(!JsonParser.parseString(raw).getAsJsonObject().has("contentVersion")){
                Path old=f.resolveSibling("rules-before-remake.json");if(!Files.exists(old))Files.copy(f,old);
                Rules fresh=defaults();candidate.main=fresh.main;candidate.daily=fresh.daily;candidate.skills=fresh.skills;candidate.contentVersion=2;
                Files.writeString(f,JSON.toJson(candidate),StandardCharsets.UTF_8);
            }''')
p.write_text(s,encoding='utf-8')
p=R/'DRCommands.java';s=p.read_text(encoding='utf-8')
a=s.index('        String[][] effects=');b=s.index('    static String objective',a)
s=s[:a]+'''        String[][] effects={{"12格直线剑气，遇障碍停止","3.5格回旋剑斩","最多6格突进，沿途检测障碍"},{"16格灵弹，沿途检测命中","目标区域寒霜减速","蓄力1.2秒，目标区域落雷"},{"最多6格冲肩，近身冲击","4格震地，击退与减速","5秒护体，受击时反震"}};
        for(int i=0;i<3;i++)es.add(card(Rules.current.skills[r.school-1][i],"skills cast "+i,effects[r.school-1][i]+" · "+Rules.current.skillCost[i]+"灵力 · "+Rules.current.skillCooldown[i]+"秒冷却 · "+Rules.realm(i*3)+"解锁","duskrain:artifact_"+r.school+"_"+(r.stage/3),r.stage>=i*3));
        String passive=new String[]{"","剑距：持剑普攻4.5格；剑心：剑伤加成；剑意：连续五次命中增强伤害并回灵。","灵海：灵力上限提高25%；灵泉：每秒额外回灵；法脉：施法消耗降低20%。","御风步：移速提高15%；纵云跃：跳跃约2格；金刚骨：生命、韧性与抗击退。"}[r.school];
        Network.menu(p,"流派技能 · "+Rules.current.schools[r.school],passive,es);
    }
'''+s[b:]
s=s.replace('case "visit"->','case "talk"->"拜访 "+Services.ALL.stream().filter(x->x.id().equals(q.target())).map(Services.Service::name).findFirst().orElse(q.target());case "practice"->"命中演武庭练功木人 × "+q.count();case "puzzle"->"破解 "+Trials.NAMES[Integer.parseInt(q.target())-1]+"的机关";case "craft"->"亲手合成 "+Content.label(ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target())));case "investigate"->"向任务殿闻溪打听旧事";case "visit"->')
a=s.index('    static void quests(');b=s.index('    static void daily(',a)
s=s[:a]+'''    static void quests(ServerPlayer p){Profile r=Store.of(p);List<Network.Entry> entries=new ArrayList<>();
        for(int i=0;i<24;i++){var q=Rules.current.main.get(i);boolean current=i==r.main,done=i<r.main;int progress=current?(q.type().equals("submit")?Gameplay.count(p,ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target()))):r.mainProgress):done?q.count():0;
            entries.add(card((done?"已成 · ":current?"当前 · ":"待续 · ")+q.title(),"quests claim "+i,objective(q)+"；进度 "+progress+"/"+q.count()+"；要求 "+Rules.realm(q.stage())+"；奖励 "+q.xp()+"修为 / "+q.money()+"灵石","minecraft:writable_book",current&&progress>=q.count()&&r.stage>=q.stage()));}
        Network.menu(p,"烟雨纪事 · 六章仙途","依章前行：拜师、炼制、调查与解阵。达到目标后点击当前任务领取；悬停查看完整要求。",entries);
    }
'''+s[b:]
s=s.replace('近战剑势 · 速度与连击','剑气破空 · 4.5格剑距与剑意').replace('近战护体 · 生命与韧性','近战护体 · 移动、跳跃与韧性')
s=s.replace('日常事务集中在主岛楼阁','日常事务沿庭院与溪岸布置').replace('烟雨浮城','烟雨山岛').replace('1—4 人 · 三波与首领','1—4 人 · 探索、解阵与首领').replace('三波战斗与一位首领','三道机关、三波战斗与原创首领').replace('青竹伏妖','青竹迷踪').replace('玄岩问心','玄岩机枢').replace('雷霆问劫','雷台问劫')
s=s.replace('Store.of(p).school=0;Network.sync(p);','Store.of(p).school=0;Skills.attributes(p);Skills.clear(p.getUUID());Network.sync(p);')
p.write_text(s,encoding='utf-8')
p=R/'Gameplay.java';s=p.read_text(encoding='utf-8').replace('    public static void progress(','''    @SubscribeEvent public void craft(PlayerEvent.ItemCraftedEvent e){if(e.getEntity() instanceof ServerPlayer p){var id=ForgeRegistries.ITEMS.getKey(e.getCrafting().getItem());if(id!=null)progress(p,"craft",id.toString());}}
    public static void progress(''')
s=s.replace('r.meditating=false;r.arena=false;restore(p);','if(r.cityVersion<2){if(p.level().dimension().equals(CITY))spawn(p);r.mainProgress=0;r.cityVersion=2;}r.meditating=false;r.arena=false;restore(p);')
s=s.replace('Trials.leave(p,true);Skills.clear', 'Trials.leave(p,true);Actions.SEEN.remove(p.getUUID());Network.LAST_ACTION.remove(p.getUUID());Skills.clear')
p.write_text(s,encoding='utf-8')
p=R/'Profile.java';s=p.read_text(encoding='utf-8').replace('public int stage,','public int cityVersion;\n    public int stage,').replace('n.putInt("stage",stage);','n.putInt("cityVersion",cityVersion);n.putInt("stage",stage);').replace('p.stage=Math.max','p.cityVersion=n.getInt("cityVersion");p.stage=Math.max');p.write_text(s,encoding='utf-8')
print('24 original main objectives and migration installed.')
