// Renderiza a saida real de um comando como janela de terminal (tema escuro).
// Uso: node term.js <nome> "<titulo>" <arquivo-com-a-saida>
const fs=require('fs');const {chromium}=require('playwright');
const OUT='/home/user/vestebemuai-poc/docs/sprints/sprint-2/evidencias/';
const [name,title,file]=process.argv.slice(2);
const esc=s=>s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
let txt=esc(fs.readFileSync(file,'utf8').replace(/\s+$/,''));
txt=txt.replace(/^(vestebemuai-poc \$)/gm,'<span class="p">$1</span>')
 .replace(/(BUILD SUCCESS|OK:[^\n]*|HTTP\/1\.1 201[^\n]*|HTTP 201[^\n]*)/g,'<span class="ok">$1</span>')
 .replace(/(HTTP\/1\.1 503[^\n]*|HTTP 503[^\n]*|Mensagem enviada para a DLQ\.)/g,'<span class="er">$1</span>')
 .replace(/(Tentativa \d de 4 falhou)/g,'<span class="wa">$1</span>');
const html=`<!doctype html><meta charset="utf-8"><style>body{margin:0;background:#fff;padding:16px;font-family:"DejaVu Sans Mono",monospace}
.w{background:#1e1e1e;border-radius:10px;overflow:hidden;width:1180px;box-shadow:0 2px 8px #0003}.t{background:#3a3a3a;color:#ddd;font:14px "DejaVu Sans",sans-serif;padding:8px 14px}
.t i{display:inline-block;width:12px;height:12px;border-radius:50%;margin-right:6px;background:#ff5f56}.t i+i{background:#ffbd2e}.t i+i+i{background:#27c93f;margin-right:14px}
pre{margin:0;padding:14px 18px;color:#e6e6e6;font-size:14px;line-height:1.45;white-space:pre-wrap;word-break:break-all}.p{color:#7ec699}.ok{color:#7ee787;font-weight:bold}.er{color:#ff7b72;font-weight:bold}.wa{color:#e3b341}</style>
<div class="w"><div class="t"><i></i><i></i><i></i>${esc(title)}</div><pre>${txt}</pre></div>`;
fs.writeFileSync(OUT+'fontes/'+name+'.html',html);
(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:1220,height:300},deviceScaleFactor:2});
await p.goto('file://'+OUT+'fontes/'+name+'.html');await p.screenshot({fullPage:true,path:OUT+name+'.png'});await b.close();console.log(name)})();
