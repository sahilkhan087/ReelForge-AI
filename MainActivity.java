package com.reelforge.ai;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.view.*;import android.widget.*;import java.io.*;import java.net.*;import java.util.concurrent.*;
public class MainActivity extends Activity{
 LinearLayout root,list; TextView status; ProgressBar bar;
 final String API="http://10.0.2.2:8000";
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 TextView t(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(z);v.setPadding(dp(10),dp(8),dp(10),dp(8));return v;}
 public void onCreate(Bundle b){super.onCreate(b);home();}
 void home(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(20),dp(16),dp(12));root.setBackgroundColor(Color.rgb(9,7,13));
 root.addView(t("⚡ ReelForge AI v5",26));root.addView(t("Real upload → AI processing → Shorts",15));
 Button up=new Button(this);up.setText("🎬 Upload Long Video");root.addView(up);up.setOnClickListener(v->pick());
 status=t("Backend ready. Upload a video.",14);root.addView(status);bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);root.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));
 ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));root.addView(t("📅 30-Day Content Factory",18));setContentView(root);}
 void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,9);}
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==9&&c==RESULT_OK)upload(d.getData());}
 void upload(Uri uri){status.setText("Uploading…");new Thread(()->{try{
   URL u=new URL(API+"/upload");HttpURLConnection q=(HttpURLConnection)u.openConnection();q.setDoOutput(true);q.setRequestMethod("POST");String boundary="----RF";
   q.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);OutputStream o=q.getOutputStream();String head="--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"video.mp4\"\r\nContent-Type: video/mp4\r\n\r\n";o.write(head.getBytes());
   InputStream in=getContentResolver().openInputStream(uri);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)o.write(buf,0,n);in.close();o.write(("\r\n--"+boundary+"--\r\n").getBytes());o.close();
   BufferedReader br=new BufferedReader(new InputStreamReader(q.getInputStream()));StringBuilder s=new StringBuilder();String x;while((x=br.readLine())!=null)s.append(x);String js=s.toString();String id=js.replaceAll(".*\"job_id\"\\s*:\\s*\"([^\"]+)\".*","$1");runOnUiThread(()->status.setText("AI job started: "+id));poll(id);
 }catch(Exception e){runOnUiThread(()->status.setText("Connection error: "+e.getMessage()));}}).start();}
 void poll(String id){new Thread(()->{for(int k=0;k<120;k++){try{Thread.sleep(3000);URL u=new URL(API+"/status/"+id);HttpURLConnection q=(HttpURLConnection)u.openConnection();BufferedReader b=new BufferedReader(new InputStreamReader(q.getInputStream()));StringBuilder s=new StringBuilder();String x;while((x=b.readLine())!=null)s.append(x);String js=s.toString();if(js.contains("\"status\":\"completed\"")){runOnUiThread(()->showResults(js));return;}if(js.contains("\"status\":\"failed\"")){runOnUiThread(()->status.setText("AI processing failed"));return;}runOnUiThread(()->status.setText("🧠 AI processing…"));}catch(Exception e){}}}).start();}
 void showResults(String js){bar.setProgress(100);status.setText("✅ 5 Shorts ready");list.removeAllViews();for(int i=1;i<=5;i++){list.addView(t("🎞 Short "+i+"   🔥 AI Ranked\n✨ Hook + Title + Hashtags\n📱 1080×1920 MP4",17));Button b=new Button(this);b.setText("📤 Export / Preview");list.addView(b);}}}