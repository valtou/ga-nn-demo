package fi.kyy.nnracing.neuralnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NeuralNetwork {

	private final List<List<Neuron>> hiddenLayers;
	private final List<Neuron> outputLayer;

	public NeuralNetwork(int numberOfInputs, int numberOfOutputs, int numberOfHiddenLayers, int numberOfNeuronsPerHiddenLayer) {
		// Initialize the hidden layers
		this.hiddenLayers = new ArrayList<List<Neuron>>(numberOfHiddenLayers);

		for (int i = 0; i < numberOfHiddenLayers; i++) {
			ArrayList<Neuron> neurons = new ArrayList<Neuron>();
			for (int j = 0; j < numberOfNeuronsPerHiddenLayer; j++) {
				if (i == 0) {
					neurons.add(new Neuron(numberOfInputs + 1));
				} else {
					neurons.add(new Neuron(numberOfNeuronsPerHiddenLayer + 1));
				}
			}
			this.hiddenLayers.add(neurons);
		}

		// Initialize the outputs with as many inputs as the number of neurons
		// per hidden layer
		this.outputLayer = new ArrayList<Neuron>();
		for (int i = 0; i < numberOfOutputs; i++) {
			Neuron neuron = new Neuron(numberOfNeuronsPerHiddenLayer + 1);
			this.outputLayer.add(neuron);
		}
	}

	public List<Float> update(List<Float> inputs) {
		List<Float> hiddenLayerOutput = evaluateHiddenLayers(inputs);
		return evaluateOutputLayer(hiddenLayerOutput);
	}

	public List<Float> update(Float... inputs) {
		List<Float> inputList = Arrays.asList(inputs);
		return update(inputList);
	}

	public void importWeights(Float[] weights) {
		int weightsIndex = 0;

		for (List<Neuron> layer : hiddenLayers) {
			for (Neuron neuron : layer) {
				List<Float> weightsList = new ArrayList<Float>();
				for (int i = 0; i < neuron.getWeights().length; i++) {
					weightsList.add(weights[weightsIndex++]);
				}
				neuron.setWeights(weightsList.toArray(new Float[weightsList.size()]));
			}
		}

		for (Neuron neuron : outputLayer) {
			List<Float> weightsList = new ArrayList<Float>();
			for (int i = 0; i < neuron.getWeights().length; i++) {
				weightsList.add(weights[weightsIndex++]);
			}
			neuron.setWeights(weightsList.toArray(new Float[weightsList.size()]));
		}
	}

	public List<Float> exportWeights() {
		List<Float> weights = new ArrayList<Float>();

		for (List<Neuron> layer : hiddenLayers) {
			for (Neuron neuron : layer) {
				for (int i = 0; i < neuron.getWeights().length; i++) {
					weights.add(neuron.getWeights()[i]);
				}
			}
		}

		for (Neuron neuron : outputLayer) {
			for (int i = 0; i < neuron.getWeights().length; i++) {
				weights.add(neuron.getWeights()[i]);
			}
		}

		return weights;

	}

	public Integer getWeightCount() {
		List<Float> exportWeights = exportWeights();
		return exportWeights != null ? exportWeights.size() : null;
	}

	private List<Float> evaluateHiddenLayers(List<Float> inputs) {
		List<Float> hiddenLayerInput = inputs;
		List<Float> hiddenLayerOutput = new ArrayList<Float>();

		for (int i = 0; i < hiddenLayers.size(); i++) {
			List<Neuron> layer = hiddenLayers.get(i);
			for (Neuron neuron : layer) {
				hiddenLayerOutput.add(neuron.evaluate(hiddenLayerInput.toArray(new Float[inputs.size()])));
			}
			hiddenLayerInput = hiddenLayerOutput;
		}
		return hiddenLayerOutput;
	}

	private List<Float> evaluateOutputLayer(List<Float> hiddenLayerOutput) {
		List<Float> outputs = new ArrayList<Float>();

		for (int i = 0; i < outputLayer.size(); i++) {
			Neuron neuron = outputLayer.get(i);

			outputs.add(neuron.evaluate(hiddenLayerOutput.toArray(new Float[hiddenLayerOutput.size()])));
		}

		return outputs;
	}

	@Override
	public String toString() {
		String s = "NeuralNetwork [\n";
		s += "  Hidden Layers [";
		for (List<Neuron> list : hiddenLayers) {
			s += "\n    Layer [\n      ";
			for (Neuron neuron : list) {
				s += neuron.toString() + "\n      ";
			}
			s += "]";
		}

		s += "\n  ]\n  Output Layer [";
		for (Neuron neuron : outputLayer) {
			s += neuron.toString() + "\n  ";
		}

		return s + "\n]";
	}

}
