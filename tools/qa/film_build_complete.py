from film_control import *
started=time.time()
while time.time()-started<120:
    try:
        st=status()
        if st.get('screen')=='game' and 'constructionPhase' in st and time.time()-(RUN/'duskrain-director-status.json').stat().st_mtime<3:break
    except Exception:pass
    time.sleep(1)
else:raise RuntimeError('Film client not ready')
send('close\nwindow 1920 1080\ngui 3\nhidehud\ncmd time set 6000\nprepare_film_build',3)
started=time.time()
while time.time()-started<90:
    st=status()
    if st.get('constructionPaused') and st.get('constructionPhase')==0:break
    time.sleep(1)
else:raise RuntimeError('Foundation reset did not finish')
a=orient(65,208,-96,0,173,-192)
send('cmd dr build camera off\ncmd execute in duskrain:construction run tp @s '+' '.join(map(str,a))+'\nhidehud',5)
send('shot construction-foundation')
begin('27-construction-complete');time.sleep(3)
send('cmd dr build speed 60\ncmd dr build resume')
started=time.time()
while time.time()-started<240:
    if status().get('constructionPhase')==99:break
    time.sleep(2)
else:raise RuntimeError('Build timed out')
time.sleep(8);end('27-construction-complete','仙宫完整逐块建造：基础、梁柱、青瓦、陈设与灯光')
send('shot construction-complete')
