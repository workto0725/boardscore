package io.github.workto0725.boardscoreplus;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.widget.Toast;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.os.Environment;
import android.util.Base64;
import java.io.OutputStream;
public class MainActivity extends Activity {
 private static final String HOME="https://workto0725.github.io/boardscore/";
 private WebView web;
 private ValueCallback<Uri[]> picker;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  web=new WebView(this);setContentView(web);
  web.setBackgroundColor(Color.rgb(18,49,40));
  WebSettings st=web.getSettings();
  st.setJavaScriptEnabled(true);st.setDomStorageEnabled(true);st.setAllowFileAccess(false);
  st.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){
    Uri u=req.getUrl();
    if("https".equalsIgnoreCase(u.getScheme())&&"workto0725.github.io".equalsIgnoreCase(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/boardscore/"))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){Toast.makeText(MainActivity.this,"링크를 열지 못했어요",Toast.LENGTH_SHORT).show();}
    return true;
   }
   @Override public void onPageFinished(WebView v,String url){
    if(!url.startsWith(HOME))return;
    v.evaluateJavascript("(function(){if(window.__boardscoreAndroidPng)return;window.__boardscoreAndroidPng=true;document.addEventListener('click',async function(e){const a=e.target.closest('a[download]');if(!a||!a.href.startsWith('blob:'))return;e.preventDefault();try{const b=await(await fetch(a.href)).blob();if(b.type&&b.type!=='image/png')throw Error('PNG only');const reader=new FileReader();reader.onload=function(){BoardScoreDownload.savePng(String(reader.result),a.download||'boardscore.png')};reader.readAsDataURL(b)}catch(e){alert('이미지 저장 실패: '+e.message)}},true)})();",null);
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(picker!=null)picker.onReceiveValue(null);picker=cb;
    try{startActivityForResult(params.createIntent(),91);return true;}catch(Exception e){picker=null;cb.onReceiveValue(null);return false;}
   }
  });
  web.addJavascriptInterface(new Object(){
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
 @Override protected void onSaveInstanceState(Bundle b){web.saveState(b);super.onSaveInstanceState(b);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==91&&picker!=null){picker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));picker=null;}
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}