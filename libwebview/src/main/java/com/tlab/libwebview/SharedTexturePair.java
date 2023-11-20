package com.tlab.libwebview;

import com.robot9.shared.SharedTexture;
public class SharedTexturePair {

    public SharedTexturePair(int id, SharedTexture texture) {
        this.id = id;
        this.texture = texture;
    }

    public int id;
    public SharedTexture texture;
}
