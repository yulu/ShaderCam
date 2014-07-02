package com.littlecheesecake.shadercam.gl;

import android.opengl.GLES20;

/**
 * This class defines a RenderTarget, a framebuffer object and a texture is generated.
 * Camera frame is renderer to this target, and the texture can be further process in the rendering
 * pipeline
 * @author yulu
 *
 */
public class FBORenderTarget {
	
	private int mTextureHandles[] = {};
	private int mFramebufferHandle = 1;
	
	public int width, height;

	public FBORenderTarget() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * bind the framebuffer object to use
	 */
	public void bindFBO(int i){
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);
		GLES20.glViewport(0, 0, width, height);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, 
									GLES20.GL_COLOR_ATTACHMENT0, 
									GLES20.GL_TEXTURE_2D, 
									mTextureHandles[i], 0);
	}
	
	/**
	 * get the texture id the camera frame rendered to
	 */
	public int getTextureId(int i){
		return mTextureHandles[i];
	}
	
	/**
	 * init framebuffer
	 */
	public void init(int width, int height){
		reset();
		
		this.width = width;
		this.height = height;
		
		int handle[] = {0};
		GLES20.glGenFramebuffers(1, handle, 0);
		mFramebufferHandle = handle[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);
		
		mTextureHandles = new int[2];
		GLES20.glGenTextures(2, mTextureHandles, 0);
		for(int handler : mTextureHandles){
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handler);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, 
					GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		}
		
		
	}
	
	public void reset(){
		int[] handle = {mFramebufferHandle};
		GLES20.glDeleteFramebuffers(1, handle, 0);
		GLES20.glDeleteTextures(mTextureHandles.length, mTextureHandles, 0);
		mFramebufferHandle = -1;
		mTextureHandles = new int[0];
		width = height = 0;
	}


}
