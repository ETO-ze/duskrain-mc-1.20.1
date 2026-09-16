from pathlib import Path
import json
root=Path(__file__).resolve().parents[2]
p=root/'mod/src/main/resources/data/duskrain/guide.json'
pages=json.loads(p.read_text('utf8'))
for i,text in enumerate(pages):
 text=text.replace('18件分境界法器','21件分境界法器').replace('18阶段各有','21阶段各有')
 if text.startswith('廿五 ·'):
  text='廿五 · 商铺上架\n\n在自家柜台选背包实物，填写每件单价与数量。\n27个陈列位，每种最多4096件。\n可补货、调价或取回部分。买家选择数量后确认总价。旧寄售按整组旧价出售。'
 text=text.replace('挑战开始会消耗所需材料；请先准备好。','原六境挑战入场耗材；登神挑战只在成功后结算。')
 pages[i]=text
p.write_text(json.dumps(pages,ensure_ascii=False,indent=2),encoding='utf8')
# Symmetric protocol size checks catch invalid configured views at their source.
p=root/'mod/src/main/java/cn/duskrain/V14Network.java';s=p.read_text('utf8')
s=s.replace('static void writeMarket(MarketStock.View v,FriendlyByteBuf b){','static void writeMarket(MarketStock.View v,FriendlyByteBuf b){if(v.rows().size()>27)throw new IllegalArgumentException("Market view exceeds 27 rows");')
s=s.replace('static void writeEnchant(Enchanting.View v,FriendlyByteBuf b){','static void writeEnchant(Enchanting.View v,FriendlyByteBuf b){if(v.choices().size()>8)throw new IllegalArgumentException("Enchant view exceeds 8 choices");')
p.write_text(s,encoding='utf8')
p=root/'mod/src/main/java/cn/duskrain/GuildFormScreen.java';s=p.read_text('utf8').replace('create?"渡劫后期 · 宗务堂登记 · 免费创建"','verb.equals("rename")?"中文宗名 2–16 字 · 保留成员与洞天":create?"至少渡劫后期 · 宗务堂登记 · 免费创建"');p.write_text(s,encoding='utf8')
print('Updated existing guide pages and bounded protocol views.')
