package io.github.workto0725.boardscoreplus;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.content.Intent;
import android.content.ClipData;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.widget.Toast;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.ContentValues;
import android.os.Environment;
import android.util.Base64;
import java.io.OutputStream;
import android.view.View;
import android.widget.LinearLayout;
import android.content.pm.ApplicationInfo;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
public class MainActivity extends Activity {
 private static final String HOME="https://workto0725.github.io/boardscore/";
 private WebView web;
 private AdView banner;
 private LinearLayout root;
 private boolean previewAds;
 private boolean bannerAllowed=false;
 private boolean bannerRequested=false;
 private boolean homeVisible=false;
 private ValueCallback<Uri[]> picker;
 private Uri cameraPhotoUri;
 private OnBackInvokedCallback systemBackCallback;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  if(Build.VERSION.SDK_INT>=33){
   systemBackCallback=()->handleAppBack();
   getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,systemBackCallback);
  }
  WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.rgb(18,49,40));
  ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
   Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
   Insets keyboard=insets.getInsets(WindowInsetsCompat.Type.ime());
   view.setPadding(safe.left,safe.top,safe.right,Math.max(safe.bottom,keyboard.bottom));
   return WindowInsetsCompat.CONSUMED;
  });
  setContentView(root);
  ViewCompat.requestApplyInsets(root);
  web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
  previewAds=(getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)!=0;
  if(previewAds){
   banner=new AdView(this);banner.setAdUnitId("ca-app-pub-3940256099942544/6300978111");banner.setAdSize(AdSize.BANNER);
   root.addView(banner,new LinearLayout.LayoutParams(-1,-2));
   banner.setVisibility(View.GONE);
   requestAdConsent();
  }
  web.setBackgroundColor(Color.rgb(18,49,40));
  WebSettings st=web.getSettings();
  st.setJavaScriptEnabled(true);st.setDomStorageEnabled(true);st.setAllowFileAccess(false);
  st.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap favicon){
    homeVisible=false;
    refreshBannerVisibility();
   }
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){
    Uri u=req.getUrl();
    if("https".equalsIgnoreCase(u.getScheme())&&"workto0725.github.io".equalsIgnoreCase(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/boardscore/"))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){Toast.makeText(MainActivity.this,"링크를 열지 못했어요",Toast.LENGTH_SHORT).show();}
    return true;
   }
   @Override public void onPageFinished(WebView v,String url){
    if(!url.startsWith(HOME)){homeVisible=false;refreshBannerVisibility();return;}
    if(previewAds)v.evaluateJavascript("(function(){if(window.__boardscoreBannerWatch)return;window.__boardscoreBannerWatch=true;var home=document.getElementById('chooser');if(!home)return;function update(){BoardScoreDownload.showBanner(!home.classList.contains('hidden'))}new MutationObserver(update).observe(home,{attributes:true,attributeFilter:['class']});update()})();",null);
    v.evaluateJavascript("(function(){if(window.__boardscoreAndroidPng)return;window.__boardscoreAndroidPng=true;document.addEventListener('click',async function(e){const a=e.target.closest('a[download]');if(!a||!a.href.startsWith('blob:'))return;e.preventDefault();try{const b=await(await fetch(a.href)).blob();if(b.type&&b.type!=='image/png')throw Error('PNG only');const reader=new FileReader();reader.onload=function(){BoardScoreDownload.savePng(String(reader.result),a.download||'boardscore.png')};reader.readAsDataURL(b)}catch(e){alert('이미지 저장 실패: '+e.message)}},true)})();",null);
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(picker!=null)picker.onReceiveValue(null);picker=cb;
    try{
     if(params.isCaptureEnabled()){
      java.io.File photo=java.io.File.createTempFile("boardscore_camera_",".jpg",getCacheDir());
      cameraPhotoUri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".fileprovider",photo);
      Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraPhotoUri);
      camera.setClipData(ClipData.newRawUri("photo",cameraPhotoUri));
      camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      startActivityForResult(camera,91);
     }else{
      cameraPhotoUri=null;
      startActivityForResult(params.createIntent(),91);
     }
     return true;
    }catch(Exception e){cameraPhotoUri=null;picker=null;cb.onReceiveValue(null);Toast.makeText(MainActivity.this,"카메라 또는 사진을 열 수 없어요",Toast.LENGTH_SHORT).show();return false;}
   }
  });
  web.addJavascriptInterface(new Object(){
   @JavascriptInterface public void showBanner(boolean show){
    runOnUiThread(()->{homeVisible=show;refreshBannerVisibility();});
   }

   @JavascriptInterface public void savePng(String dataUrl,String filename){
    runOnUiThread(()->{
     try{
      Uri page=Uri.parse(web.getUrl());
      if(!"https".equals(page.getScheme())||!"workto0725.github.io".equals(page.getHost())||!page.getPath().startsWith("/boardscore/"))return;
      if(dataUrl==null||!dataUrl.startsWith("data:image/png;base64,")||dataUrl.length()>16000000)throw new IllegalArgumentException("PNG 형식 오류");
      byte[] bytes=Base64.decode(dataUrl.substring("data:image/png;base64,".length()),Base64.DEFAULT);
      if(bytes.length>12000000)throw new IllegalArgumentException("파일이 너무 커요");
      String name=(filename==null?"boardscore.png":filename).replaceAll("[^a-zA-Z0-9가-힣_.-]","_");
      if(!name.endsWith(".png"))name+=".png";
      if(Build.VERSION.SDK_INT>=29){
       ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"image/png");cv.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/BoardScorePlus");
       Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);
       if(uri==null)throw new IllegalStateException("저장 위치 없음");
       try(OutputStream os=getContentResolver().openOutputStream(uri)){if(os==null)throw new IllegalStateException("저장 오류");os.write(bytes);}
      }else{
       java.io.File dir=getExternalFilesDir(Environment.DIRECTORY_PICTURES);if(dir==null)throw new IllegalStateException("폴더 없음");
       try(java.io.FileOutputStream os=new java.io.FileOutputStream(new java.io.File(dir,name))){os.write(bytes);}
      }
      Toast.makeText(MainActivity.this,"PNG 저장 완료",Toast.LENGTH_LONG).show();
     }catch(Exception e){Toast.makeText(MainActivity.this,"저장 실패: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    });
   }
  },"BoardScoreDownload");
  if(state!=null)web.restoreState(state);else web.loadUrl(HOME+"?app_launch=home");
 }
 private void requestAdConsent(){
  ConsentInformation info=UserMessagingPlatform.getConsentInformation(this);
  ConsentRequestParameters params=new ConsentRequestParameters.Builder().build();
  info.requestConsentInfoUpdate(this,params,()->{
   UserMessagingPlatform.loadAndShowConsentFormIfRequired(this,error->{
    if(info.canRequestAds())allowTestBanner();
   });
   if(info.canRequestAds())allowTestBanner();
  },error->{if(info.canRequestAds())allowTestBanner();});
 }
 private void allowTestBanner(){
  if(!previewAds||banner==null)return;
  bannerAllowed=true;
  if(!bannerRequested){bannerRequested=true;MobileAds.initialize(this,status->{});banner.loadAd(new AdRequest.Builder().build());}
  refreshBannerVisibility();
 }
 private void refreshBannerVisibility(){
  if(banner!=null)banner.setVisibility(previewAds&&bannerAllowed&&homeVisible?View.VISIBLE:View.GONE);
 }
 @Override protected void onSaveInstanceState(Bundle b){web.saveState(b);super.onSaveInstanceState(b);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==91&&picker!=null){
   Uri[] selected=result==RESULT_OK&&cameraPhotoUri!=null?new Uri[]{cameraPhotoUri}:WebChromeClient.FileChooserParams.parseResult(result,data);
   picker.onReceiveValue(selected);picker=null;cameraPhotoUri=null;
  }
 }
 @Override public void onBackPressed(){
  handleAppBack();
 }
 private void handleAppBack(){
  if(web==null){super.onBackPressed();return;}
  String url=web.getUrl();
  if(url==null||!url.startsWith(HOME)){
   if(web.canGoBack())web.goBack();else web.loadUrl(HOME+"?app_launch=home");
   return;
  }
  // The score screen uses in-page navigation, which WebView.canGoBack() cannot reliably detect.
  web.evaluateJavascript("(function(){"+
   "if(typeof closeGameOverlays==='function'&&closeGameOverlays())return 'handled';"+
   "var dice=document.getElementById('diceOverlay');"+
   "if(dice&&!dice.classList.contains('hidden')){if(typeof closeDice==='function')closeDice();return 'handled';}"+
   "var session=document.getElementById('session');"+
   "if(session&&!session.classList.contains('hidden')){if(typeof showHome==='function')showHome();return 'handled';}"+
   "return 'exit';"+
   "})()",result->{
    if("\"exit\"".equals(result))new AlertDialog.Builder(this)
     .setMessage("모두의 보드점수+를 종료할까요?")
     .setNegativeButton("취소",(dialog,which)->{})
     .setPositiveButton("종료",(dialog,which)->finish())
     .show();
   });
 }
 @Override protected void onDestroy(){if(Build.VERSION.SDK_INT>=33&&systemBackCallback!=null){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBackCallback);systemBackCallback=null;}if(banner!=null){banner.destroy();banner=null;}if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
