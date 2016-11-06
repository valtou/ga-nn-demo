package fi.kyy.nnracing;

import com.badlogic.gdx.Game;

import fi.kyy.nnracing.screens.GameScreen;

public class NNRacingGame extends Game {

	GameScreen gameScreen;
	
	@Override
	public void create() {
        gameScreen = new GameScreen(this);
        
        gameScreen.onCreate();
        setScreen(gameScreen);
	}
}
