// Prints do painel do RabbitMQ (visual nativo) e do Mailpit (tema escuro).
const {chromium}=require('playwright');const OUT='/home/user/vestebemuai-poc/docs/sprints/sprint-2/evidencias/';
const acao=process.argv[2];
(async()=>{const b=await chromium.launch();
 if(acao.startsWith('rabbit')){
  const p=await b.newPage({viewport:{width:1400,height:900},deviceScaleFactor:2});
  await p.goto('http://localhost:15672/');await p.fill('input[name=username]','vestebemuai');await p.fill('input[name=password]','vestebemuai');
  await p.click('input[type=submit]');await p.waitForSelector('#logout',{timeout:15000});
  if(acao==='rabbit-mensagem'){
   await p.goto('http://localhost:15672/#/queues/%2F/notificacoes.pedido-criado');await p.waitForTimeout(2500);
   const h=p.locator('h2',{hasText:'Get messages'});await h.click();await p.waitForTimeout(500);
   await p.click('input[value="Get Message(s)"]');await p.waitForTimeout(1500);
   const y=await p.evaluate(()=>{const h=[...document.querySelectorAll('h2')].find(e=>e.textContent.includes('Get messages'));return h.getBoundingClientRect().top+window.scrollY-20;});
   const fim=await p.evaluate(()=>document.body.scrollHeight);
   await p.screenshot({path:OUT+process.argv[3]+'.png',fullPage:true,clip:{x:0,y:y,width:1400,height:Math.min(fim-y,900)}});
  } else if(acao==='rabbit-filas'){
   await p.goto('http://localhost:15672/#/queues');await p.waitForTimeout(6000);await p.screenshot({path:OUT+process.argv[3]+'.png'});
  } else if(acao==='rabbit-exchange'){
   await p.goto('http://localhost:15672/#/exchanges/%2F/pedidos');await p.waitForTimeout(3000);await p.screenshot({path:OUT+process.argv[3]+'.png',fullPage:false});
  }
 } else if(acao==='mailpit'){
  const ctx=await b.newContext({viewport:{width:1400,height:800},deviceScaleFactor:2,colorScheme:'dark'});const p=await ctx.newPage();
  await p.goto(process.argv[4]||'http://localhost:8025/');await p.waitForTimeout(2500);
  if(process.argv[5]){await p.locator('a.message, .message').first().click();await p.waitForTimeout(2500);}
  await p.screenshot({path:OUT+process.argv[3]+'.png'});
 }
 await b.close();console.log('ok',acao);})();
