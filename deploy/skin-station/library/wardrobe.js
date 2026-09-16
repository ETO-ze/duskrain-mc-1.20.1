const button=document.getElementById('dr-use-same-name');
if(button)button.addEventListener('click',()=>{const input=document.getElementById('fallback-player');if(input){input.value=button.dataset.playerName;input.focus();}});
