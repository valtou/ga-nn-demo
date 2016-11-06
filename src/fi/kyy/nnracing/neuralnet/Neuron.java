package fi.kyy.nnracing.neuralnet;

import java.util.Arrays;

import fi.kyy.nnracing.utils.RandomUtil;

public class Neuron {
	private Float[] weights;
	private boolean isSigmoid = true;

	public Neuron(int numberOfInputs) {
		this.weights = new Float[numberOfInputs];
		for (int i = 0; i < weights.length; i++) {
			this.weights[i] = RandomUtil.nextClampedFloat();
		}
	}

	public Neuron(Neuron n) {
		this.weights = new Float[n.weights.length];
		for (int i = 0; i < weights.length; i++) {
			this.weights[i] = n.weights[i];
		}
	}

	public float evaluate(Float[] inputs) {
		float net = 0;

		if (inputs.length != weights.length) {
			// OMG
		}

		for (int i = 0; i < inputs.length; i++) {
			net += inputs[i] * weights[i];
		}

		net += weights[weights.length - 1] * -1f;// bias

		if (isSigmoid) {
			return sigmoid(net);
		} else {
			return net;
		}

	}

	private static float sigmoid(float x) {
		return (float) (1 / (1 + Math.exp(-x)));
	}

	public Float[] getWeights() {
		return weights;
	}

	public void setWeights(Float[] weights) {
		this.weights = weights;
	}

	@Override
	public String toString() {
		return "Neuron [weights=" + Arrays.toString(weights) + "]";
	}

	public boolean isSigmoid() {
		return isSigmoid;
	}

	public void setSigmoid(boolean isSigmoid) {
		this.isSigmoid = isSigmoid;
	}
}
