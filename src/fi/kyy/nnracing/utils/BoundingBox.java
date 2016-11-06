package fi.kyy.nnracing.utils;

import java.awt.Point;

import com.badlogic.gdx.math.Shape2D;
import com.badlogic.gdx.math.Vector2;

public interface BoundingBox {

    public Shape2D getShape();

    public void update();

    public boolean intersects(BoundingBox boundingBox);
    
    public boolean contains(Point p);

    public Vector2 getMaxVertice();

    public Vector2 getMinVertice();

}
