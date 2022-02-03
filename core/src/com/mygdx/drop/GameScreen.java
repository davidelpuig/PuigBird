package com.mygdx.drop;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

public class GameScreen implements Screen {

    final Bird game;

    Texture birdImage;
    Texture pipeUpImage;
    Texture pipeDownImage;
    Texture pauseImage;
    Texture backgroundImage;
    Sound flapSound;
    Sound failSound;
    OrthographicCamera camera;
    Rectangle player;
    Rectangle pause;
    Array<Rectangle> obstacles;
    long lastObstacleTime;
    float score;

    float speedy;
    float gravity;

    boolean paused;

    public GameScreen(final Bird gam) {
        this.game = gam;

        // load the images
        birdImage = new Texture(Gdx.files.internal("bird.png"));
        pipeUpImage = new Texture(Gdx.files.internal("pipe_up.png"));
        pipeDownImage = new Texture(Gdx.files.internal("pipe_down.png"));
        pauseImage = new Texture(Gdx.files.internal("pause.png"));
        backgroundImage = new Texture(Gdx.files.internal("background.png"));

        // load the drop sound effect and the rain background "music"
        flapSound = Gdx.audio.newSound(Gdx.files.internal("flap.wav"));
        failSound = Gdx.audio.newSound(Gdx.files.internal("fail.wav"));
        //rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));
        //rainMusic.setLooping(true);

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        // create a Rectangle to logically represent the player
        player = new Rectangle();
        player.x = 200; //800 / 2 - 64 / 2; // center the player vertically
        player.y = 480 / 2 - 64 / 2;
        // the bottom screen edge
        player.width = 64;
        player.height = 45;

        pause = new Rectangle();
        pause.x = 800 - 80;
        pause.y = 480 - 80;
        pause.width = 64;
        pause.height = 64;

        // create the obstacles array and spawn the first obstacle
        obstacles = new Array<Rectangle>();
        spawnObstacle();

        paused = false;

        speedy = 0;
        gravity = 850f;

        score = 0;

    }

    private void spawnObstacle() {

        // Calcula la alçada de l¡obstacle aleatoriament
        float holey = MathUtils.random(50, 230);

        // Crea dos obstacles: Una tuberoa superior i una inferior
        Rectangle pipe1 = new Rectangle();
        pipe1.x = 800;
        pipe1.y = holey - 230;
        pipe1.width = 64;
        pipe1.height = 230;
        obstacles.add(pipe1);

        Rectangle pipe2 = new Rectangle();
        pipe2.x = 800;
        pipe2.y = holey + 200;
        pipe2.width = 64;
        pipe2.height = 230;
        obstacles.add(pipe2);

        lastObstacleTime = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {

        boolean dead = false;

        // clear the screen with a color
        ScreenUtils.clear(0.3f, 0.8f, 0.8f, 1);

        // tell the camera to update its matrices.
        camera.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        game.batch.setProjectionMatrix(camera.combined);

        // begin a new batch and draw the player and
        // all obstacles
        game.batch.begin();
        game.batch.draw(backgroundImage, 0, 0);
        game.batch.draw(birdImage, player.x, player.y);

        // DIbuixa els obstacles: Els parells son tuberia inferior, els imparells tuberia superior
        for(int i = 0; i < obstacles.size; i++)
        {
                game.batch.draw( i % 2 == 0 ? pipeUpImage : pipeDownImage, obstacles.get(i).x, obstacles.get(i).y);
        }
        if(paused)
            game.font.draw(game.batch, "PAUSED", 400, 240);
        else
            game.batch.draw(pauseImage, pause.x, pause.y);
        game.font.draw(game.batch, "Score: " + (int)score, 10, 470);
        game.batch.end();

        // process user input
        if (Gdx.input.justTouched()) {
            if(!paused) {
                Vector3 touchPos = new Vector3();
                touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPos);
                // Pause game
                if (pause.contains(touchPos.x, touchPos.y))
                    paused = true;
                else {
                    speedy = 400f;
                    flapSound.play();
                }
            }
            else
            {
                paused = false;
            }
        }

        if(!paused)
        {
            //Actualitza la posició del jugador amb la velocitat vertical
            player.y += speedy * Gdx.graphics.getDeltaTime();

            //Actualitza la velocitat vertical amb la gravetat
            speedy -= gravity * Gdx.graphics.getDeltaTime();

            //La puntuació augmenta amb el temps de joc
            score += Gdx.graphics.getDeltaTime();

            /*if (Gdx.input.isKeyPressed(Keys.LEFT))
                player.x -= 200 * Gdx.graphics.getDeltaTime();
            if (Gdx.input.isKeyPressed(Keys.RIGHT))
                player.x += 200 * Gdx.graphics.getDeltaTime();
            */

            // Comprova que el jugador no es surt de la pantalla.
            // Si surt per la part inferior, game over
            if (player.y > 480 - 45)
                player.y = 480 - 45;
            if (player.y < 0 - 45) {
                dead = true;
            }

            // Comprova si cal generar un obstacle nou
            if (TimeUtils.nanoTime() - lastObstacleTime > 1500000000)
                spawnObstacle();

            // Mou els obstacles. Elimina els que estan fora de la pantalla
            // Comprova si el jugador colisiona amb un obstacle, llavors game over
            Iterator<Rectangle> iter = obstacles.iterator();
            while (iter.hasNext()) {
                Rectangle raindrop = iter.next();
                raindrop.x -= 200 * Gdx.graphics.getDeltaTime();
                if (raindrop.x  < -64)
                    iter.remove();
                if (raindrop.overlaps(player)) {
                    dead = true;
                }
            }

            if(dead)
            {
                failSound.play();
                game.lastScore = (int)score;
                if(game.lastScore > game.topScore)
                    game.topScore = game.lastScore;
                game.setScreen(new GameOverScreen(game));
                dispose();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
        // start the playback of the background music
        // when the screen is shown
        //rainMusic.play();
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        birdImage.dispose();
        pipeUpImage.dispose();
        pipeDownImage.dispose();
        failSound.dispose();
        flapSound.dispose();
    }

}