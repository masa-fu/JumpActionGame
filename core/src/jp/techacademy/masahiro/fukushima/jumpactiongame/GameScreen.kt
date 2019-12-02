package jp.techacademy.masahiro.fukushima.jumpactiongame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.OrthographicCamera
import java.util.*
import com.badlogic.gdx.audio.Sound



class GameScreen(private val mGame: JumpActionGame) : ScreenAdapter() {
    // カメラのサイズを表す定数を定義
    // 物理的なディスプレイのサイズに依存しないサイズとなる
    companion object {
        val CAMERA_WIDTH = 10f
        val CAMERA_HEIGHT = 15f
        val WORLD_WIDTH = 10f
        val WORLD_HEIGHT = 15 * 20    // 20画面分登れば終了
        val GUI_WIDTH = 320f
        val GUI_HEIGHT = 480f

        // ゲームの状態を表す定数
        // ゲーム開始前
        val GAME_STATE_READY = 0
        // ゲーム中
        val GAME_STATE_PLAYING = 1
        // ゴールか落下してゲーム終了
        val GAME_STATE_GAMEOVER = 2

        // 重力
        val GRAVITY = -12
    }

    private val mBg: Sprite
    // カメラを表すOrthographicCameraクラスをプロパティとして定義
    private val mCamera: OrthographicCamera
    private val mGuiCamera: OrthographicCamera
    // ビューポートのFitViewportクラスをプロパティとして定義
    private val mViewPort: FitViewport
    private val mGuiViewPort: FitViewport

    // mRandom : 乱数(ランダムに生成される数字)を取得するためのクラス
    private var mRandom: Random
    // mSteps : 生成して配置した踏み台を保持するリスト
    private var mSteps: ArrayList<Step>
    // mStars : 生成して配置した★を保持するリスト
    private var mStars: ArrayList<Star>
    // mUfo : 生成して配置したUFO(ゴール)を保持する
    private lateinit var mUfo: Ufo
    // mPlayer : 生成して配置したプレイヤーを保持する
    private lateinit var mPlayer: Player
    // mEnemys : 生成して配置した敵を保持する
    private var mEnemys: ArrayList<Enemy>

    // 敵に当たった時のサウンドを設定
    val sound = Gdx.audio.newSound(Gdx.files.internal("attack.mp3"))

    // ゲームの状態を保持するプロパティを定義
    private var mGameState: Int
    // どの高さからプレイヤーが地面からどれだけ離れたかを保持するプロパティを定義
    private var mHeightSoFar: Float = 0f
    // タッチされた座標を保持するプロパティを定義
    private var mTouchPoint: Vector3
    // BitmapFontクラスのプロパティを定義
    private var mFont: BitmapFont
    // 現在のスコアを保持するプロパティを定義
    private var mScore: Int
    // ハイスコアを定義するプロパティを定義
    private var mHighScore: Int
    private var mPrefs: Preferences

    init {
        // 背景の準備
        val bgTexture = Texture("back.png")
        // TextureRegionで切り出す時の原点は左上
        mBg = Sprite(TextureRegion(bgTexture, 0, 0, 540, 810))
        // SpriteクラスのsetPositionメソッドで描画する位置を指定する
        // 左下を基準として位置を指定する
        mBg.setSize(CAMERA_WIDTH, CAMERA_HEIGHT)
        mBg.setPosition(0f, 0f)

        // カメラ、ViewPortを生成、設定する
        mCamera = OrthographicCamera()
        // コンストラクタでプロパティに初期化して代入
        // カメラのサイズとビューポートのサイズを同じにし、縦横比を固定する
        mCamera.setToOrtho(false, CAMERA_WIDTH, CAMERA_HEIGHT)
        mViewPort = FitViewport(CAMERA_WIDTH, CAMERA_HEIGHT, mCamera)

        // GUI用のカメラを設定する
        mGuiCamera = OrthographicCamera()
        mGuiCamera.setToOrtho(false, GUI_WIDTH, GUI_HEIGHT)
        mGuiViewPort = FitViewport(GUI_WIDTH, GUI_HEIGHT, mGuiCamera)

        // プロパティの初期化
        mRandom = Random()
        mSteps = ArrayList<Step>()
        mStars = ArrayList<Star>()
        mEnemys = ArrayList<Enemy>()
        mGameState = GAME_STATE_READY
        mTouchPoint = Vector3()

        // フォントファイルを指定して読み込み
        mFont = BitmapFont(Gdx.files.internal("font.fnt"), Gdx.files.internal("font.png"), false)
        // フォントサイズ変更
        mFont.data.setScale(0.8f)
        mScore = 0
        mHighScore = 0

        // ハイスコアをPreferencesから取得する
        mPrefs = Gdx.app.getPreferences("jp.techacademy.masahiro.fukushima.jumpactiongame")
        // 取得したPreferencesのgetIntegerメソッドにキーを与えて値を取得する
        // 第2引数はキーに対応する値がなかった場合に返ってくる値（初期値）となる
        mHighScore = mPrefs.getInteger("HIGHSCORE", 0)

        createStage()
    }

    // コンストラクタで準備したスプライトをrenderメソッド内で描画する
    // ScreenAdapterを継承したクラスのrenderメソッドは基本的に1/60秒ごとに自動的に呼び出される
    override fun render(delta: Float) {
        // それぞれの状態をアップデートする
        update(delta)

        // glClearColorメソッドは画面をクリアする時の色を赤、緑、青、透過で指定する
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        // Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)で実際にその色でクリア（塗りつぶし）を行う
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // カメラの中心を超えたらカメラを上に移動させる つまりキャラが画面の上半分には絶対に行かない
        if (mPlayer.y > mCamera.position.y) {
            mCamera.position.y = mPlayer.y
        }

        // カメラの座標をアップデート(計算)し、スプライトの表示に反映させる
        // (カメラのupdateメソッドでは行列演算を行ってカメラの座標値の再計算を行ってくれるメソッド)
        mCamera.update()
        // SpriteBatchクラスのsetProjectionMatrixメソッドをOrthographicCameraクラスの
        // combinedプロパティを引数に与えて呼び出す
        // (物理ディスプレイに依存しない表示を行うことができる)
        // setProjectionMatrixメソッドとcombinedメソッドで座標をスプライトに反映
        mGame.batch.projectionMatrix = mCamera.combined

        // スプライトを描画する際はSpriteBatchクラスのbeginメソッドとendメソッドの間で行う
        mGame.batch.begin()

        // 背景
        // 原点は左下
        mBg.setPosition(mCamera.position.x - CAMERA_WIDTH / 2, mCamera.position.y - CAMERA_HEIGHT / 2)

        // Spriteクラスのdrawメソッドを呼び出すことで描画する
        mBg.draw(mGame.batch)

        // Step
        for (i in 0 until mSteps.size) {
            mSteps[i].draw(mGame.batch)
        }

        // Star
        for (i in 0 until mStars.size) {
            mStars[i].draw(mGame.batch)
        }

        // UFO
        mUfo.draw(mGame.batch)

        //Player
        mPlayer.draw(mGame.batch)

        // Enemy
        for (i in 0 until mEnemys.size) {
            mEnemys[i].draw(mGame.batch)
        }

        mGame.batch.end()

        // スコア表示
        mGuiCamera.update()
        mGame.batch.projectionMatrix = mGuiCamera.combined
        mGame.batch.begin()
        // BitmapFontクラスのdrawメソッドで描画する
        // 第1引数にSpriteBatch、第2引数に表示されたい文字列、第3引数にx座標、第4引数にy座標を指定
        mFont.draw(mGame.batch, "HighScore: $mHighScore", 16f, GUI_HEIGHT - 15)
        mFont.draw(mGame.batch, "Score: $mScore", 16f, GUI_HEIGHT - 35)
        mGame.batch.end()
    }

    // resizeメソッドをオーバーライドしてFitViewportクラスのupdateメソッドを呼び出す
    // resizeメソッドは物理的な画面のサイズが変更されたときに呼び出されるメソッド
    // Androidではcreate直後やバックグランドから復帰したときに呼び出される
    override fun resize(width: Int, height: Int) {
        mViewPort.update(width, height)
        mGuiViewPort.update(width, height)
    }

    // ステージを作成する
    private fun createStage() {

        // テクスチャの準備
        val stepTexture = Texture("step.png")
        val starTexture = Texture("star.png")
        val playerTexture = Texture("uma.png")
        val ufoTexture = Texture("ufo.png")
        val enemyTexture = Texture("enemy.png")

        // StepとStar、Enemyをゴールの高さまで配置していく
        var y = 0f

        val maxJumpHeight = Player.PLAYER_JUMP_VELOCITY * Player.PLAYER_JUMP_VELOCITY / (2 * -GRAVITY)
        while (y < WORLD_HEIGHT - 5) {
            val type = if(mRandom.nextFloat() > 0.8f) Step.STEP_TYPE_MOVING else Step.STEP_TYPE_STATIC
            // 乱数はmRandom.nextFloat()のように呼び出すと 0.0 から 1.0 までの値が取得できる
            // 1/2の確率で何か行いたい場合はmRandom.nextFloat() > 0.5のように条件判断を行う
            val x = mRandom.nextFloat() * (WORLD_WIDTH - Step.STEP_WIDTH)

            val step = Step(type, stepTexture, 0, 0, 144, 36)
            step.setPosition(x, y)
            mSteps.add(step)

            if (mRandom.nextFloat() > 0.6f) {
                val star = Star(starTexture, 0, 0, 72, 72)
                star.setPosition(step.x + mRandom.nextFloat(), step.y + Star.STAR_HEIGHT + mRandom.nextFloat() * 3)
                mStars.add(star)
            }

            if (mRandom.nextFloat() > 0.8f) {
                val enemy = Enemy(enemyTexture, 0, 0, 100, 72)
                enemy.setPosition(step.x + mRandom.nextFloat(), step.y + Enemy.ENEMY_HEIGHT + mRandom.nextFloat() * 3)
                mEnemys.add(enemy)
            }

            y += (maxJumpHeight - 0.5f)
            y -= mRandom.nextFloat() * (maxJumpHeight / 3)
        }

        // Playerを配置
        mPlayer = Player(playerTexture, 0, 0, 72, 72)
        mPlayer.setPosition(WORLD_WIDTH / 2 - mPlayer.width / 2, Step.STEP_HEIGHT)

        // ゴールのUFOを配置
        mUfo = Ufo(ufoTexture, 0, 0, 120, 74)
        mUfo.setPosition(WORLD_WIDTH / 2 - Ufo.UFO_WIDTH / 2, y)
    }

    // それぞれのオブジェクトの状態をアップデートする
    private fun update(delta: Float) {
        when (mGameState) {
            GAME_STATE_READY ->
                updateReady()
            GAME_STATE_PLAYING ->
                updatePlaying(delta)
            GAME_STATE_GAMEOVER ->
                updateGameOver()
        }
    }
    private fun updateReady() {
        if (Gdx.input.justTouched()) {
            // タッチされたら状態をゲーム中であるGAME_STATE_PLAYINGに変更する
            mGameState = GAME_STATE_PLAYING
        }
    }

    private fun updatePlaying(delta: Float) {
        var accel = 0f
        if (Gdx.input.isTouched) {
            // タッチされたら、その座標が画面の左側か右側かを判断
            // タッチされた座標はGdx.input.xとGdx.input.yで取得できる
            // それらの値をmTouchPointにsetメソッドで設定
            // Vector3クラスはx,yだけでなくZ軸を保持するプロパティzも持っているためsetメソッドの第3引数には0f
            // そのmTouchPointをOrthographicCameraクラスのunprojectメソッドに与えて呼び出す
            // ことでカメラを使った座標に変換する
            mGuiViewPort.unproject(mTouchPoint.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            // 画面のどこをタッチされたかの判断はRectangleクラスの左半分を表す短形leftと 右半分を表す短形rightを
            // 定義し、containsメソッドにタッチされた座標を与えることでその領域をタッチしているのか判断
            val left = Rectangle(0f, 0f, GUI_WIDTH / 2, GUI_HEIGHT)
            val right = Rectangle(GUI_WIDTH / 2, 0f, GUI_WIDTH / 2, GUI_HEIGHT)
            if (left.contains(mTouchPoint.x, mTouchPoint.y)) {
                // 左側をタッチされた時は加速度としてaccel = 5.0f
                accel = 5.0f
            }
            if (right.contains(mTouchPoint.x, mTouchPoint.y)) {
                // 右側をタッチされたときはaccel = -5.0fを設定する
                accel = -5.0f
            }
        }

        // Step
        for (i in 0 until mSteps.size) {
            // 踏み台の状態を更新させるためStepクラスのupdateメソッドを呼び出し
            mSteps[i].update(delta)
        }

        // Enemy
        for (i in 0 until mEnemys.size) {
            mEnemys[i].update(delta)
        }

        // Player
        if (mPlayer.y <= 0.5f) {
            // プレイヤーの座標が0.5以下になった場合は踏み台に乗ったと同じ処理(hitStepメソッド)を行い、ジャンプさせる
            // (ゲーム開始時にジャンプさせるための処理)
            mPlayer.hitStep()
        }
        // 加速度をPlayerクラスのupdateメソッドに与えて呼び出す
        mPlayer.update(delta, accel)
        // updatePlayingメソッドの最後はプレイヤーがどれだけ地面から離れたかを、Mathクラスのmaxメソッドを呼び出して
        // 保持している距離か、今のプレイヤーの高さか大きい方を保持する
        mHeightSoFar = Math.max(mPlayer.y, mHeightSoFar)

        // 当たり判定を行う
        checkCollision()

        // ゲームオーバーか判断する
        checkGameOver()
    }

    private fun updateGameOver() {
        // ゲーム終了後にタッチしたら結果画面に遷移する
        if (Gdx.input.justTouched()) {
            mGame.screen = ResultScreen(mGame, mScore)
        }
    }

    private fun checkGameOver() {
        // プレイヤーの地面との距離であるmHeightSoFarから、
        // カメラの高さの半分を引いた値よりプレイヤーの位置が低くなったらゲームオーバーとする
        // (画面の下までプレイヤーが落ちたらゲームオーバーとすることを表す)
        if (mHeightSoFar - CAMERA_HEIGHT / 2 > mPlayer.y) {
            Gdx.app.log("JampActionGame", "GAMEOVER")
            mGameState = GAME_STATE_GAMEOVER
        }
    }

    // 当たり判定メソッド
    // 当たり判定を行うには当たり判定を行うオブジェクト（スプライト）の矩形同士が重なっているかを判断し、
    // 重なっていれば当たっていると判断する
    private fun checkCollision() {
        // UFO(ゴールとの当たり判定)
        // SpriteクラスのboundingRectangleプロパティでスプライトの短形を表すRectangleを取得
        // (各オブジェクトのクラスはGameObjectクラスを継承しており、
        //  GameObjectクラスはSpriteクラスを継承しているのでboundingRectangleプロパティを取得することができる)
        // Rectangleクラスのoverlapsメソッドに当たり判定を行いたい相手のRectangleを指定する
        // 戻り値がtrueであれば重なっている＝当たっていることになる
        if (mPlayer.boundingRectangle.overlaps(mUfo.boundingRectangle)) {
            // UFOと当たった場合はゲームクリアとなるので状態をGAME_STATE_GAMEOVERにしてメソッドを抜ける
            mGameState = GAME_STATE_GAMEOVER
            return
        }

        // Enemyとの当たり判定
        for (i in 0 until mEnemys.size) {
            val enemy = mEnemys[i]
            if (mPlayer.boundingRectangle.overlaps(enemy.boundingRectangle)) {
                // 衝突音を出力
                sound.play(1.0f);
                // ゲームオーバー
                mGameState = GAME_STATE_GAMEOVER
                return
                }
                break
        }

        // Starとの当たり判定
        // 星との当たり判定は相手となるStarクラスのmStateがStar.STAR_NONEの場合はすでに当たって獲得済み
        // なので当たり判定を行わない
        for (i in 0 until mStars.size) {
            val star = mStars[i]

            if (star.mState == Star.STAR_NONE) {
                continue
            }

            if (mPlayer.boundingRectangle.overlaps(star.boundingRectangle)) {
                star.get()
                // 星に触れた時にmScoreに1足す
                mScore++
                // mHighScoreと比較し、現在のスコアの方が大きければmHighScoreに現在のスコアを代入する
                if (mScore > mHighScore) {
                    mHighScore = mScore
                    //ハイスコアをPreferenceに保存する
                    // putIntegerメソッドで第1引数にキー、第2引数に値を指定する
                    mPrefs.putInteger("HIGHSCORE", mHighScore)
                    // flushメソッドを呼び出すことで実際に値を永続化する
                    mPrefs.flush()
                }
                break
            }
        }

        // Stepとの当たり判定
        // 上昇中はStepとの当たり判定を確認しない
        if (mPlayer.velocity.y > 0) {
            return
        }

        for (i in 0 until mSteps.size) {
            val step = mSteps[i]

            if (step.mState == Step.STEP_STATE_VANISH) {
                continue
            }

            if (mPlayer.y > step.y) {
                if (mPlayer.boundingRectangle.overlaps(step.boundingRectangle)) {
                    mPlayer.hitStep()
                    if (mRandom.nextFloat() > 0.5f) {
                        // 踏み台と当たった場合はmRandom.nextFloat() > 0.5fで判断して、
                        // つまり1/2の確率で踏み台を消す
                        step.vanish()
                    }
                    break
                }
            }
        }
    }
}