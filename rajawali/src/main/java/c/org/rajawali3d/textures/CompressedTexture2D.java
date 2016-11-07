/**
 * Copyright 2013 Dennis Ippel
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package c.org.rajawali3d.textures;

import android.opengl.GLES20;
import c.org.rajawali3d.textures.annotation.Compression2D.CompressionType2D;
import c.org.rajawali3d.textures.annotation.Filter;
import c.org.rajawali3d.textures.annotation.Wrap;

import java.nio.ByteBuffer;

/**
 * Abstract implementation of 2D compressed texture.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public abstract class CompressedTexture2D extends SingleTexture2D {

    protected ByteBuffer[] mByteBuffers;

    /**
     * Texture2D compression type
     */
    @CompressionType2D
    protected int mCompressionType;
    /**
     * Bitmap compression format. Use together with {@link CompressionType2D}
     */
    protected int mCompressionFormat;

    protected CompressedTexture2D() throws TextureException {
        super();
        setWrapType(Wrap.REPEAT_S | Wrap.REPEAT_T | Wrap.REPEAT_R);
    }

    public CompressedTexture2D(CompressedTexture2D other) throws TextureException {
        this();
        setFrom(other);
    }

    public CompressedTexture2D(String textureName) throws TextureException {
        this();
        setTextureName(textureName);
    }

    public CompressedTexture2D(String textureName, ByteBuffer byteBuffer) throws TextureException {
        this(textureName);
        setByteBuffer(byteBuffer);
    }

    public CompressedTexture2D(String textureName, ByteBuffer[] byteBuffers) throws TextureException {
        this(textureName);
        setByteBuffers(byteBuffers);
    }

    /**
     * Copies every property from another ACompressedTexture object
     *
     * @param other
     *            another ACompressedTexture object to copy from
     */
    public void setFrom(CompressedTexture2D other) throws TextureException {
        super.setFrom(other);
        mCompressionType = other.getCompressionType();
        mCompressionFormat = other.getCompressionFormat();
    }

    /**
     * @return the texture compression type
     */
    @CompressionType2D
    public int getCompressionType() {
        return mCompressionType;
    }

    /**
     * @param compressionType
     *            the texture compression type
     */
    public void setCompressionType(@CompressionType2D int compressionType) {
        this.mCompressionType = compressionType;
    }

    /**
     * @return the Bitmap compression format
     */
    public int getCompressionFormat() {
        return mCompressionFormat;
    }

    /**
     * @param compressionFormat
     *            the Bitmap compression format
     */
    public void setCompressionFormat(int compressionFormat) {
        this.mCompressionFormat = compressionFormat;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        setByteBuffers(new ByteBuffer[]{byteBuffer});
    }

    public void setByteBuffers(ByteBuffer[] byteBuffers) {
        mByteBuffers = byteBuffers;
    }

    public ByteBuffer[] getByteBuffers() {
        return mByteBuffers;
    }

    void add() throws TextureException {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];
        if (textureId > 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            @Filter.FilterType final int filterType = getFilterType();
            @Wrap.WrapType final int wrapType = getWrapType();

            if (filterType == Filter.BILINEAR)
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            else
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

            if (filterType == Filter.BILINEAR)
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            else
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            if (wrapType == (Wrap.REPEAT_S | Wrap.REPEAT_T | Wrap.REPEAT_R)) {
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            } else {
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            }
            if ((mByteBuffers != null && mByteBuffers.length == 0) || mByteBuffers == null) {
                GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, 0, mCompressionFormat, getWidth(), getHeight(), 0, 0, null);
            } else {
                int w = getWidth(), h = getHeight();
                for (int i = 0; i < mByteBuffers.length; i++) {
                    GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, i, mCompressionFormat, w, h, 0,
                        mByteBuffers[i].capacity(), mByteBuffers[i]);
                    w = w > 1 ? w / 2 : 1;
                    h = h > 1 ? h / 2 : 1;
                }
            }
            setTextureId(textureId);
        } else {
            throw new TextureException("Couldn't generate a texture name.");
        }

        for (int i = 0; i < mByteBuffers.length; i++) {
            if (mByteBuffers[i] != null) {
                mByteBuffers[i].limit(0);
            }
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    void remove() throws TextureException {
        GLES20.glDeleteTextures(1, new int[]{getTextureId()}, 0);
    }

    void replace() throws TextureException {
        if (mByteBuffers == null || mByteBuffers.length == 0)
            throw new TextureException("Texture2D could not be replaced because there is no ByteBuffer set.");

        if (getWidth() == 0 || getHeight() == 0)
            throw new TextureException(
                "Could not update ByteBuffer texture. One or more of the following properties haven't been set: width or height");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < mByteBuffers.length; i++) {
            GLES20.glCompressedTexSubImage2D(GLES20.GL_TEXTURE_2D, i, 0, 0, w, h, mCompressionFormat,
                mByteBuffers[i].capacity(), mByteBuffers[i]);
            w = w > 1 ? w / 2 : 1;
            h = h > 1 ? h / 2 : 1;
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    void reset() throws TextureException {
        if (mByteBuffers != null) {
            for (int i = 0; i < mByteBuffers.length; i++) {
                if (mByteBuffers[i] != null) {
                    mByteBuffers[i].limit(0);
                }
            }
        }
    }
}
