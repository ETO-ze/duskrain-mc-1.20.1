const $ = (id) => document.getElementById(id);
const API = '/library/api/';
let page = 1, catalogSerial = 0, selectionSerial = 0, lookupSerial = 0, selected = null, viewer = null, ownedPlayers = [];
let catalogAbort = null, accountLoaded = false;
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
  const query = new URLSearchParams({page:String(page), keyword:$('keyword').value.trim(), filter:$('model').value, sort:$('sort').value});
  try {
    const payload = await json(API + 'catalog?' + query, controller.signal);
    if (serial !== catalogSerial) return;
    const rows = Array.isArray(payload.data) ? payload.data.filter(r => r.public === true && ['steve','alex'].includes(r.type) && Number.isSafeInteger(r.tid) && r.tid > 0).slice(0,20) : [];
    const grid = $('catalog'); grid.replaceChildren();
    for (const row of rows) {
      const card = document.createElement('button'); card.className = 'skin-card'; card.type='button'; card.setAttribute('aria-label', '预览 ' + row.name);
      const art = document.createElement('div'); art.className = 'skin-art';
      const image = document.createElement('img'); image.loading='lazy'; image.decoding='async'; image.src=API+'preview/'+row.tid; image.alt=row.name;
      image.onerror=()=>{image.hidden=true;art.append(text('span','点击查看 3D 预览','quiet'));};
      art.append(image); const info=document.createElement('div'); info.className='skin-info';
      info.append(text('h3',row.name),text('p','上传者 · '+row.nickname));
      const meta=document.createElement('div');meta.className='skin-meta';meta.append(text('span',row.type==='alex'?'纤细 / 细臂':'经典 / 宽臂'),text('span',(row.hd?'HD · ':'')+'♡ '+row.likes));
      info.append(meta);card.append(art,info);card.onclick=()=>openLittleSkin(row);grid.append(card);
    }
    message('catalog-status',rows.length ? '' : '这页暂时没有找到皮肤，可以换个关键词或选择“全部”。');
    $('page-label').textContent='第 '+page+' 页';$('prev-page').disabled=page<=1;$('next-page').disabled=!payload.next_page_url;
  } catch(error) {if(serial===catalogSerial) {message('catalog-status',error.name==='AbortError'?'查询超时，请重新搜索。':error.message,true);$('catalog').replaceChildren();$('prev-page').disabled=page<=1;}}
  finally {clearTimeout(timeout);if(serial===catalogSerial)$('catalog').setAttribute('aria-busy','false');}
}
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
function updateApply(){$('apply-skin').disabled=!selected||selected.blocked||!ownedPlayers.length;}
async function openPreview(item) {
  const serial=++selectionSerial;selected=item;message('preview-status','');
  $('preview-title').textContent=item.name;$('preview-source').textContent=item.source==='mojang'?'MOJANG / 正版皮肤':'LITTLESKIN / 社区作品';
  $('preview-author').textContent=item.author; $('preview-model').textContent=item.model==='slim'?'纤细 · 细臂':'经典 · 宽臂';$('preview-resolution').textContent=item.hd?'HD 皮肤':'';
  $('source-link').href=item.sourceUrl;$('follow-label').hidden=item.source!=='mojang';$('follow-official').checked=false;
  $('preview-fallback').hidden=true;$('skin-canvas').hidden=false;
  $('apply-note').textContent='应用后替换该角色当前皮肤，保留名字、UUID、披风与游戏进度。';
  if(!$('preview-dialog').open)$('preview-dialog').showModal();
  updateApply();loadAccount();
  if(viewer){viewer.dispose();viewer=null;}
  if(item.blocked){message('preview-status','该材质受保护，请通过原站了解使用方式。',true);$('preview-fallback').src=API+'preview/'+item.id;$('preview-fallback').hidden=false;$('skin-canvas').hidden=true;return;}
  try {
    const {skinview3d}=await import('/web/public/bundle.js');if(serial!==selectionSerial)return;
    const container=$('skin-canvas').parentElement;const width=Math.max(240,container.clientWidth);const height=innerWidth<=720?300:500;
    viewer=new skinview3d.SkinViewer({canvas:$('skin-canvas'),width,height});
    await viewer.loadSkin(item.texture,{model:item.model==='slim'?'slim':'default'});
    if(serial!==selectionSerial)return;
    viewer.autoRotate=true;viewer.autoRotateSpeed=.45;viewer.controls.enablePan=false;
    if(skinview3d.IdleAnimation)viewer.animation=new skinview3d.IdleAnimation();
    viewer.render();
  }catch(error){if(serial!==selectionSerial)return;$('skin-canvas').hidden=true;$('preview-fallback').hidden=false;$('preview-fallback').src=item.texture;message('preview-status','3D 预览暂不可用，已切换为平面贴图；仍可应用。');}
}
async function openLittleSkin(row) {
  const serial=++lookupSerial;
  message('catalog-status','正在打开 '+row.name+'……');
  try {
    const info=await json(API+'info/'+row.tid);
    if(serial!==lookupSerial)return;
    if(info.public!==true||!['steve','alex'].includes(info.type))throw new Error('此材质未开放为公开皮肤。');
    message('catalog-status','');
    openPreview({id:row.tid,source:'littleskin',name:info.name,author:'上传者 · '+row.nickname,model:info.type==='alex'?'slim':'classic',hd:!!info.hd,blocked:!!info.protected,texture:API+'raw/'+row.tid,sourceUrl:'https://littleskin.cn/skinlib/show/'+row.tid});
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
    const textures=JSON.parse(atob(prop.value));const skin=textures.textures?.SKIN;
    if(typeof skin?.url!=='string')throw new Error('这个正版角色暂时没有公开皮肤。');
    const url=new URL(skin.url);const match=url.pathname.match(/^\/texture\/([0-9a-f]{32,64})$/i);
    if(url.hostname!=='textures.minecraft.net'||!['http:','https:'].includes(url.protocol)||!match)throw new Error('正版档案没有可识别的官方皮肤地址。');
    message('official-status','已识别 '+profile.name+' · '+profile.id);
    await openPreview({id:profile.id,source:'mojang',name:profile.name+' 的正版皮肤',author:'Mojang 正版档案 · '+profile.name,model:skin.metadata?.model==='slim'?'slim':'classic',texture:API+'mojang/texture/'+match[1],sourceUrl:'https://www.minecraft.net/msaprofile/mygames/editskin'});
  }catch(error){message('official-status',error.message||'查询暂时不可用，请稍后重试。',true);}
  finally{button.disabled=false;}
};
$('follow-official').onchange=()=>{$('apply-note').textContent=$('follow-official').checked?'清除当前上传皮肤，持续跟随该正版角色的外观，约 10 分钟缓存。跟随要求没有本地披风；已有披风时请选择复制，或先在衣柜中处理。':'复制当前外观，之后的正版皮肤变化不会覆盖这份皮肤。名字、UUID、披风与进度保留。';};
$('apply-skin').onclick=async()=>{
  const item=selected;const uuid=$('player-select').value;
  if(!item||item.blocked||!ownedPlayers.some(p=>p.uuid===uuid))return;
  $('apply-skin').disabled=true;message('preview-status','正在保存到你的衣柜……');
  try {
    const body=new FormData();body.set('uuid',uuid);body.set('skinModel',item.model);body.set('returnUrl',location.origin+'/web/player/'+uuid);
    if(item.source==='mojang'&&$('follow-official').checked){
      const playerPage=await request('/web/player/'+uuid);
      const playerDoc=new DOMParser().parseFromString(await playerPage.text(),'text/html');
      const state=playerDoc.getElementById('dr-player-state');
      if(!state)throw new Error('暂时无法确认角色状态，请重新登录后重试。');
      if(state.dataset.localCape!=='false')throw new Error('此角色已有本地披风。请取消“跟随”以复制皮肤，或先到衣柜处理披风；本次未更改外观。');
      body.set('fallbackPlayer',item.id);body.set('deleteSkin','on');
    }
    else {
      // Recheck public availability at application time; the origin enforces download restrictions too.
      if(item.source==='littleskin'){const fresh=await json(API+'info/'+item.id+'?refresh='+Date.now());if(fresh.public!==true||fresh.protected)throw new Error('此皮肤现在未开放下载，请前往原站查看。');}
      const r=await request(item.texture);const blob=await r.blob();
      if(blob.size>4*1024*1024)throw new Error('皮肤超过 4 MiB，无法应用。');
      const bytes=new Uint8Array(await blob.slice(0,24).arrayBuffer());
      if(bytes.length<24||![137,80,78,71,13,10,26,10].every((b,i)=>bytes[i]===b))throw new Error('来源返回的不是有效 PNG 皮肤。');
      const view=new DataView(bytes.buffer);const w=view.getUint32(16),h=view.getUint32(20);
      if(w<64||w>512||(w&(w-1))!==0||![w,w/2].includes(h))throw new Error('皮肤尺寸不符合本站要求。');
      body.set('skinFile',blob,'skin.png');
    }
    const response=await fetch('/web/update-player',{method:'POST',body,credentials:'same-origin',signal:AbortSignal.timeout(25000)});
    const html=await response.text();const doc=new DOMParser().parseFromString(html,'text/html');
    const error=doc.querySelector('.error-message');if(error)throw new Error(error.textContent.trim());
    if(!response.ok||!doc.querySelector('.success-message'))throw new Error('保存未确认，请检查登录状态后重试。');
    const verified=new URL(response.url);if(verified.origin!==location.origin||verified.pathname!=='/web/player/'+uuid)throw new Error('登录可能已失效，请重新登录后应用。');
    location.assign('/web/player/'+uuid);
  }catch(error){message('preview-status',error.message,true);updateApply();}
};
$('close-preview').onclick=()=>$('preview-dialog').close();
$('preview-dialog').addEventListener('close',()=>{++selectionSerial;if(viewer){viewer.dispose();viewer=null;}selected=null;});
$('preview-dialog').addEventListener('click',(e)=>{if(e.target===$('preview-dialog')){const rect=$('preview-dialog').getBoundingClientRect();if(e.clientX<rect.left||e.clientX>rect.right||e.clientY<rect.top||e.clientY>rect.bottom)$('preview-dialog').close();}});
loadCatalog();
