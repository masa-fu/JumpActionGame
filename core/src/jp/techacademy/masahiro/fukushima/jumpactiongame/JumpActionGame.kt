package jp.techacademy.masahiro.fukushima.jumpactiongame

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/*
 * ScreenAdapterクラスと呼ばれる1画面に相当するクラスを設定して
 * 簡単に画面遷移を行える機能を持つGameクラスを継承元にする
 */
class JumpActionGame(val mRequestHandler: ActivityRequestHandler) : Game() {
    lateinit var batch: SpriteBatch

    override fun create() {
        batch = SpriteBatch()

        // GameScreenを表示する
        setScreen(GameScreen(this))
    }
}
