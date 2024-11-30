package com.tlab.viewtobuffer;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

public class ViewToPBORenderer extends ViewToBufferRenderer {

    private int[] mGlPboId;
    private boolean[] mGlPboReady;
    private int mNowWritePboId;
    private int mNowReadPboId;
    private byte[] mPixelBuffer;

    @Override
    protected void destroyBuffer() {
        if (mGlPboId != null) {
            GLES30.glDeleteBuffers(mGlPboId.length, mGlPboId, 0);
            mGlPboId = null;
        }
    }

    @Override
    protected void initBuffer() {
        if (mEglCore.getGlVersion() >= 3) {
            GLES30.glGetError();
            boolean bError = false;
            int[] glPbos = new int[3];
            GLES30.glGenBuffers(glPbos.length, glPbos, 0);
            mGlPboReady = new boolean[glPbos.length];
            for (int i = 0; i < glPbos.length; i++) {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, glPbos[i]);
                int errNo = GLES30.glGetError();
                if (errNo != 0) {
                    bError = true;
                    break;
                }
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mTexSize.x * mTexSize.y * 4, null, GLES30.GL_STREAM_READ);
                mGlPboReady[i] = true;
            }
            if (bError) {
                GLES30.glDeleteBuffers(glPbos.length, glPbos, 0);
            } else {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
                mGlPboId = glPbos;
            }
            mNowWritePboId = 0;
            mNowReadPboId = glPbos.length - 1;
        }
    }

    @Override
    public void CopySurfaceTextureToBuffer() {
        if (!mInitialized || mGlPboId == null) return;

        GLES30.glUseProgram(mGlSamplerProgram);

        GLES30.glViewport(0, 0, mTexSize.x, mTexSize.y);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureID[0]);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGlCubeID[0]);
        GLES30.glVertexAttribPointer(mGlSamplerPositionID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(mGlSamplerPositionID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGlTexCoordID[0]);
        GLES30.glVertexAttribPointer(mGlSamplerTexCoordID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(mGlSamplerTexCoordID);

        GLES30.glUniform1i(mGlSamplerTexID, 0);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        int id = (mNowWritePboId + 1) % mGlPboId.length;
        if (mGlPboReady[id]) {
            mNowWritePboId = id;
            mNowReadPboId = (mNowReadPboId + 1) % mGlPboId.length;
            glUtilReadPixels(0, 0, mTexSize.x, mTexSize.y, mGlPboId[mNowWritePboId]);
            mGlPboReady[id] = false;
        }
        mPixelBuffer = glUtilGetPboBuffer(mTexSize.x, mTexSize.y, mGlPboId[mNowReadPboId]);
        mGlPboReady[mNowReadPboId] = true;

        GLES30.glDisableVertexAttribArray(mGlSamplerPositionID);
        GLES30.glDisableVertexAttribArray(mGlSamplerTexCoordID);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Log.i(TAG, "[VPBOR] [CopySurfaceTextureToBuffer]");
    }

    public byte[] getPixelBuffer() {
        return mPixelBuffer;
    }

    public native void glUtilReadPixels(int x, int y, int width, int height, int pboId);

    public native byte[] glUtilGetPboBuffer(int width, int height, int pboId);

    static {
        System.loadLibrary("glutil");
    }
}
