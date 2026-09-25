const $ = (id) => document.getElementById(id);
const API = '/library/api/';
let page = 1, catalogSerial = 0, selectionSerial = 0, lookupSerial = 0, selected = null, viewer = null, ownedPlayers = [];
let catalogAbort = null, accountLoaded = false, category = 'skin', applying = false;
function message(id, text, error = false) { $(id).textContent = text; $(id).classList.toggle('error', error); }
async function request(path, signal) {
  const response = await fetch(path, {credentials: path.startsWith(API) ? 'omit' : 'same-origin', signal: signal || AbortSignal.timeout(18000)});
  if (!response.ok) {
    if ([400,404,204].includes(response.status)) throw new Error('没有找到对应内容，请检查名称或编号。');
    if ([401,403].includes(response.status)) throw new Error('原站未开放此内容，请前往原站查看。');
    if (response.status === 429) throw new Error('请求较频繁，请稍后再试。');
    throw new Error('皮肤来源暂时无法连接，请稍后重试。');
  }
  return response;
}
async function json(path, signal) {
  const r = await request(path, signal);
  if (r.status === 204) throw new Error('没有找到这个正版角色。');
  return r.json();
}
function text(tag, value, cls) { const e = document.createElement(tag); e.textContent = value; if (cls) e.className = cls; return e; }
function setTab(official) {
  $('library-panel').hidden = official; $('official-panel').hidden = !official;
  $('library-tab').setAttribute('aria-selected', String(!official)); $('official-tab').setAttribute('aria-selected', String(official));
  if (official) $('official-name').focus();
}
$('library-tab').onclick = () => setTab(false);
$('official-tab').onclick = () => setTab(true);
for (const btn of document.querySelectorAll('[role=tab]')) btn.onkeydown = (event) => {
  if (['ArrowLeft','ArrowRight'].includes(event.key)) {event.preventDefault(); const official = btn.id === 'library-tab'; setTab(official); $(official ? 'official-tab' : 'library-tab').focus();}
};
async function loadCatalog() {
  const serial = ++catalogSerial;
  if (catalogAbort) catalogAbort.abort(); const controller = new AbortController(); catalogAbort = controller;
  const timeout = setTimeout(() => controller.abort(), 18000);
  $('prev-page').disabled = true; $('next-page').disabled = true;
  message('catalog-status', '正在翻阅衣藏……'); $('catalog').setAttribute('aria-busy','true');
  const query = new URLSearchParams({page:String(page), keyword:$('keyword').value.trim(), filter:category==='cape'?'cape':$('model').value, sort:$('sort').value});
  try {
    const payload = await json(API + 'catalog?' + query, controller.signal);
    if (serial !== catalogSerial) return;
    const rows = Array.isArray(payload.data) ? payload.data.filter(r => r.public === true && (category==='cape'?r.type==='cape':['steve','alex'].includes(r.type)) && Number.isSafeInteger(r.tid) && r.tid > 0).slice(0,20) : [];
    const grid = $('catalog'); grid.replaceChildren();
    for (const row of rows) {
      const card = document.createElement('button'); card.className = 'skin-card'; card.type='button'; card.setAttribute('aria-label', '预览 ' + row.name);
      const art = document.createElement('div'); art.className = 'skin-art';
      const image = document.createElement('img'); image.loading='lazy'; image.decoding='async'; image.src=API+'preview/'+row.tid; image.alt=row.name;
      image.onerror=()=>{image.hidden=true;art.append(text('span','点击查看 3D 预览','quiet'));};
      art.append(image); const info=document.createElement('div'); info.className='skin-info';
      info.append(text('h3',row.name),text('p','上传者 · '+row.nickname));
      const meta=document.createElement('div');meta.className='skin-meta';meta.append(text('span',row.type==='cape'?'披风':row.type==='alex'?'纤细 / 细臂':'经典 / 宽臂'),text('span',(row.hd?'HD · ':'')+'♡ '+row.likes));
      info.append(meta);card.append(art,info);card.onclick=()=>openLittleSkin(row);grid.append(card);
    }
    message('catalog-status',rows.length ? '' : '这页暂时没有找到'+(category==='cape'?'披风':'皮肤')+'，可以换个关键词或选择“全部”。');
    $('page-label').textContent='第 '+page+' 页';$('prev-page').disabled=page<=1;$('next-page').disabled=!payload.next_page_url;
  } catch(error) {if(serial===catalogSerial) {message('catalog-status',error.name==='AbortError'?'查询超时，请重新搜索。':error.message,true);$('catalog').replaceChildren();$('prev-page').disabled=page<=1;}}
  finally {clearTimeout(timeout);if(serial===catalogSerial)$('catalog').setAttribute('aria-busy','false');}
}
function setCategory(value) {
  category=value;page=1;$('model').disabled=value==='cape';
  $('skins-category').setAttribute('aria-pressed',String(value==='skin'));
  $('capes-category').setAttribute('aria-pressed',String(value==='cape'));
  $('category-hint').textContent=value==='cape'?'挑选披风，保留现有皮肤。':'选择衣装，试穿新风格。';
  $('keyword').value=value==='cape'?'':'古风';
  for(const b of document.querySelectorAll('[data-keyword]'))b.classList.toggle('active',b.dataset.keyword===$('keyword').value);
  loadCatalog();
}
$('skins-category').onclick=()=>setCategory('skin');
$('capes-category').onclick=()=>setCategory('cape');
$('search-form').onsubmit=(e)=>{e.preventDefault();page=1;loadCatalog();};
for(const e of [$('model'),$('sort')])e.onchange=()=>{page=1;loadCatalog();};
for(const button of document.querySelectorAll('[data-keyword]'))button.onclick=()=>{
  $('keyword').value=button.dataset.keyword;page=1;for(const b of document.querySelectorAll('[data-keyword]'))b.classList.toggle('active',b===button);loadCatalog();
};
$('prev-page').onclick=()=>{if(page>1){page--;loadCatalog();$('library-tab').scrollIntoView({block:'start',behavior:'smooth'});}};
$('next-page').onclick=()=>{page++;loadCatalog();$('library-tab').scrollIntoView({block:'start',behavior:'smooth'});};
async function loadAccount() {
  if(accountLoaded)return;
  try {
    const response=await request('/web/user');const html=await response.text();const doc=new DOMParser().parseFromString(html,'text/html');
    const unique=new Map();
    for(const a of doc.querySelectorAll('a[href]')) {
      const url=new URL(a.getAttribute('href'),location.origin);
      const match=url.pathname.match(/^\/web\/player\/([0-9a-f-]{36})$/i);
      if(url.origin===location.origin&&match)unique.set(match[1],a.textContent.trim());
    }
    ownedPlayers=[...unique].map(([uuid,name])=>({uuid,name}));
    $('player-select').replaceChildren();
    for(const p of ownedPlayers){const o=document.createElement('option');o.value=p.uuid;o.textContent=p.name;$('player-select').append(o);}
    const desired=new URLSearchParams(location.search).get('player');if(ownedPlayers.some(p=>p.uuid===desired))$('player-select').value=desired;
    $('player-select').disabled=!ownedPlayers.length;
    if(ownedPlayers.length){message('account-state','已连接你的衣柜');accountLoaded=true;}
    else {const box=$('account-state');box.replaceChildren(text('span','登录后即可应用皮肤。 '));const a=text('a','登录衣冠阁 →');a.href='/';box.append(a);}
  }catch(error){message('account-state','暂时无法读取衣柜，请先登录后重试。',true);}
  updateApply();
}
function selectionScope() {
  return selected?.source==='mojang'?$('apply-scope').value:selected?.kind;
}
function updateApply() {
  const follow=$('follow-official').checked;
  const scope=selectionScope();
  $('apply-skin').disabled=applying||!selected||selected.blocked||!ownedPlayers.length;
  $('apply-scope').disabled=applying||follow;
  $('follow-official').disabled=applying;
  $('player-select').disabled=applying||!ownedPlayers.length;
  $('apply-skin').textContent=follow?'跟随整套外观':scope==='cape'?'应用此披风':scope==='both'?'应用皮肤与披风':'应用此皮肤';
  $('apply-note').textContent=follow
    ?'将清除本角色当前上传的皮肤和披风，改为跟随该正版角色整套外观（约 10 分钟缓存）。名字、UUID 与进度保留。'
    :scope==='cape'?'只替换披风，保留现有皮肤与模型。若当前皮肤来自正版跟随，将保存当前外观以便混搭。'
    :scope==='both'?'替换本角色的皮肤与披风两项，保留名字、UUID 与游戏进度。'
    :'只替换皮肤，保留现有披风。若披风来自正版跟随，将保存当前披风以便混搭。';
}
async function renderPreview(item,serial) {
  if(viewer){viewer.dispose();viewer=null;}
  try {
    const {skinview3d}=await import('/web/public/bundle.js');if(serial!==selectionSerial)return;
    const container=$('skin-canvas').parentElement;
    const instance=new skinview3d.SkinViewer({canvas:$('skin-canvas'),width:Math.max(240,container.clientWidth),height:innerWidth<=720?300:500});
    viewer=instance;
    if(item.textures.skin)await instance.loadSkin(item.textures.skin,{model:item.model==='slim'?'slim':'default'});
    else instance.playerObject.skin.visible=false;
    if(serial!==selectionSerial)return;
    if(item.textures.cape)await instance.loadCape(item.textures.cape);
    if(serial!==selectionSerial)return;
    instance.autoRotate=true;instance.autoRotateSpeed=.35;instance.controls.enablePan=false;
    if(item.textures.cape)instance.playerObject.rotation.y=Math.PI;
    if(skinview3d.IdleAnimation)instance.animation=new skinview3d.IdleAnimation();
    instance.render();
  }catch(error){
    if(serial!==selectionSerial)return;
    $('skin-canvas').hidden=true;$('preview-fallback').hidden=false;
    $('preview-fallback').src=item.textures.cape||item.textures.skin;
    message('preview-status','3D 预览暂不可用，已切换为平面贴图；仍可应用。');
  }
}
async function openPreview(item) {
  const serial=++selectionSerial;selected=item;message('preview-status','');
  $('preview-title').textContent=item.name;
  $('preview-source').textContent=item.source==='mojang'?'MOJANG / 正版外观':'LITTLESKIN / 社区作品';
  $('preview-author').textContent=item.author;
  $('preview-model').textContent=item.kind==='cape'?'披风':item.model==='slim'?'纤细 · 细臂':'经典 · 宽臂';
  $('preview-resolution').textContent=item.hd?'HD 高清 · 需客户端高清组件':'';
  $('source-link').href=item.sourceUrl;
  $('follow-label').hidden=$('scope-field').hidden=item.source!=='mojang';
  $('follow-official').checked=false;
  for(const o of $('apply-scope').options)o.disabled=o.value==='skin'?!item.textures.skin:o.value==='cape'?!item.textures.cape:!(item.textures.skin&&item.textures.cape);
  $('apply-scope').value=item.textures.skin?'skin':'cape';
  $('cape-state').textContent=item.source==='mojang'?(item.textures.cape?'已识别当前正版披风，可单独应用。':'此正版角色当前未公开穿戴披风。'):item.kind==='cape'?'披风 3D 预览 · 应用时保留现有皮肤。':'';
  $('preview-fallback').hidden=true;$('skin-canvas').hidden=false;
  if(!$('preview-dialog').open)$('preview-dialog').showModal();
  updateApply();loadAccount();
  if(item.blocked){
    if(viewer){viewer.dispose();viewer=null;}
    message('preview-status','该材质受保护，请通过原站了解使用方式。',true);
    $('preview-fallback').src=API+'preview/'+item.id;$('preview-fallback').hidden=false;$('skin-canvas').hidden=true;return;
  }
  await renderPreview(item,serial);
}
async function openLittleSkin(row) {
  const serial=++lookupSerial;
  message('catalog-status','正在打开 '+row.name+'……');
  try {
    const info=await json(API+'info/'+row.tid);
    if(serial!==lookupSerial)return;
    if(info.public!==true||!['steve','alex','cape'].includes(info.type))throw new Error('此材质未开放为公开皮肤或披风。');
    message('catalog-status','');
    const kind=info.type==='cape'?'cape':'skin';
    openPreview({kind,id:row.tid,source:'littleskin',name:info.name,author:'上传者 · '+row.nickname,model:info.type==='alex'?'slim':'classic',hd:!!info.hd,blocked:!!info.protected,textures:{[kind]:API+'raw/'+row.tid},sourceUrl:'https://littleskin.cn/skinlib/show/'+row.tid});
  }catch(error){if(serial===lookupSerial)message('catalog-status',error.message,true);}
}
$('official-form').onsubmit=async(e)=>{
  e.preventDefault();const value=$('official-name').value.trim();const uuid=value.replaceAll('-','');
  const isUUID=/^[0-9a-f]{32}$/i.test(uuid);
  if(!isUUID&&!/^[A-Za-z0-9_]{1,16}$/.test(value)){message('official-status','请输入 Java 正版玩家名，或 32 / 36 位 UUID。',true);return;}
  message('official-status','正在查询 Mojang 正版档案……');const button=e.currentTarget.querySelector('button');button.disabled=true;
  try {
    const identity=isUUID?{id:uuid}:await json(API+'mojang/name/'+encodeURIComponent(value));
    if(!/^[0-9a-f]{32}$/i.test(identity.id))throw new Error('正版档案返回了无效的 UUID。');
    const profile=await json(API+'mojang/profile/'+identity.id);
    const prop=profile.properties?.find(p=>p.name==='textures');if(!prop)throw new Error('这个正版角色暂时没有公开皮肤。');
    const textures=JSON.parse(atob(prop.value)).textures||{};
    const skin=textures.SKIN,cape=textures.CAPE;
    if(!skin&&!cape)throw new Error('这个正版角色暂时没有公开皮肤或披风。');
    const resolved={};if(skin)resolved.skin=texturePath(skin.url);if(cape)resolved.cape=texturePath(cape.url);
    message('official-status','已识别 '+profile.name+' · '+profile.id+(cape?' · 含当前披风':' · 当前没有披风'));
    await openPreview({kind:skin?'skin':'cape',id:profile.id,source:'mojang',name:profile.name+' 的正版外观',author:'Mojang 正版档案 · '+profile.name,model:skin?.metadata?.model==='slim'?'slim':'classic',textures:resolved,sourceUrl:'https://www.minecraft.net/msaprofile/mygames/editskin'});
  }catch(error){message('official-status',error.message||'查询暂时不可用，请稍后重试。',true);}
  finally{button.disabled=false;}
};
// Only known texture paths can be read; account cookies never go to upstream providers.
function texturePath(value) {
  if(typeof value!=='string')throw new Error('档案中缺少有效材质地址。');
  const url=new URL(value,location.origin);
  const official=url.pathname.match(/^\/texture\/([a-f0-9]{32,64})$/i);
  if(url.hostname==='textures.minecraft.net'&&['http:','https:'].includes(url.protocol)&&official)return API+'mojang/texture/'+official[1];
  if(url.origin===location.origin&&/^\/textures\/texture\/(skin|cape|default-skin|default-cape)\/[A-Za-z0-9._%-]+$/.test(url.pathname))return url.pathname;
  throw new Error('材质地址不在允许的来源中。');
}
async function textureBlob(path,kind) {
  const r=await request(path);const blob=await r.blob();
  if(blob.size>4*1024*1024)throw new Error('材质超过 4 MiB，无法应用。');
  const bytes=new Uint8Array(await blob.slice(0,24).arrayBuffer());
  if(bytes.length<24||![137,80,78,71,13,10,26,10].every((b,i)=>bytes[i]===b))throw new Error('来源返回的不是有效 PNG 材质。');
  const view=new DataView(bytes.buffer);const w=view.getUint32(16),h=view.getUint32(20);
  if(w<64||w>512||w%64!==0||(kind==='cape'?h!==w/2:![w,w/2].includes(h)))throw new Error(kind==='cape'?'披风须为 64×32 的整数倍，宽度不超过 512。':'皮肤尺寸不符合本站要求。');
  return blob;
}
async function playerState(uuid) {
  const page=await request('/web/player/'+uuid);
  const doc=new DOMParser().parseFromString(await page.text(),'text/html');
  const state=doc.getElementById('dr-player-state');
  if(!state||!['true','false'].includes(state.dataset.localSkin)||!['true','false'].includes(state.dataset.localCape))throw new Error('暂时无法确认角色状态，请重新登录后重试。');
  return {skin:state.dataset.localSkin==='true',cape:state.dataset.localCape==='true'};
}
async function preserveOtherTexture(body,uuid,scope) {
  if(scope==='both')return;
  const other=scope==='cape'?'skin':'cape';const state=await playerState(uuid);
  if(state[other])return; // Already stored locally; omit it entirely from the update.
  const profile=await json('/session/session/minecraft/profile/'+uuid.replaceAll('-',''));
  if(profile.id!==uuid.replaceAll('-',''))throw new Error('角色资料不一致，已取消应用。');
  const prop=profile.properties?.find(p=>p.name==='textures');
  const texture=prop?JSON.parse(atob(prop.value)).textures?.[other.toUpperCase()]:null;
  if(!texture)return;
  // Drasl cannot mix fallback and uploaded textures. Snapshot the visible other item before mixing.
  body.set(other+'File',await textureBlob(texturePath(texture.url),other),other+'.png');
  if(other==='skin')body.set('skinModel',texture.metadata?.model==='slim'?'slim':'classic');
}
$('apply-scope').onchange=updateApply;
$('follow-official').onchange=updateApply;
$('apply-skin').onclick=async()=>{
  const item=selected;const uuid=$('player-select').value;
  if(applying||!item||item.blocked||!ownedPlayers.some(p=>p.uuid===uuid))return;
  const follow=item.source==='mojang'&&$('follow-official').checked;
  const scope=selectionScope();
  applying=true;updateApply();message('preview-status','正在保存到你的衣柜……');
  try {
    const body=new FormData();body.set('uuid',uuid);body.set('returnUrl',location.origin+'/web/player/'+uuid);
    if(follow){
      // The checkbox and button explicitly select replacement of BOTH local textures.
      body.set('fallbackPlayer',item.id);body.set('deleteSkin','on');body.set('deleteCape','on');
      body.set('skinModel',item.model);
    }else {
      if(item.source==='littleskin'){
        const fresh=await json(API+'info/'+item.id+'?refresh='+Date.now());
        if(fresh.public!==true||fresh.protected||(scope==='cape'?fresh.type!=='cape':!['steve','alex'].includes(fresh.type)))throw new Error('此材质现在未开放下载或分类已变更，请前往原站查看。');
      }
      const kinds=scope==='both'?['skin','cape']:[scope];
      for(const kind of kinds){
        if(!item.textures[kind])throw new Error('当前正版档案没有可应用的'+(kind==='cape'?'披风':'皮肤')+'。');
        body.set(kind+'File',await textureBlob(item.textures[kind],kind),kind+'.png');
      }
      if(kinds.includes('skin'))body.set('skinModel',item.model);
      await preserveOtherTexture(body,uuid,scope);
      const total=[...body.values()].filter(v=>v instanceof Blob).reduce((sum,v)=>sum+v.size,0);
      if(total>4*1024*1024-16384)throw new Error('组合后的材质超过本站上传上限，请选择更小的材质。');
    }
    const response=await fetch('/web/update-player',{method:'POST',body,credentials:'same-origin',signal:AbortSignal.timeout(25000)});
    const html=await response.text();const doc=new DOMParser().parseFromString(html,'text/html');
    const error=doc.querySelector('.error-message');if(error)throw new Error(error.textContent.trim());
    if(!response.ok||!doc.querySelector('.success-message'))throw new Error('保存未确认，请检查登录状态后重试。');
    const verified=new URL(response.url);if(verified.origin!==location.origin||verified.pathname!=='/web/player/'+uuid)throw new Error('登录可能已失效，请重新登录后应用。');
    location.assign('/web/player/'+uuid);
  }catch(error){message('preview-status',error.message,true);}
  finally{applying=false;updateApply();}
};
$('close-preview').onclick=()=>$('preview-dialog').close();
$('preview-dialog').addEventListener('close',()=>{++selectionSerial;if(viewer){viewer.dispose();viewer=null;}selected=null;});
$('preview-dialog').addEventListener('click',(e)=>{if(e.target===$('preview-dialog')){const rect=$('preview-dialog').getBoundingClientRect();if(e.clientX<rect.left||e.clientX>rect.right||e.clientY<rect.top||e.clientY>rect.bottom)$('preview-dialog').close();}});
loadCatalog();
