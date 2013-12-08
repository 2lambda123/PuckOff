package com.ladinc.puckoff.core.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.ladinc.puckoff.core.PuckOff;
import com.ladinc.puckoff.core.ai.SimpleAi;
import com.ladinc.puckoff.core.controls.IControls;
import com.ladinc.puckoff.core.objects.HockeyPlayer;
import com.ladinc.puckoff.core.objects.Puck;
import com.ladinc.puckoff.core.objects.Rink;
import com.ladinc.puckoff.core.utilities.DebugUtilities;
import com.ladinc.puckoff.core.utilities.GenericEnums.Side;

public class HockeyScreen implements Screen 
{
	private OrthographicCamera camera;
    private SpriteBatch spriteBatch;

    private World world;
    
    private Box2DDebugRenderer debugRenderer;
    
    //Used for sprites etc
	private int screenWidth;
    private int screenHeight;
    
    //Used for Box2D
    private float worldWidth;
    private float worldHeight;
    private static int PIXELS_PER_METER = 10;  
    
    private Vector2 center;
    
    private PuckOff game;
    
    //Game Actor Objects
    public List<HockeyPlayer> hockeyPlayerList;
    private Rink rink;
    private Puck puck;
    private List<SimpleAi> AiList;
    
    public HockeyScreen(PuckOff game)
    {
    	this.game = game;
    	
    	this.screenWidth = this.game.screenWidth;
    	this.screenHeight = this.game.screenHeight;
    	
    	this.worldHeight = this.screenHeight / PIXELS_PER_METER;
    	this.worldWidth = this.screenWidth / PIXELS_PER_METER;
    	
    	this.center = new Vector2(worldWidth / 2, worldHeight / 2);
    	
    	this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, this.screenWidth, this.screenHeight);
        
        this.debugRenderer = new Box2DDebugRenderer();
    }
    
    private float aiCoolDown = 0;
    
    //This is the main game loop, it is repeatedly called
	@Override
	public void render(float delta) 
	{
		camera.update();
		spriteBatch.setProjectionMatrix(camera.combined);
		
		if(aiCoolDown > 0)
		{
			aiCoolDown -= delta;
		}
		
		if(Gdx.input.isKeyPressed(Input.Keys.NUM_0) && aiCoolDown <= 0)
		{
			aiCoolDown = 0.5f;
			createAIPlayer(Side.Away);
		}
		
		if(Gdx.input.isKeyPressed(Input.Keys.NUM_9) && aiCoolDown <= 0)
		{
			aiCoolDown = 0.5f;
			createAIPlayer(Side.Home);
		}
		
		if(this.game.controllerManager.checkForNewControllers())
		{
			createPlayers();
		}
		
		world.step(Gdx.app.getGraphics().getDeltaTime(), 3, 3);
        world.clearForces();
        
        for(SimpleAi ai: AiList)
        {
        	ai.movementFollowPuck(ai.player.body.getWorldCenter(), this.puck.body.getWorldCenter());
        }
        
        for(HockeyPlayer hp: hockeyPlayerList)
		{
        	hp.updateMovement(delta);
		}
        
        this.puck.update();
        
        
        world.step(Gdx.app.getGraphics().getDeltaTime(), 100, 100);
        //world.clearForces();
        //world.step(1/60f, 3, 3);
        world.clearForces();
		this.spriteBatch.begin();
		
		renderSprites(this.spriteBatch);
		
		this.spriteBatch.end();
		
		
		
		debugRenderer.render(world, camera.combined.scale(PIXELS_PER_METER,PIXELS_PER_METER,PIXELS_PER_METER));
		
		
		
	}
	
	//Sprites are drawn in order, so something drawn first will be bottom of the pile so to speak i.e. draw background first
	private void renderSprites(SpriteBatch spriteBatch)
    {
		if(Gdx.app.getLogLevel() == Application.LOG_DEBUG)
			DebugUtilities.renderFPSCounter(spriteBatch, this.camera);
		
		//Sets the background colour of the canvas
		Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        
        this.rink.rinkImage.draw(spriteBatch);
        
        for(HockeyPlayer hp: hockeyPlayerList)
		{
        	hp.updateSprite(spriteBatch);
		}
        
        this.puck.updateSprite(spriteBatch);

    }
	
	private void createAIPlayer(Side side)
	{
		if(AiList == null)
		{
			AiList = new ArrayList<SimpleAi>();
		}
		
		SimpleAi ai = new SimpleAi();
		AiList.add(ai);
		
		int nextPlayerNumber = this.hockeyPlayerList.size() + 1;
		
		HockeyPlayer hp = new HockeyPlayer(world, nextPlayerNumber, side, ai, this.rink.getPlayerStartingPosition(side, nextPlayerNumber), this.camera);
		hockeyPlayerList.add(hp);
		
		ai.player = hp;
		
	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	//Show gets called when the screen is first navigated to
	@Override
	public void show() {
		world = new World(new Vector2(0.0f, 0.0f), true);
		
		this.rink = new Rink(world, this.worldHeight, this.worldWidth, this.center);
		
		spriteBatch = new SpriteBatch();
		
		createPlayers();
		this.puck = new Puck(world, this.rink.getPuckStartingPoint());
		
		if(AiList == null)
		{
			AiList = new ArrayList<SimpleAi>();
		}
	}
	
	private Side lastUsedSide = Side.Away;
	
	private void createPlayers()
	{
		Gdx.app.debug("Hockey Screen", "createPlayers()");
		
		if(hockeyPlayerList == null)
		{
			hockeyPlayerList= new ArrayList<HockeyPlayer>();		
		}
		
		int nextPlayerNumber = this.hockeyPlayerList.size() + 1;
		
		for(IControls ic : this.game.controllerManager.controls)
		{
			Gdx.app.debug("Hockey Screen", "createPlayers() - looping through controllers");
			
			boolean controllerHasPlayer = false;
			for(HockeyPlayer hp: hockeyPlayerList)
			{
				Gdx.app.debug("Hockey Screen", "createPlayers() - checking to see is controller already assigned");
				 if(hp.controller == ic)
					 controllerHasPlayer = true;
				 
			 }
			
			if(!controllerHasPlayer)
			{
				if(lastUsedSide == Side.Away)
					lastUsedSide = Side.Home;
				else
					lastUsedSide = Side.Away;
				
				Gdx.app.debug("Hockey Screen", "createPlayers() - creating new player");
				hockeyPlayerList.add(new HockeyPlayer(world, nextPlayerNumber, lastUsedSide, ic, this.rink.getPlayerStartingPosition(lastUsedSide, nextPlayerNumber), this.camera));
				nextPlayerNumber++;
			}
		}
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}
