import http from 'node:http';

const PORT = Number(process.env.PORT || 8787);
const API_KEY = process.env.OPENAI_API_KEY || '';
const MODEL = process.env.OPENAI_MODEL || 'gpt-5.6-luna';
const GATEWAY_TOKEN = process.env.NOORA_GATEWAY_TOKEN || '';

function send(res, status, body) {
  res.writeHead(status, {'content-type':'application/json; charset=utf-8','cache-control':'no-store'});
  res.end(JSON.stringify(body));
}

function authorized(req) {
  if (!GATEWAY_TOKEN) return true; // local development only; production must set a token
  return req.headers.authorization === `Bearer ${GATEWAY_TOKEN}`;
}

async function askOpenAI(messages) {
  const r = await fetch('https://api.openai.com/v1/responses', {
    method:'POST',
    headers:{'content-type':'application/json','authorization':`Bearer ${API_KEY}`},
    body:JSON.stringify({model:MODEL,instructions:'You are NOORA, a warm, respectful, Muslim-oriented female AI assistant. Speak naturally and answer in the user\'s language, including Urdu-English mixed conversation when appropriate. Never claim to have done an action you did not perform.',input:messages})
  });
  const data = await r.json();
  if (!r.ok) throw new Error(data?.error?.message || `Provider error ${r.status}`);
  const output = Array.isArray(data.output) ? data.output : [];
  const text = output.flatMap(x => Array.isArray(x.content) ? x.content : [])
    .filter(x => x.type === 'output_text').map(x => x.text).join('');
  return {text, provider:'openai', model:MODEL};
}

const server = http.createServer(async (req,res) => {
  if (req.method === 'GET' && req.url === '/health') return send(res,200,{ok:true,service:'noora-gateway'});
  if (req.method !== 'POST' || req.url !== '/v1/chat') return send(res,404,{error:'Not found'});
  if (!authorized(req)) return send(res,401,{error:'Unauthorized'});
  if (!API_KEY) return send(res,503,{error:'AI provider is not configured on the gateway'});
  try {
    let raw='';
    for await (const chunk of req) { raw += chunk; if (raw.length > 200000) throw new Error('Request too large'); }
    const body=JSON.parse(raw || '{}');
    const messages=Array.isArray(body.messages) ? body.messages : [];
    if (!messages.length) return send(res,400,{error:'messages is required'});
    const result=await askOpenAI(messages);
    return send(res,200,result);
  } catch (e) {
    return send(res,500,{error:e?.message || 'Gateway error'});
  }
});

server.listen(PORT,()=>console.log(`NOORA gateway listening on ${PORT}`));
