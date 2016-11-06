package fi.kyy.nnracing.ga;

import java.util.Arrays;

import fi.kyy.nnracing.utils.RandomUtil;

public class Genome implements Comparable<Genome> {
	private static final Float MUTATION_RATE = .15f;

	private Float[] genes;
	private float fitness;

	public Genome(int n) {
		this.genes = new Float[n];
		for (int i = 0; i < genes.length; i++) {
			this.genes[i] = RandomUtil.nextClampedFloat();
		}
	}

	public Genome(float fitness, Float[] genes, boolean mutate) {
		this.fitness = fitness;

		this.genes = new Float[genes.length];

		for (int i = 0; i < genes.length; i++) {
			this.genes[i] = genes[i];
		}

		if (mutate) {
			mutate();
		}
	}
	
	public Genome(float fitness, Float[] genes) {
		this(fitness, genes, false);
	}

	public Genome cloneGenome(boolean mutate) {
		return new Genome(this.fitness, this.genes, mutate);
	}

	public void mutate(float rate) {
		for (int i = 0; i < genes.length; i++) {
			if (RandomUtil.nextFloat() < rate) {
				genes[i] *= RandomUtil.nextClampedFloat();
			}
		}
	}

	public void mutate() {
		mutate(MUTATION_RATE);
	}

	public Float[] getGenes() {
		return genes;
	}

	public void setGenes(Float[] genes) {
		this.genes = genes;
	}

	public float getFitness() {
		return fitness;
	}

	public void setFitness(float fitness) {
		this.fitness = fitness;
	}

	@Override
	public int compareTo(Genome o) {
		return this.fitness < o.fitness ? 1 : -1;
	}

	@Override
	public String toString() {
		return "Genome [cromossomes=" + Arrays.toString(genes) + ", fitness=" + fitness + "]";
	}
}

