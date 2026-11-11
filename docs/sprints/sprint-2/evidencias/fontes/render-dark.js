// Renderiza arquivos do repositorio no visual escuro do GitHub (evidencias da Sprint 2).
// Uso: node render-dark.js   (gera os PNG em docs/sprints/sprint-2/evidencias)
const fs=require('fs'),path=require('path');const {marked}=require('marked');const hljs=require('highlight.js');const {chromium}=require('playwright');
const REPO='/home/user/vestebemuai-poc/',OUT=REPO+'docs/sprints/sprint-2/evidencias/',SRC=OUT+'fontes/';
const css=fs.readFileSync(require.resolve('github-markdown-css/github-markdown-dark.css'),'utf8');
const hcss=fs.readFileSync(require.resolve('highlight.js/styles/github-dark.css'),'utf8');
const N='notificacoes-service/src/main/java/br/com/vestebemuai/notificacoes/';
const M='monolito/src/main/java/br/com/vestebemuai/monolito/';
// [nome, arquivo, modo, linguagem, linha inicial, linha final]
const jobs=[
 ['e1-workspace-alvo-dsl','docs/c4/fontes/workspace-alvo.dsl','src','plaintext',1,46],
 ['e2-publicador-pedido-criado',M+'notificacao/PublicadorPedidoCriado.java','src','java',1,0],
 ['e3-pedido-criado-listener',N+'evento/PedidoCriadoListener.java','src','java',1,0],
 ['e4-retry-dlq-config','notificacoes-service/src/main/resources/application.yml','src','yaml',1,48],
 ['e5-idempotencia-service',N+'evento/NotificacaoPedidoService.java','src','java',1,0],
 ['e6-checkout-service',M+'pedido/CheckoutService.java','src','java',1,0],
 ['r1-adr-002','docs/adr/ADR-002-rabbitmq-como-broker.md','md',null],
 ['r1-adr-003','docs/adr/ADR-003-reenvio-e-dlq.md','md',null],
 ['r1-adr-004','docs/adr/ADR-004-evidencia-aws-terraform.md','md',null],
];
const only=process.argv.slice(2);
(async()=>{const b=await chromium.launch();
for(const [name,file,mode,lang,from,to] of jobs){
 if(only.length&&!only.includes(name))continue;
 let src=fs.readFileSync(REPO+file,'utf8');let body;
 if(mode==='md'){body='<article class="markdown-body">'+marked.parse(src)+'</article>';}
 else{let lines=src.split('\n');if(lines.at(-1)==='')lines.pop();const total=lines.length;const end=to||total;
  const hl=hljs.highlight(lines.slice(from-1,end).join('\n'),{language:lang}).value.split('\n');
  body='<div class="meta">'+total+' linhas'+(from>1||end<total?` · trecho das linhas ${from} a ${end}`:'')+'</div><table class="code">'+hl.map((l,i)=>`<tr><td class="n">${i+from}</td><td class="c">${l||' '}</td></tr>`).join('')+'</table>';}
 const html=`<!doctype html><meta charset="utf-8"><style>${css}${hcss}
 body{margin:0;background:#010409;font-family:-apple-system,"Segoe UI","DejaVu Sans",Arial,sans-serif;padding:18px}
 .box{background:#0d1117;border:1px solid #30363d;border-radius:8px;width:1100px;overflow:hidden}
 .hd{padding:10px 16px;border-bottom:1px solid #30363d;background:#161b22;font-size:14px;color:#e6edf3}.hd b{color:#4493f8;font-weight:600}
 .markdown-body{padding:24px 32px;font-size:15px}.meta{padding:6px 16px;font-size:12px;color:#9198a1;border-bottom:1px solid #30363d}
 table.code{border-collapse:collapse;font:13px/1.5 "DejaVu Sans Mono",monospace;width:100%}td.n{color:#6e7681;text-align:right;padding:0 12px;width:36px;user-select:none}td.c{color:#e6edf3;white-space:pre;padding-right:16px}</style>
 <div class="box"><div class="hd">github.com/<b>leonardomdeoliv/vestebemuai-poc</b> / ${file.split('/').join(' / ')}</div>${body}</div>`;
 const f=SRC+name+'.html';fs.writeFileSync(f,html);
 const p=await b.newPage({viewport:{width:1140,height:2000},deviceScaleFactor:2});await p.goto('file://'+f);
 const h=await p.evaluate(()=>document.querySelector('.box').getBoundingClientRect().bottom+18);
 await p.screenshot({path:OUT+name+'.png',clip:{x:0,y:0,width:1140,height:Math.min(h,1900)}});await p.close();console.log(name,Math.round(h));}
await b.close();})();
