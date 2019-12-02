package jp.techacademy.masahiro.fukushima.jumpactiongame

import com.badlogic.gdx.graphics.Texture

class Enemy(texture: Texture, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int)
    : GameObject(texture, srcX, srcY, srcWidth, srcHeight) {

    companion object {
        // 横幅、高さ
        val ENEMY_WIDTH = 1.4f
        val ENEMY_HEIGHT = 0.8f

        // 速度
        val STEP_VELOCITY = 1.0f
    }

    init {
        setSize(ENEMY_WIDTH, ENEMY_HEIGHT)
        velocity.x = STEP_VELOCITY
    }

    // 座標を更新する
    fun update(deltaTime: Float) {
        x += velocity.x * deltaTime

        if (x < Step.STEP_WIDTH / 2) {
            velocity.x = -velocity.x
            x = Step.STEP_WIDTH / 2
        }
        if (x > GameScreen.WORLD_WIDTH - Step.STEP_WIDTH / 2) {
            velocity.x = -velocity.x
            x = GameScreen.WORLD_WIDTH - Step.STEP_WIDTH / 2
        }
    }

}