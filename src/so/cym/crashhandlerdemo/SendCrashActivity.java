package so.cym.crashhandlerdemo;

import java.io.File;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

/**
 * 
 * @author hzcaoyanming
 *
 * 发送crash的activity。该activity是在崩溃后自动重启的。
 */
public class SendCrashActivity extends Activity {


	private static final String uploadUrl = "http://3.saymagic.sinaapp.com/ReceiveCrash.php";

	/**
	 * localFileUrl
	 * 本地log文件的存放地址
	 */
	private static String localFileUrl = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_crash);
		//这里把刚才异常堆栈信息写入SD卡的Log日志里面
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 
		{
			String sdcardPath = Environment.getExternalStorageDirectory().getPath();
			localFileUrl = sdcardPath + "/cym/crash/crash.log";
		}
	}

	public void sendCrash(View view){
		new SendCrashLog().execute("");

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.send_crash, menu);
		return true;
	}
	/**
	 * 
	 * @author hzcaoyanming
	 * 向服务器发送崩溃信息
	 */
	public class SendCrashLog extends AsyncTask<String, String, Boolean> 
	{
		public SendCrashLog() {  }

		@Override
		protected Boolean doInBackground(String... params) 
		{
			Log.d("TAG", "向服务器发送崩溃信息");
			UploadUtil.uploadFile(new File(localFileUrl), uploadUrl);
			return null;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Toast.makeText(getApplicationContext(), "成功将崩溃信息发送到服务器，感谢您的反馈", 1000).show();
			Log.d("TAG", "发送完成");
			
		}
	}
}
