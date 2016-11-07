package fi.kyy.nnracing.car;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;

public class Wheel {	

	public static final float PIXELS_PER_METER = 60.0f;
	
	public Car car;
	public float width;
	public float length;
	public boolean revolving;
	public boolean powered;
	public Body body;

	public Wheel(World world, Car car, float posX, float posY, float width, float length,
			boolean revolving, boolean powered) {
		super();
		this.car = car;
		this.width = width;
		this.length = length;
		this.revolving = revolving;
		this.powered = powered;
		
		// Create body 
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.position.set(car.body.getWorldPoint(new Vector2(posX, posY)));
		bodyDef.angle = car.body.getAngle();
		this.body = world.createBody(bodyDef);

		// Create shape
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 1.0f;
		fixtureDef.isSensor=true; // Wheel does not participate in collision calculations: resulting complications are unnecessary
		PolygonShape wheelShape = new PolygonShape();
		wheelShape.setAsBox(this.width/2, this.length/2);
		fixtureDef.shape = wheelShape;
		this.body.createFixture(fixtureDef);
		wheelShape.dispose();
		
	    // Create joint to connect wheel to body
	    if(this.revolving){
	    	RevoluteJointDef jointdef=new RevoluteJointDef();
	        jointdef.initialize(this.car.body, this.body, this.body.getWorldCenter());
	        jointdef.enableMotor=false; // We'll be controlling the wheel's angle manually
		    world.createJoint(jointdef);
	    }else{
	    	PrismaticJointDef jointdef=new PrismaticJointDef();
	        jointdef.initialize(this.car.body, this.body, this.body.getWorldCenter(), new Vector2(1, 0));
	        jointdef.enableLimit=true;
	        jointdef.lowerTranslation=jointdef.upperTranslation=0;
		    world.createJoint(jointdef);
	    }
	}
	
	public void setAngle (float angle){
		this.body.setTransform(body.getPosition(), this.car.body.getAngle() + (float) Math.toRadians(angle));
	};

	public Vector2 getLocalVelocity () {
	    return this.car.body.getLocalVector(this.car.body.getLinearVelocityFromLocalPoint(this.body.getPosition()));
	};

	public Vector2 getDirectionVector () {
		Vector2 directionVector;
		if (this.getLocalVelocity().y > 0)
			directionVector = new Vector2(0,1);
		else
			directionVector = new Vector2(0,-1);
			
		return directionVector.rotate((float) Math.toDegrees(this.body.getAngle()));	    
	};


	public Vector2 getKillVelocityVector (){
	    Vector2 velocity = this.body.getLinearVelocity();
	    Vector2 sidewaysAxis=this.getDirectionVector();
	    float dotprod = velocity.dot(sidewaysAxis);
	    return new Vector2(sidewaysAxis.x*dotprod, sidewaysAxis.y*dotprod);
	};

	public void killSidewaysVelocity (){
	    this.body.setLinearVelocity(this.getKillVelocityVector());
	};
}
