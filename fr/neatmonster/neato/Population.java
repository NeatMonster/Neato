package fr.neatmonster.neato;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Population {
    public static final int POPULATION = 150;
    public static final int STALENESS  = 15;
    public static final int INPUTS     = 64;
    public static final int OUTPUTS    = 6;

    public static final double SELECTION    = 0.20;
    public static final double CROSSOVER    = 0.75;
    public static final double BIAS_MUT     = 0.80;
    public static final double BIAS_PERT    = 0.90;
    public static final double BIAS_STEP    = 0.10;
    public static final double CONNECT_MUT  = 0.80;
    public static final double CONNECT_PERT = 0.90;
    public static final double CONNECT_STEP = 0.10;
    public static final double DISABLE_MUT  = 0.10;
    public static final double ENABLE_MUT   = 0.05;
    public static final double LINK_MUT     = 0.30;
    public static final double NODE_MUT     = 0.20;

    public static final double DELTA_DISJOINT  = 1.0;
    public static final double DELTA_EXCESS    = 1.0;
    public static final double DELTA_THRESHOLD = 3.0;
    public static final double DELTA_WEIGHTS   = 0.4;

    public static final Random RANDOM = new Random();

    public static boolean isInput(final int neuronId) {
        return neuronId < INPUTS;
    }

    public static boolean isOutput(final int neuronId) {
        return neuronId >= INPUTS && neuronId < INPUTS + OUTPUTS;
    }

    public final List<Species>    species     = new ArrayList<Species>();
    public final List<Individual> population  = new ArrayList<Individual>();
    public double                 avgFitness  = 0.0;
    public double                 bestFitness = 0.0;

    public Population() {
        for (int i = 0; i < POPULATION; ++i) {
            final Individual creature = new Individual();
            creature.mutate();
            creature.generate();
            population.add(creature);
        }
    }

    public void addToSpecies(final Individual creature) {
        for (final Species species : this.species)
            if (creature.sameSpecies(species.founder)) {
                species.members.add(creature);
                return;
            }

        final Species newSpecies = new Species();
        newSpecies.founder = creature;
        newSpecies.members.add(creature);
        species.add(newSpecies);
    }

    public void newGeneration() {
        for (final Individual creature : population)
            addToSpecies(creature);

        for (final Species species : new ArrayList<Species>(this.species))
            if (species.members.isEmpty())
                this.species.remove(species);

        Individual best = null;
        avgFitness = bestFitness = 0.0;
        for (final Individual creature : population) {
            avgFitness += creature.fitness;
            if (creature.fitness > bestFitness) {
                bestFitness = creature.fitness;
                best = creature;
            }
        }
        avgFitness /= population.size();

        for (final Species species : this.species)
            species.best = species.members.contains(best);

        for (final Species species : new ArrayList<Species>(this.species)) {
            double bestFitness = 0.0;
            for (final Individual creature : species.members)
                if (creature.fitness > bestFitness)
                    bestFitness = creature.fitness;

            if (bestFitness > species.bestFitness) {
                species.bestFitness = bestFitness;
                species.staleness = 0;
                continue;
            }
            ++species.staleness;

            if (species.staleness > STALENESS && !species.best) {
                this.species.remove(species);
                population.removeAll(species.members);
            }
        }

        final double avgFitness = totalAvgFitness();
        for (final Species species : this.species)
            species.offsprings = (int) Math
                    .round(POPULATION * species.calcAvgFitness() / avgFitness);

        for (final Species species : new ArrayList<Species>(this.species))
            if (species.offsprings == 0) {
                this.species.remove(species);
                population.removeAll(species.members);
            }

        List<Individual> newPopulation = new ArrayList<Individual>();
        for (final Species species : this.species)
            newPopulation.addAll(species.reproduce());

        if (newPopulation.size() > POPULATION)
            newPopulation = new ArrayList<Individual>(
                    newPopulation.subList(0, POPULATION));
        else {
            while (newPopulation.size() < POPULATION) {
                final Individual mother = population
                        .get(RANDOM.nextInt(population.size()));
                final Individual father = population
                        .get(RANDOM.nextInt(population.size()));
                final Individual child = new Individual(mother, father);
                child.mutate();
                child.generate();
                newPopulation.add(child);
            }
        }

        population.clear();
        population.addAll(newPopulation);
    }

    public double totalAvgFitness() {
        double total = 0.0;
        for (final Species species : this.species)
            total += species.calcAvgFitness();
        return total;
    }
}
