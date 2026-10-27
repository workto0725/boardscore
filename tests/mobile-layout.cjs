const fs=require('node:fs');
const assert=require('node:assert/strict');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const html=fs.readFileSync('index.html','utf8').replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi,'');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
 const page=await browser.newPage();
 await page.route('**/*',route=>route.abort());
 for(const width of [320,360,390,432,700,1024]){
  await page.setViewportSize({width,height:900});
  await page.setContent(html);
  await page.evaluate(()=>{
   document.querySelector('#chooser').classList.add('hidden');
   document.querySelector('#session').classList.remove('hidden');
   document.querySelector('#gametitle').textContent='윙스팬 - 7 원더스 듀얼 게임방';
   document.querySelector('#gameRoomName').textContent='';
   document.querySelector('#roomDirectActions').classList.remove('hidden');
   document.querySelector('#gamePhotoCard').scrollIntoView();
  });
  const title=await page.locator('#gametitle').boundingBox();
  const card=await page.locator('#gamePhotoCard').boundingBox();
  if(width<=700)assert(title.width>card.width*.85,`Title compressed at ${width}`);
  await page.evaluate(()=>{
   document.querySelector('#onlinePanel').classList.remove('hidden');
   document.querySelector('#onlineSetup').classList.add('hidden');
   document.querySelector('#onlineInvite').classList.remove('hidden');
   document.querySelector('#currentRoomName').classList.remove('hidden');
   document.querySelector('#currentRoomName').textContent='🏠 방 이름: 7 원더스 듀얼 게임방';
  });
  const boxes=await page.locator('#onlineInvite>.bar>.btn').evaluateAll(els=>els.map(e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,right:r.right}}));
  if(width<=700){
   assert(boxes.every(b=>b.w>=120&&b.x>=0&&b.right<=width),`Narrow or overflowing room buttons at ${width}`);
   assert(boxes[2].y>boxes[0].y,`Room buttons did not wrap at ${width}`);
  }
  console.log(`Mobile layout checks passed: ${width}px`);
  if(width===390 && process.env.SCREENSHOT_DIR){
   await page.locator('#gamePhotoCard').screenshot({path:process.env.SCREENSHOT_DIR+'/mobile-game-layout.jpg'});
   await page.locator('#onlinePanel').screenshot({path:process.env.SCREENSHOT_DIR+'/mobile-room-layout.jpg'});
  }
 }
 await browser.close();
})().catch(e=>{console.error(e);process.exitCode=1});
