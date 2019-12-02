package jp.techacademy.masahiro.fukushima.jumpactiongame

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite

class Star(texture: Texture, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int)
    : Sprite(texture, srcX, srcY, srcWidth, srcHeight) {

    companion object {
        // 横幅、高さ
        val STAR_WIDTH = 0.8f
        val STAR_HEIGHT = 0.8f

        // 状態
        val STAR_EXIST = 0
        val STAR_NONE = 1
    }

    var mState: Int = 0

    init {
        setSize(STAR_WIDTH, STAR_HEIGHT)
        mState = STAR_EXIST
    }

    // getメソッドはプレイヤーが触れる時に呼ばれる
    fun get() {
        // 状態をSTAR_NONEにし、setAlphaメソッドで透明にする
        mState = STAR_NONE
        setAlpha(0f)
    }
}