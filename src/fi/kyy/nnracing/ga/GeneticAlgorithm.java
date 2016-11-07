package fi.kyy.nnracing.ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.kyy.nnracing.utils.RandomUtil;

public class GeneticAlgorithm {

	private ArrayList<Genome> population;
	private final int populationSize;
	private final int numberOfGenes;
	private int generation;

	public GeneticAlgorithm(int populationSize, int numberOfGenes) {
		this.numberOfGenes = numberOfGenes;
		this.generation = 0;
		this.populationSize = populationSize;
		this.population = new ArrayList<Genome>(populationSize);

		for (int i = 0; i < populationSize; i++) {
			this.population.add(new Genome(numberOfGenes));
		}
	}

	public GeneticAlgorithm(int populationSize, List<Float> genes) {
		this.numberOfGenes = genes.size();
		this.generation = 0;
		this.populationSize = populationSize;
		this.population = new ArrayList<Genome>(populationSize);

		for (int i = 0; i < populationSize; i++) {
			this.population.add(new Genome(numberOfGenes, genes.toArray(new Float[genes.size()])));
		}
	}

	public void breed() {
		ArrayList<Genome> newPopulation = new ArrayList<Genome>();

		List<Genome> fittest = getFittest(20);
		Genome father = fittest.get(0);
		Genome mother = fittest.get(1);

		// Father and mother stay in the next generation
		System.out.println("FATHER FITNESS: " + father.getFitness());
		
		newPopulation.add(father);
		newPopulation.add(mother);
		newPopulation.add(father.cloneGenome(true));
		newPopulation.add(mother.cloneGenome(true));

		for (int i = newPopulation.size(); i < populationSize; i++) {
			Genome baby = crossOver(father, mother);
			baby.mutate();
			newPopulation.add(baby);
		}

		this.population = newPopulation;
		this.generation++;
	}

	private Genome crossOver(Genome father, Genome mother) {
		List<Float> babyGenes = new ArrayList<Float>();

		for (int i = 0; i < father.getGenes().length; i++) {
			if (RandomUtil.nextBoolean()) {
				babyGenes.add(father.getGenes()[i]);
			} else {
				babyGenes.add(mother.getGenes()[i]);
			}
		}

		return new Genome(0, babyGenes.toArray(new Float[babyGenes.size()]));
	}

	public Genome getFittest() {
		List<Genome> fittest = getFittest(1);
		return fittest.get(0);
	}
	
	public Genome getMother() {
		List<Genome> fittest = getFittest(2);
		return fittest.get(1);
	}

	public List<Genome> getFittest(int n) {
		List<Genome> result = null;

		if (n > populationSize) {
			result = population;
		} else {
			Collections.sort(population);
			result = population.subList(0, n);
		}

		return result;
	}

	public int getGeneration() {
		return generation;
	}

	public ArrayList<Genome> getPopulation() {
		return population;
	}

	@Override
	public String toString() {
		return this.generation + " " + this.getFittest().toString();
	}

}
