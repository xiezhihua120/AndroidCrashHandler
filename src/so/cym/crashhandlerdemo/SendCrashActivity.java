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
 * ����crash��activity����activity���ڱ������Զ������ġ�
 */
public class SendCrashActivity extends Activity {


	private static final String uploadUrl = "http://3.saymagic.sinaapp.com/ReceiveCrash.php";

	/**
	 * localFileUrl
	 * ����log�ļ��Ĵ�ŵ�ַ
	 */
	private static String localFileUrl = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_crash);
		//����Ѹղ��쳣��ջ��Ϣд��SD����Log��־����
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
	 * ����������ͱ�����Ϣ
	 */
	public class SendCrashLog extends AsyncTask<String, String, Boolean> 
	{
		public SendCrashLog() {  }

		@Override
		protected Boolean doInBackground(String... params) 
		{
			Log.d("TAG", "����������ͱ�����Ϣ");
			UploadUtil.uploadFile(new File(localFileUrl), uploadUrl);
			return null;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Toast.makeText(getApplicationContext(), "�ɹ���������Ϣ���͵�����������л���ķ���", 1000).show();
			Log.d("TAG", "�������");
			
		}
	}
}
