package so.cym.crashhandlerdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	}
	
	/**
	 * �����ť������������
	 * @param view
	 */
	public void generateCrash(View view){
		int a = 2/0;
	}

}
