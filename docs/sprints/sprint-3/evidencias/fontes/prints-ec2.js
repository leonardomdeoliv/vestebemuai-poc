// Prints das telas da PoC rodando na EC2 (S3.5), tirados de dentro da propria instancia
// com o Playwright em conteiner, ligado a rede do Compose (mailpit e rabbitmq pelo nome).
// Uso (na EC2):
//   docker run --rm --network vestebemuai_default -v "$PWD":/w -w /w mcr.microsoft.com/playwright:v1.48.2-noble \
//     bash -c "npm i --no-save playwright@1.48.2 >/dev/null 2>&1 && node docs/sprints/sprint-3/evidencias/fontes/prints-ec2.js <acao> <arquivo.png> [url] [busca para abrir uma mensagem]"
// acao: mailpit (tema escuro) | rabbit-filas (visual nativo)
const {chromium}=require('playwright');
const [acao,arquivo,url,abrir]=process.argv.slice(2);
(async()=>{const b=await chromium.launch();
 if(acao==='rabbit-filas'){
  const p=await b.newPage({viewport:{width:1400,height:650},deviceScaleFactor:2});
  await p.goto('http://rabbitmq:15672/');await p.fill('input[name=username]','vestebemuai');await p.fill('input[name=password]','vestebemuai');
  await p.click('input[type=submit]');await p.waitForSelector('#logout',{timeout:15000});
  await p.goto('http://rabbitmq:15672/#/queues');await p.waitForTimeout(6000);await p.screenshot({path:arquivo});
 } else {
  const ctx=await b.newContext({viewport:{width:1400,height:800},deviceScaleFactor:2,colorScheme:'dark'});const p=await ctx.newPage();
  await p.goto(url||'http://mailpit:8025/');await p.waitForTimeout(3000);
  if(abrir){ // abrir = busca do Mailpit; abre a mensagem mais recente que casar com ela
   const base=new URL(url||'http://mailpit:8025/').origin;
   const r=await (await p.request.get(base+'/api/v1/search?limit=1&query='+encodeURIComponent(abrir))).json();
   await p.goto(base+'/view/'+r.messages[0].ID);await p.waitForTimeout(3000);}
  await p.screenshot({path:arquivo});
 }
 await b.close();console.log('ok',acao,arquivo);})();
