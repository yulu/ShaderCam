package com.littlecheesecake.shadercam;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;
import android.view.WindowManager;


import com.littlecheesecake.shadercam.gl.CameraRenderer;
import com.littlecheesecake.shadercameraexample.R;

public class MainActivity extends ActionBarActivity {
	private CameraRenderer mRenderer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);
		mRenderer = (CameraRenderer)findViewById(R.id.renderer_view);
	}

	@Override
	public void onStart(){
		super.onStart();

	}
	
	
	@Override
	public void onPause(){
		super.onPause();
		mRenderer.onDestroy();
		
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mRenderer.onResume();
	}

}
