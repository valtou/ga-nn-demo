package fi.kyy.nnracing.car;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import fi.kyy.nnracing.ga.Genome;
import fi.kyy.nnracing.ga.GenomeAware;
import fi.kyy.nnracing.neuralnet.NeuralNetwork;
import fi.kyy.nnracing.screens.GameScreen;
import fi.kyy.nnracing.utils.Input;

public class Car implements GenomeAware {

	protected EntityState previousState = new EntityState();
	protected EntityState currentState = new EntityState();
	protected Vector2 renderPosition = new Vector2(0, 0);

	private Input input = new Input();

	private final NeuralNetwork network = new NeuralNetwork(7, 4, 1, 40);
	private Genome genome;

	public Body body;
	float width, length, angle, maxSteerAngle, minSteerAngle, maxSpeed, power;
	float wheelAngle;
	private int steer, accelerate;
	public List<Wheel> wheels;

	public static final int STEER_NONE = 0;
	public static final int STEER_LEFT = 1;
	public static final int STEER_RIGHT = 2;
	public static final int STEER_HARD_LEFT = 3;
	public static final int STEER_HARD_RIGHT = 4;

	public static final int ACC_NONE = 0;
	public static final int ACC_ACCELERATE = 1;
	public static final int ACC_BRAKE = 2;

	protected Vector2 maxAcceleration = new Vector2(0.04f, 0.04f);

	protected Vector2 speed = new Vector2(0, 0);
	protected Vector2 acceleration = new Vector2(0, 0);

	protected Vector2 normal = new Vector2();

	protected float alpha = 1f;

	protected boolean hasFriction = true;

	protected EntityType type;
	protected Car parent;

	protected BoundingBox boundingBox;

	public boolean stuck;
	private float timer;

	private final Integer DISTANCE_UPDATE = 100;
	private float distanceTimer;
	private double oldDistance;

	private int id;
	private int checkPointCount = 0;
	private long timeBeforeCrash = 0;
	private float centerCount = 0;

	public float travelledDistance = 0;

	private Vector2 lastPosition = new Vector2();
	private Vector2 startPosition;

	// Ray distances to track walls, these are the inputs for the neural network
	private float d1, d2, d3, d4, d5, d6, d7;
	private int dCount;

	private ArrayList<Vector2> hitList1 = new ArrayList<Vector2>(), hitList2 = new ArrayList<Vector2>(),
			hitList3 = new ArrayList<Vector2>(), hitList4 = new ArrayList<Vector2>(),
			hitList5 = new ArrayList<Vector2>(), hitList6 = new ArrayList<Vector2>(),
			hitList7 = new ArrayList<Vector2>();

	private Array<Vector2> raysToDraw = new Array<Vector2>();

	private int counter;

	public Car(World world, float width, float length, Vector2 position, float angle, float power, float minSteerAngle,
			float maxSteerAngle, float maxSpeed, int id, Genome genome) {
		super();
		dCount = 0;
		this.steer = STEER_NONE;
		this.accelerate = ACC_NONE;
		this.counter = 0;
		this.genome = genome;
		this.lastPosition = position;
		this.width = width;
		this.length = length;
		this.angle = angle;
		this.maxSteerAngle = maxSteerAngle;
		this.minSteerAngle = minSteerAngle;
		this.maxSpeed = maxSpeed;
		this.power = power;
		this.wheelAngle = 0;
		this.id = id;
		this.checkPointCount = 0;
		this.startPosition = position;
		this.centerCount = 0;
		this.timeBeforeCrash = System.currentTimeMillis();

		// Create body
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.position.set(position);
		bodyDef.angle = angle;
		this.body = world.createBody(bodyDef);
		this.body.setUserData("Car" + id);
		this.stuck = false;
		travelledDistance = 0;

		// Create shape
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 1.0f;
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.2f;
		PolygonShape carShape = new PolygonShape();
		carShape.setAsBox(this.width / 2, this.length / 2);
		fixtureDef.shape = carShape;
		fixtureDef.filter.groupIndex = -1;
		this.body.createFixture(fixtureDef);

		// Create wheels
		this.wheels = new ArrayList<Wheel>();
		this.wheels.add(new Wheel(world, this, -0.5f, -0.6f, 0.2f, 0.4f, true, true)); // top
																						// left
		this.wheels.add(new Wheel(world, this, 0.5f, -0.6f, 0.2f, 0.4f, true, true)); // top
																						// right
		this.wheels.add(new Wheel(world, this, -0.5f, 0.6f, 0.2f, 0.4f, false, false)); // back
																						// left
		this.wheels.add(new Wheel(world, this, 0.5f, 0.6f, 0.2f, 0.4f, false, false)); // back
																						// right
		this.network.importWeights(genome.getGenes());

	}

	public List<Wheel> getPoweredWheels() {
		List<Wheel> poweredWheels = new ArrayList<Wheel>();
		for (Wheel wheel : this.wheels) {
			if (wheel.powered)
				poweredWheels.add(wheel);
		}
		return poweredWheels;
	}

	public Vector2 getLocalVelocity() {
		return this.body.getLocalVector(this.body.getLinearVelocityFromLocalPoint(new Vector2(0, 0)));
	}

	public List<Wheel> getRevolvingWheels() {
		List<Wheel> revolvingWheels = new ArrayList<Wheel>();
		for (Wheel wheel : this.wheels) {
			if (wheel.revolving)
				revolvingWheels.add(wheel);
		}
		return revolvingWheels;
	}

	public float getSpeedKMH() {
		Vector2 velocity = this.body.getLinearVelocity();
		float len = velocity.len();
		return (len / 1000) * 3600;
	}

	public void setSpeed(float speed) {
		/*
		 * speed - speed in kilometers per hour
		 */
		Vector2 velocity = this.body.getLinearVelocity();
		velocity = velocity.nor();
		velocity = new Vector2(velocity.x * ((speed * 1000.0f) / 3600.0f), velocity.y * ((speed * 1000.0f) / 3600.0f));
		this.body.setLinearVelocity(velocity);
	}

	public void setSteer(int value) {
		this.steer = value;
	}

	public void setAccelerate(int value) {
		this.accelerate = value;
	}

	public void update(float deltaTime) {
		// 1. KILL SIDEWAYS VELOCITY
		GameScreen.shapeRenderer.begin(ShapeType.Filled);
		GameScreen.shapeRenderer.setColor(Color.WHITE);
		for (Wheel wheel : wheels) {
			wheel.killSidewaysVelocity();
			GameScreen.shapeRenderer.rect(wheel.body.getWorldCenter().x - wheel.width / 2,
					wheel.body.getWorldCenter().y - wheel.length / 2, wheel.width / 2, wheel.length / 2, wheel.width,
					wheel.length, 1, 1, (float) Math.toDegrees(wheel.body.getAngle()));
		}
		GameScreen.shapeRenderer.end();

		GameScreen.shapeRenderer.begin(ShapeType.Line);
		GameScreen.shapeRenderer.setColor(Color.WHITE);
		GameScreen.shapeRenderer.rect(this.body.getWorldCenter().x - this.width / 2,
				this.body.getWorldCenter().y - this.length / 2, this.width / 2, this.length / 2, this.width,
				this.length, 1, 1, (float) Math.toDegrees(this.body.getAngle()));
		GameScreen.shapeRenderer.end();

		// 2. SET WHEEL ANGLE
		float incr = (this.maxSteerAngle) * deltaTime * 5;

		if (this.steer == STEER_LEFT) {
			this.wheelAngle = Math.min(Math.max(this.wheelAngle, 0) + incr, this.minSteerAngle); // increment
																									// angle
																									// without
																									// going
																									// over
																									// max
																									// steer
			// Gdx.app.log("Steer", "left");
		} else if (this.steer == STEER_RIGHT) {
			this.wheelAngle = Math.max(Math.min(this.wheelAngle, 0) - incr, -this.minSteerAngle); // decrement
																									// angle
																									// without
																									// going
																									// over
																									// max
																									// steer
			// Gdx.app.log("Steer", "right");
		} else if (this.steer == STEER_HARD_LEFT) {
			this.wheelAngle = Math.max(Math.min(this.wheelAngle, 0) + incr, this.maxSteerAngle);
			// Gdx.app.log("Steer", "Hard left");
		} else if (this.steer == STEER_HARD_RIGHT) {
			this.wheelAngle = Math.max(Math.min(this.wheelAngle, 0) - incr, -this.maxSteerAngle);
			// Gdx.app.log("Steer", "Hard right");
		} else {
			this.wheelAngle = 0;
		}

		// Update revolving wheels
		for (Wheel wheel : this.getRevolvingWheels()) {
			wheel.setAngle(this.wheelAngle);
		}

		// 3. APPLY FORCE TO WHEELS
		Vector2 baseVector; // Vector pointing in the direction force will be
							// applied to a wheel ; relative to the wheel.

		// If accelerator is pressed down and speed limit has not been reached,
		// go forwards.
		if ((this.accelerate == ACC_ACCELERATE) && (this.getSpeedKMH() < this.maxSpeed)) {
			baseVector = new Vector2(0, -1);
		} else if (this.accelerate == ACC_BRAKE) {
			// Braking, but still moving forwards - increased force
			if (this.getLocalVelocity().y < 0)
				baseVector = new Vector2(0f, 1.3f);
			// Going in reverse - less force
			else
				baseVector = new Vector2(0f, 0f);
		} else if (this.accelerate == ACC_NONE) {
			baseVector = new Vector2(0, 0);
			if (this.getSpeedKMH() < 7)
				this.setSpeed(0);
			else if (this.getLocalVelocity().y < 0)
				baseVector = new Vector2(0, 0.7f);
			else if (this.getLocalVelocity().y > 0)
				baseVector = new Vector2(0, -0.7f);
		} else
			baseVector = new Vector2(0, 0);

		// Multiply by engine power, which gives us a force vector relative to
		// the wheel
		Vector2 forceVector = new Vector2(this.power * baseVector.x, this.power * baseVector.y);

		// Apply force to each wheel
		for (Wheel wheel : this.getPoweredWheels()) {
			Vector2 position = wheel.body.getWorldCenter();
			wheel.body.applyForce(wheel.body.getWorldVector(new Vector2(forceVector.x, forceVector.y)), position, true);
		}

		// If going very slow, stop - to prevent endless sliding
		Vector2 point = this.body.getWorldCenter();
		Vector2 direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle())),
				(float) (point.y + 8 * Math.sin(this.body.getAngle())));
		direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-30))),
				(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-30))));

		// System.out.println("CENTERPOINT: " + point.x + ", " + point.y);
		// System.out.println("DIRECTION: " + direction.x + ", " + direction.y);

		if (counter == 10) {
			travelledDistance += Math.abs(point.dst(lastPosition));
			lastPosition = point;
			counter = 0;
		}
		counter++;

		// TODO Fix this massive block of shit code, create better and general
		// implementation of ray casting
		if (!this.body.getType().equals(BodyType.StaticBody)) {
			GameScreen.world.rayCast(new RayCastCallback() {
				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList1.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList1) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d1 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);
			direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-55))),
					(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-55))));
			GameScreen.world.rayCast(new RayCastCallback() {

				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList2.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList2) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d2 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);
			direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-75))),
					(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-75))));
			GameScreen.world.rayCast(new RayCastCallback() {

				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList3.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList3) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d3 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);
			direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-90))),
					(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-90))));
			GameScreen.world.rayCast(new RayCastCallback() {

				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList4.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList4) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d4 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);
			direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-105))),
					(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-105))));
			GameScreen.world.rayCast(new RayCastCallback() {
				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList5.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList5) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d5 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);

			direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-125))),
					(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-125))));
			GameScreen.world.rayCast(new RayCastCallback() {

				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList6.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList6) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d6 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);
			direction = new Vector2((float) (point.x + 8 * Math.cos(this.body.getAngle() + Math.toRadians(-150))),
					(float) (point.y + 8 * Math.sin(this.body.getAngle() + Math.toRadians(-150))));
			GameScreen.world.rayCast(new RayCastCallback() {

				@Override
				public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
					if (fixture.isSensor() || fixture.getBody().getUserData().equals("Car")) {
						return -1;
					}
					if (fixture.getBody().getUserData().equals("Bound")) {
						return 1;
					}
					hitList7.add(point);
					Vector2 apu = new Vector2();
					float distance2 = Math.abs(Car.this.body.getWorldCenter().dst(point));

					for (Vector2 o : hitList7) {
						if (Math.abs(Car.this.body.getWorldCenter().dst(o)) < distance2) {
							apu = o;
						} else {
							apu = point;
						}
					}
					Car.this.normal.set(normal).add(apu);
					GameScreen.shapeRenderer.begin(ShapeType.Line);
					GameScreen.shapeRenderer.setColor(Color.BLUE);
					GameScreen.shapeRenderer.line(Car.this.body.getWorldCenter(), apu);
					GameScreen.shapeRenderer.line(apu, Car.this.normal);
					GameScreen.shapeRenderer.end();
					d7 = Car.this.body.getWorldCenter().dst(apu);
					raysToDraw.add(apu);
					dCount++;
					return 1;
				}

			}, point, direction);
		}

		if (dCount >= 4) {
			dCount = 0;

			// Input the sensor information to the network
			List<Float> outputs = network.update(d1, d2, d3, d4, d5, d6, d7);

			// Handle the car according to the neural network outputs
			input = new Input(outputs);

			setAccelerate(ACC_ACCELERATE);
			if (input.up <= input.down && getSpeedKMH() >= 40) {
				setAccelerate(ACC_BRAKE);
			} else {
				setAccelerate(ACC_ACCELERATE);
			}
			if (input.left > input.right) {
				setSteer(STEER_LEFT);
			} else {
				setSteer(STEER_RIGHT);
			}
			hitList1.clear();
			hitList2.clear();
			hitList3.clear();
			hitList4.clear();
			hitList5.clear();
			hitList6.clear();
			hitList7.clear();
			
			d1 = 0;
			d2 = 0;
			d3 = 0;
			d4 = 0;
			d5 = 0;
			d6 = 0;
			d7 = 0;

		}

		distanceTimer += deltaTime;
		if (distanceTimer > DISTANCE_UPDATE) {
			distanceTimer = 0;
			double newDistance = travelledDistance;

			if (newDistance < oldDistance) {
				stuck = true;
			} else {
				oldDistance = newDistance;
			}
		}

	}

	public boolean blocksLineOfSight() {
		return false;
	}

	public boolean isStuck() {
		return stuck;
	}

	@Override
	public Genome getGenome() {
		return genome;
	}

	public Vector2 getSpeed() {
		return speed;
	}

	public void setSpeed(Vector2 speed) {
		this.speed = speed;
	}

	public Vector2 getAcceleration() {
		return acceleration;
	}

	public void setAcceleration(Vector2 acceleration) {
		this.acceleration = acceleration;
	}

	public Vector2 getPosition() {
		return currentState.position;
	}

	public void setPosition(Vector2 position) {
		this.currentState.position = position;
	}

	public Vector2 getMaxAcceleration() {
		return maxAcceleration;
	}

	public void setMaxAcceleration(Vector2 maxAcceleration) {
		this.maxAcceleration = maxAcceleration;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public float getWidth() {
		return currentState.width;
	}

	public void setWidth(float width) {
		this.currentState.width = width;
	}

	public float getHeight() {
		return currentState.height;
	}

	public void setHeight(float height) {
		this.currentState.height = height;
	}

	public EntityType getType() {
		return type;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}

	public double getRotation() {
		return currentState.rotation;
	}

	public void setRotation(double rotation) {
		this.currentState.rotation = rotation;
	}

	public boolean isHasFriction() {
		return hasFriction;
	}

	public void setHasFriction(boolean hasFriction) {
		this.hasFriction = hasFriction;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public Car getParent() {
		return parent;
	}

	public void setParent(Car parent) {
		this.parent = parent;
	}

	public Vector2 getRenderPosition() {
		return renderPosition;
	}

	public void setRenderPosition(Vector2 renderPosition) {
		this.renderPosition = renderPosition;
	}

	public Status getStatus() {
		return this.currentState.status;
	}

	public void setStatus(Status status) {
		this.currentState.status = status;
	}

	public EntityState getPreviousState() {
		return previousState;
	}

	public void setPreviousState(EntityState previousState) {
		this.previousState = previousState;
	}

	public EntityState getCurrentState() {
		return currentState;
	}

	public void setCurrentState(EntityState currentState) {
		this.currentState = currentState;
	}

	public void remove() {
		this.setStatus(Status.INACTIVE);
	}

	public void setTimeToStuck(float timer) {
		this.timer = timer;

	}

	public float getTimer() {
		return timer;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCheckPointCount() {
		return checkPointCount;
	}

	public void setCheckPointCount(int checkPointCount) {
		this.checkPointCount = checkPointCount;
	}

	public Vector2 getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(Vector2 startPosition) {
		this.startPosition = startPosition;
	}

	public float getCenterCount() {
		return centerCount;
	}

	public void setCenterCount(float centerCount) {
		this.centerCount = centerCount;
	}

	public long getTimeBeforeCrash() {
		return timeBeforeCrash;
	}

	public void setTimeBeforeCrash(long timeBeforeCrash) {
		this.timeBeforeCrash = timeBeforeCrash;
	}

}
