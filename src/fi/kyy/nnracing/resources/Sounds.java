package fi.kyy.nnracing.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class Sounds {
	
	public static Sound key;
	
	public static void load(){
		key = loadSound("key.ogg");
	}
	
    private static Sound loadSound (String filename) { 
        return Gdx.audio.newSound(Gdx.files.internal("data/" + filename));  
    }
      
    public static void play (Sound sound) {  
        sound.play(1);  
    }
    
    public static void dispose(){
    	key.dispose();
    }

}