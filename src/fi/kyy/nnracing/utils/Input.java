package fi.kyy.nnracing.utils;

import java.io.Serializable;
import java.util.List;

import com.badlogic.gdx.Input.Buttons;

public class Input implements Serializable {
	private static final long serialVersionUID = 4618180701316379072L;

	private boolean leftKeyDown;
	private boolean rightKeyDown;
	private boolean upKeyDown;
	private boolean downKeyDown;

	private boolean isLeftButtonPressed;
	private boolean isRightButtonPressed;
	
	public float left, right, up, down;

	public Input() {
	}

	public Input(com.badlogic.gdx.Input input) {
		if (input != null) {
			leftKeyDown = input.isKeyPressed(com.badlogic.gdx.Input.Keys.DPAD_LEFT);
			rightKeyDown = input.isKeyPressed(com.badlogic.gdx.Input.Keys.DPAD_RIGHT);
			upKeyDown = input.isKeyPressed(com.badlogic.gdx.Input.Keys.DPAD_UP);
			downKeyDown = input.isKeyPressed(com.badlogic.gdx.Input.Keys.DPAD_DOWN);

			if (input.isButtonPressed(Buttons.LEFT)) {
				isLeftButtonPressed = true;
			}
			if (input.isButtonPressed(Buttons.RIGHT)) {
				isRightButtonPressed = true;
			}
		}
	}

	public Input(List<Float> inputs) {
		float up = inputs.get(3);
		float down = inputs.get(2);
		float left = inputs.get(1);
		float right = inputs.get(0);
		this.up = up;
		this.down = down;
		this.left = left;
		this.right = right;
		
		if (up > 0.5f) {
			upKeyDown = true;
		} else {
			upKeyDown = false;
		}

		if (down > 0.5f) {
			downKeyDown = true;
		} else {
			downKeyDown = false;
		}
		if (left > 0.5f) {
			leftKeyDown = true;
		} else {
			leftKeyDown = false;
		}
		if (right > 0.5f) {
			rightKeyDown = true;
		} else {
			rightKeyDown = false;
		}
		
	}
	
	public static double scale(final double valueIn, final double baseMin, final double baseMax, final double limitMin, final double limitMax) {
        return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
    }

	public boolean isLeftKeyDown() {
		return leftKeyDown;
	}

	public void setLeftKeyDown(boolean leftKeyDown) {
		this.leftKeyDown = leftKeyDown;
	}

	public boolean isRightKeyDown() {
		return rightKeyDown;
	}

	public void setRightKeyDown(boolean rightKeyDown) {
		this.rightKeyDown = rightKeyDown;
	}

	public boolean isUpKeyDown() {
		return upKeyDown;
	}

	public void setUpKeyDown(boolean upKeyDown) {
		this.upKeyDown = upKeyDown;
	}

	public boolean isDownKeyDown() {
		return downKeyDown;
	}

	public void setDownKeyDown(boolean downKeyDown) {
		this.downKeyDown = downKeyDown;
	}

	public boolean isLeftButtonPressed() {
		return isLeftButtonPressed;
	}

	public void setLeftButtonPressed(boolean isLeftButtonPressed) {
		this.isLeftButtonPressed = isLeftButtonPressed;
	}

	public boolean isRightButtonPressed() {
		return isRightButtonPressed;
	}

	public void setRightButtonPressed(boolean isRightButtonPressed) {
		this.isRightButtonPressed = isRightButtonPressed;
	}

	@Override
	public String toString() {
		return "Input [leftKeyDown=" + leftKeyDown + ", rightKeyDown=" + rightKeyDown + ", upKeyDown=" + upKeyDown + ", downKeyDown=" + downKeyDown
				+ ", isLeftButtonPressed=" + isLeftButtonPressed + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (downKeyDown ? 1231 : 1237);
		result = prime * result + (isLeftButtonPressed ? 1231 : 1237);
		result = prime * result + (isRightButtonPressed ? 1231 : 1237);
		result = prime * result + (leftKeyDown ? 1231 : 1237);
		result = prime * result + (rightKeyDown ? 1231 : 1237);
		result = prime * result + (upKeyDown ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Input other = (Input) obj;
		if (downKeyDown != other.downKeyDown)
			return false;
		if (isLeftButtonPressed != other.isLeftButtonPressed)
			return false;
		if (isRightButtonPressed != other.isRightButtonPressed)
			return false;
		if (leftKeyDown != other.leftKeyDown)
			return false;
		if (rightKeyDown != other.rightKeyDown)
			return false;
		if (upKeyDown != other.upKeyDown)
			return false;
		return true;
	}

}
