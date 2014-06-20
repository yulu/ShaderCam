package com.littlecheesecake.shadercam.gl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import com.littlecheesecake.shadercameraexample.R;

public class CameraRenderer extends GLSurfaceView implements 
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener{
	private Context mContext;
	
	/**
	 * Camera and SurfaceTexture
	 */
	private Camera mCamera;
	private SurfaceTexture mSurfaceTexture;
	
	private final FBORenderTarget mRenderTarget = new FBORenderTarget();
	private final OESTexture mCameraTexture = new OESTexture();
	private final Shader mOffscreenShader = new Shader();
	private final Shader mOnscreenShader = new Shader();
	private int mWidth, mHeight;
	private boolean updateTexture = false;
	
	/**
	 * OpenGL params
	 */
	private ByteBuffer mFullQuadVertices;
	private float[] mTransformM = new float[16];
	private float[] mOrientationM = new float[16];
	
	public CameraRenderer(Context context) {
		super(context);
		mContext = context;
		init();
	}
	
	public CameraRenderer(Context context, AttributeSet attrs){
		super(context, attrs);
		mContext = context;
		init();
	}
	
	private void init(){
		//Create full scene quad buffer
		final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
		mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
		mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);
		
		setPreserveEGLContextOnPause(true);
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		Matrix.setRotateM(mOrientationM, 0, 90.0f, 0f, 0f, 1f);
	}
	
	@Override
	public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture){
		updateTexture = true;
		requestRender();
	}


	@Override
	public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//load and compile shader
		
		try {
			mOffscreenShader.setProgram(R.raw.offscreen_vshader, R.raw.offscreen_fshader, mContext);
			mOnscreenShader.setProgram(R.raw.onscreen_vshader, R.raw.onscreen_fshader, mContext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

	@SuppressLint("NewApi")
	@Override
	public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight= height;
		
		//reinit render target
		if(mRenderTarget.width != mWidth || mRenderTarget.height != mHeight){
			mRenderTarget.init(mWidth, mHeight);
		}
		
		//generate camera texture
		mCameraTexture.init();
		
		//set up surfacetexture
		SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
		mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId());
		mSurfaceTexture.setOnFrameAvailableListener(this);
		if(oldSurfaceTexture != null){
			oldSurfaceTexture.release();
		}
		
		//start camera
		if(mCamera != null){
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		mCamera = Camera.open();
		try{
			mCamera.setPreviewTexture(mSurfaceTexture);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		Camera.Parameters param = mCamera.getParameters();
		List<Size> psize = param.getSupportedPreviewSizes();
		if(psize.size() > 0 ){
			int i;
			for (i = 0; i < psize.size(); i++){
				if(psize.get(i).width < width || psize.get(i).height < height)
					break;
			}
			if(i>0)
				i--;
			param.setPreviewSize(psize.get(i).width, psize.get(i).height);
			Log.d("CameraRenderer", "Set preview size to "+Integer.valueOf((int)psize.get(i).width) + "x" +
					Integer.valueOf((int)psize.get(i).height));
		}	
		mCamera.setParameters(param);	
		mCamera.startPreview();
		
		
		requestRender();		
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		//render the texture to FBO
		if(updateTexture){
			mSurfaceTexture.updateTexImage();
			mSurfaceTexture.getTransformMatrix(mTransformM);
			
			updateTexture = false;
		
			mRenderTarget.bindFBO();
			
			mOffscreenShader.useProgram();
			
			int uTransformM = mOffscreenShader.getHandle("uTransformM");
			int uOrientationM = mOffscreenShader.getHandle("uOrientationM");
			
			GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
			GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
			
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCameraTexture.getTextureId());
			
			renderQuad(mOffscreenShader.getHandle("aPosition"));
		}
		
		//bind screen buffer into use, render the texture in FBO to screen
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		
		mOnscreenShader.useProgram();
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTarget.getTextureId());
		
		renderQuad(mOnscreenShader.getHandle("aPosition"));
		
	}
	
	private void renderQuad(int aPosition){
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mFullQuadVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void onDestroy(){
		updateTexture = false;
		mSurfaceTexture.release();
		if(mCamera != null){
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
		}
		
		mCamera = null;
	}
	
}
