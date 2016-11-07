package fi.kyy.nnracing.screens;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import fi.kyy.nnracing.CameraHelper;
import fi.kyy.nnracing.NNRacingGame;
import fi.kyy.nnracing.car.Car;
import fi.kyy.nnracing.car.Status;
import fi.kyy.nnracing.car.Wheel;
import fi.kyy.nnracing.ga.GeneticAlgorithm;
import fi.kyy.nnracing.ga.Genome;
import fi.kyy.nnracing.resources.MapBodyBuilder;

public class GameScreen implements Screen, ContactListener {
	NNRacingGame game;
	private SpriteBatch spriteBatch;
	CameraHelper camera;
	CameraHelper graphCamera;
	public static World world;
	private World emptyWorld;
	private World shapeWorld;

	private Box2DDebugRenderer debugRenderer;
	public static ShapeRenderer shapeRenderer;
	public static ShapeRenderer graphShapeRenderer;

	private static final int VIRTUAL_WIDTH = (int)(1024 *1.5f);
	private static final int VIRTUAL_HEIGHT = (int)(768 *1.5f);

	private float worldWidth;
	private float worldHeight;
	private static int PIXELS_PER_METER = 16;

	private static final int POPULATION_SIZE = 15;
	private static final float TTL = 30000; // 30 seconds TTL

	// Modify deltaTime inside render to go through generations and simulations faster
	private final float deltaTime = 1f / 120f * 1000;
	private double currentTime = System.currentTimeMillis();
	private float accumulator = 0.0f;
	private boolean populationAlive = false;
	private int done;

	private GeneticAlgorithm geneticAlgorithm;
	private final ArrayList<Car> activePopulation = new ArrayList<Car>();
	private float generationTimer = 0;
	
	private Array<Float> averageFitnesses = new Array<Float>();
	private Array<Float> bestFitnesses = new Array<Float>();
	private Array<Float> motherFitnesses = new Array<Float>();

	private BitmapFont font;
	private float average = 0;
	
	float[] obstacleVertices;
	
	public GameScreen(NNRacingGame RacerGame) {
		this.game = RacerGame;
	}

	public void onCreate() {
		geneticAlgorithm = new GeneticAlgorithm(POPULATION_SIZE, 645);

		world = new World(new Vector2(0.0f, 0.0f), true);
		world.setContactListener(this);
		emptyWorld = new World(new Vector2(0.0f, 0.0f), true);
		shapeWorld = new World(new Vector2(0.0f, 0.0f), true);
		
		camera = new CameraHelper(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
		graphCamera = new CameraHelper(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

		worldWidth = camera.getViewportWidth() / PIXELS_PER_METER;
		worldHeight = camera.getViewportHeight() / PIXELS_PER_METER;

		debugRenderer = new Box2DDebugRenderer();
		shapeRenderer = new ShapeRenderer();
		graphShapeRenderer = new ShapeRenderer();
		
		spriteBatch = new SpriteBatch();
		font = new BitmapFont();
		font.setColor(Color.GOLD);
		font.getData().setScale(.1f);

		MapBodyBuilder.buildShapes(new TmxMapLoader().load("testi.tmx"), 32, world);
		MapBodyBuilder.buildShapes(new TmxMapLoader().load("testi.tmx"), 32, shapeWorld);
		MapBodyBuilder.buildBounds(new TmxMapLoader().load("testi.tmx"), 32, world);
		MapBodyBuilder.buildCheckpoints(new TmxMapLoader().load("testi.tmx"), 32, world);

	}
	
	@Override
	public void resume() {
	}

	@Override
	public void show() {
	}

	@Override
	public void dispose() {
		spriteBatch.dispose();
	}

	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shapeRenderer.setProjectionMatrix(camera.getCombined());
		graphShapeRenderer.setProjectionMatrix(graphCamera.getCombined());
		spriteBatch.setProjectionMatrix(graphCamera.getCombined());

		float millisecondsDelta = delta * 1000.0f;

		if (!populationAlive) {
			generateNewPopulation();
		}

		if (averageFitnesses.size > 1) {
			for (int i = 1; i < averageFitnesses.size; i++) {
				graphShapeRenderer.begin(ShapeType.Line);
				graphShapeRenderer.setColor(Color.YELLOW);
				graphShapeRenderer.line(new Vector2(i - 1, averageFitnesses.get(i - 1) / 300), 
						new Vector2(i, averageFitnesses.get(i) / 300));
				graphShapeRenderer.end();
			}
		} else if (averageFitnesses.size == 1) {
			graphShapeRenderer.begin(ShapeType.Point);
			graphShapeRenderer.setColor(Color.YELLOW);
			graphShapeRenderer.point(geneticAlgorithm.getGeneration() - 1, averageFitnesses.get(0) / 300, 0);
			graphShapeRenderer.end();
		}
		if (motherFitnesses.size > 1) {
			for (int i = 1; i < motherFitnesses.size; i++) {
				graphShapeRenderer.begin(ShapeType.Line);
				graphShapeRenderer.setColor(Color.BROWN);
				graphShapeRenderer.line(new Vector2(i - 1, motherFitnesses.get(i - 1) / 300), 
						new Vector2(i, motherFitnesses.get(i) / 300));
				graphShapeRenderer.end();
			}
		} else if (motherFitnesses.size == 1) {
			graphShapeRenderer.begin(ShapeType.Point);
			graphShapeRenderer.setColor(Color.BROWN);
			graphShapeRenderer.point(geneticAlgorithm.getGeneration() - 1, motherFitnesses.get(0) / 300, 0);
			graphShapeRenderer.end();
		}
		if (bestFitnesses.size > 1) {
			for (int i = 1; i < bestFitnesses.size; i++) {
				graphShapeRenderer.begin(ShapeType.Line);
				graphShapeRenderer.setColor(Color.RED);
				graphShapeRenderer.line(new Vector2(i - 1, bestFitnesses.get(i - 1) / 300), 
						new Vector2(i, bestFitnesses.get(i) / 300));
				graphShapeRenderer.end();
			}
		} else if (bestFitnesses.size == 1) {
			graphShapeRenderer.begin(ShapeType.Point);
			graphShapeRenderer.setColor(Color.RED);
			graphShapeRenderer.point(geneticAlgorithm.getGeneration() - 1, bestFitnesses.get(0) / 300, 0);
			graphShapeRenderer.end();
		}
		
		double newTime = System.currentTimeMillis();
		double frameTime = newTime - currentTime;
		currentTime = newTime;
		accumulator += frameTime;

		if (populationAlive) {
			generationTimer += millisecondsDelta;
			if (done >= POPULATION_SIZE || generationTimer > TTL) {
				done = 0;
				generationTimer = 0;
				for (Car car : activePopulation) {
					
					// TODO Modify this to include distance/time
					// Currently the best fitness on this shitty track is about 1980 and >1776 when a car gets to finish
					// float fitness = car.getCheckPointCount() * (1-car.getCenterCount())*50 + car.travelledDistance / 10 + TimeUnit.MILLISECONDS.toSeconds(car.getTimeBeforeCrash()) * 5;
					
					float fitness = car.getCheckPointCount() * (1-car.getCenterCount())*100 + car.travelledDistance / 5;
					car.getGenome().setFitness(fitness);
					car.update(Gdx.app.getGraphics().getDeltaTime());
					car.remove();

				}

				System.out.println(geneticAlgorithm.getFittest());
				bestFitnesses.add(geneticAlgorithm.getFittest().getFitness());
				motherFitnesses.add(geneticAlgorithm.getMother().getFitness());
				geneticAlgorithm.breed();
				average = 0;
				for (Car c : activePopulation) {
					average += c.getGenome().getFitness();
					removeCar(c);
				}
				average = average / activePopulation.size();
				averageFitnesses.add(average);
				activePopulation.clear();
				populationAlive = false;
			} else {
				for (Car car : activePopulation) {
					if (car.isStuck() && car.getStatus() != Status.DONE) {
						car.setStatus(Status.DONE);
						car.setTimeToStuck(generationTimer);
						this.done++;
					} else {
						car.update(Gdx.app.getGraphics().getDeltaTime());
					}
				}
				
				// Update camera position according to the position of the father
				camera.update(activePopulation.get(0).body.getPosition().x * PIXELS_PER_METER,
						activePopulation.get(0).body.getPosition().y * PIXELS_PER_METER);
			}
		}
		
		graphCamera.update(graphCamera.getViewportWidth() / 2,
				graphCamera.getViewportHeight() / 2);
		
		world.step(Gdx.app.getGraphics().getDeltaTime(), 3, 3);
		world.clearForces();
		
		debugRenderer.render(emptyWorld, graphCamera.getCombined().scale(PIXELS_PER_METER, PIXELS_PER_METER, PIXELS_PER_METER));
		debugRenderer.render(shapeWorld, camera.getCombined().scale(PIXELS_PER_METER, PIXELS_PER_METER, PIXELS_PER_METER));

	}

	private void generateNewPopulation() {
		ArrayList<Genome> population = geneticAlgorithm.getPopulation();
		this.activePopulation.clear();
		this.done = 0;
		int i = 0;
		for (Genome genome : population) {
			genome.setFitness(0);
			Car c = new Car(world, 1, 2, new Vector2(7f, 5f), (float) Math.PI, 60, 15, 20, 70, i, genome);
			i++;
			this.activePopulation.add(c);
		}
		System.out.println("New generation " + geneticAlgorithm.getGeneration() + " " + population.size() + "  " + done);
		populationAlive = true;
	}
	
	public static void removeCar(final Car car) {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				boolean destroyed = false;
				while (!destroyed) {
					if (!world.isLocked()) {
						final Array<JointEdge> list = car.body.getJointList();
						while (list.size > 0) {
							world.destroyJoint(list.get(0).joint);
						}
						world.destroyBody(car.body);
						for (Wheel w : car.wheels) {
							world.destroyBody(w.body);
						}
						destroyed = true;
					}
				}
			}
		});

	}

	public static void setCarStatic(final Body body) {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				if (!world.isLocked()) {
					body.setType(BodyType.StaticBody);
					for(Fixture f : body.getFixtureList()) {
						f.setSensor(true);
					}
				}
			}
		});
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void beginContact(Contact contact) {
		Body a = contact.getFixtureA().getBody();
		Body b = contact.getFixtureB().getBody();

		if (a.getUserData() == null || b.getUserData() == null) {
			return;
		}

		for (Car c : activePopulation) {
			if (a.getUserData().equals("Obstacle") && b.getUserData().equals("Car" + c.getId())) {
				// CAR HITS WALL
				c.setTimeBeforeCrash(System.currentTimeMillis() - c.getTimeBeforeCrash());
				setCarStatic(b);
				done++;
				
			}
			if (b.getUserData().equals("Obstacle") && a.getUserData().equals("Car" + c.getId())) {
				// CAR HITS WALL
				c.setTimeBeforeCrash(System.currentTimeMillis() - c.getTimeBeforeCrash());
				setCarStatic(a);
				done++;
			}

			if (a.getUserData().equals("Checkpoint") && b.getUserData().equals("Car" + c.getId())) {
				// CAR HITS CHECKPOINT
					c.setCheckPointCount(c.getCheckPointCount() + 1);
					c.setCenterCount(1 / a.getWorldCenter().dst(b.getWorldCenter()));
			}
			if (b.getUserData().equals("Checkpoint") && a.getUserData().equals("Car" + c.getId())) {
				// CAR HITS CHECKPOINT
					c.setCheckPointCount(c.getCheckPointCount() + 1);
					c.setCenterCount(1 / b.getWorldCenter().dst(a.getWorldCenter()));
			}
		}

	}

	@Override
	public void endContact(Contact contact) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub

	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		// TODO Auto-generated method stub

	}

}
