package fr.neatmonster.neato;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Population {
    public static final int POPULATION = 250;
    public static final int STALENESS  = 30;

    public static final int    INPUTS  = 62;
    public static final int    OUTPUTS = 6;
    public static final int    FITNESS = 5;
    public static final double EPSILON = 0.001;

    public static final double CROSSOVER    = 0.75;
    public static final double CONNECT_MUT  = 0.80;
    public static final double CONNECT_PERT = 0.90;
    public static final double CONNECT_STEP = 0.10;
    public static final double ENABLE_MUT   = 0.05;
    public static final double DISABLE_MUT  = 0.10;
    public static final double LINK_MUT     = 0.50;
    public static final double BIAS_MUT     = 0.05;
    public static final double NODE_MUT     = 0.05;

    public static final double DELTA_DISJOINT  = 2.0;
    public static final double DELTA_WEIGHTS   = 0.4;
    public static final double DELTA_THRESHOLD = 1.0;

    public static final Random RANDOM = new Random();

    public static Comparator<Individual> RANK_CMP = new Comparator<Individual>() {

        @Override
        public int compare(final Individual o1, final Individual o2) {
            return o1.ranking - o2.ranking;
        }
    };

    public static Comparator<Individual> DIST_CMP = new Comparator<Individual>() {

        @Override
        public int compare(final Individual o1, final Individual o2) {
            final int cmpRank = o1.ranking - o2.ranking;
            if (cmpRank != 0)
                return cmpRank;

            final double cmpDist = o2.distance - o1.distance;
            return cmpDist > 0.0 ? 1 : cmpDist < 0.0 ? -1 : 0;
        }
    };

    public static Comparator<Individual> GLOB_CMP = new Comparator<Individual>() {

        @Override
        public int compare(final Individual o1, final Individual o2) {
            return o2.global - o1.global;
        }
    };

    public static int nextInnovation = OUTPUTS;

    public static boolean isInput(final int neuronId) {
        return neuronId < INPUTS;
    }

    public static boolean isOutput(final int neuronId) {
        return neuronId >= INPUTS && neuronId < INPUTS + OUTPUTS;
    }

    public Ensemble               ensemble   = null;
    public final List<Species>    species    = new ArrayList<Species>();
    public final List<Individual> phenotypes = new ArrayList<Individual>();

    public Population() {
        for (int i = 0; i < POPULATION; ++i) {
            final Individual creature = new Individual();
            creature.mutate();
            creature.generate();
            addToSpecies(creature);
        }
    }

    public void addToSpecies(final Individual creature) {
        for (final Species species : this.species)
            if (creature.sameSpecies(species.members.get(0))) {
                species.members.add(creature);
                return;
            }

        final Species newSpecies = new Species();
        newSpecies.members.add(creature);
        species.add(newSpecies);
    }

    public void calcDist() {
        final Map<Integer, List<Individual>> fronts = new HashMap<Integer, List<Individual>>();

        for (final Individual creature : phenotypes) {
            final int rank = creature.ranking;
            if (!fronts.containsKey(rank))
                fronts.put(rank, new ArrayList<Individual>());
            fronts.get(rank).add(creature);
        }

        for (final List<Individual> front : fronts.values()) {
            for (final Individual creature : front)
                creature.distance = 0.0;

            for (int i = 0; i < FITNESS; ++i) {
                final int index = i;
                Collections.sort(front, new Comparator<Individual>() {

                    @Override
                    public int compare(final Individual o1,
                            final Individual o2) {
                        final double cmp = o1.fitness[index]
                                - o2.fitness[index];
                        return cmp > 0.0 ? 1 : cmp < 0.0 ? -1 : 0;
                    }
                });

                final Individual minSol = front.get(0);
                minSol.distance = Double.MAX_VALUE;
                final double min = minSol.fitness[index];

                final Individual maxSol = front.get(front.size() - 1);
                maxSol.distance = Double.MAX_VALUE;
                final double max = maxSol.fitness[index];

                for (int j = 1; j < front.size() - 2; ++j) {
                    final Individual sol = front.get(j);
                    if (sol.distance == Double.MAX_VALUE)
                        continue;

                    final double next = front.get(j + 1).fitness[index];
                    final double prev = front.get(j - 1).fitness[index];
                    sol.distance += (next - prev) / (max - min);
                }
            }
        }
    }

    public void calcRank() {
        for (final Individual creature : phenotypes)
            creature.ranking = 0;

        for (int i = 0; i < phenotypes.size() - 1; ++i)
            for (int j = i + 1; j < phenotypes.size(); ++j) {
                final Individual first = phenotypes.get(i);
                final Individual second = phenotypes.get(j);

                if (first.dominates(second))
                    ++second.ranking;
                else if (second.dominates(first))
                    ++first.ranking;
            }
    }

    public void cullSpecies(final boolean cutToOne) {
        for (final Species species : this.species) {
            Collections.sort(species.members, GLOB_CMP);

            double remaining = 1.0;
            if (!cutToOne)
                remaining = Math.ceil(species.members.size() / 2.0);

            while (species.members.size() > remaining)
                species.members.remove(species.members.size() - 1);
        }
    }

    public void listAll() {
        phenotypes.clear();
        for (final Species species : this.species)
            for (final Individual creature : species.members)
                phenotypes.add(creature);
    }

    public void newGeneration() {
        rankGlobally();
        cullSpecies(false);

        rankGlobally();
        removeStaleSpecies();

        rankGlobally();
        for (final Species species : this.species)
            species.calcAvgGlobal();
        removeWeakSpecies();

        final List<Individual> children = new ArrayList<Individual>();

        final double sum = totalAvgGlobal();
        for (final Species species : this.species) {
            final double breed = Math
                    .floor(POPULATION * species.avgGlobal / sum);
            for (int i = 0; i < breed - 1.0; ++i)
                children.add(species.breedChild());
        }

        cullSpecies(true);

        while (children.size() + species.size() < POPULATION)
            children.add(
                    species.get(RANDOM.nextInt(species.size())).breedChild());

        for (final Individual child : children)
            addToSpecies(child);

        setEnsemble();
    }

    public void rankGlobally() {
        listAll();
        calcRank();
        Collections.sort(phenotypes, RANK_CMP);
        calcDist();
        Collections.sort(phenotypes, DIST_CMP);
        for (int i = 0; i < phenotypes.size(); ++i)
            phenotypes.get(i).global = POPULATION - i;
    }

    public void removeStaleSpecies() {
        final List<Species> survived = new ArrayList<Species>();

        for (final Species species : this.species) {
            Collections.sort(species.members, GLOB_CMP);

            if (species.members.get(0).global > species.topGlobal) {
                species.topGlobal = species.members.get(0).global;
                species.staleness = 0;
            } else
                ++species.staleness;

            if (species.staleness < STALENESS)
                survived.add(species);
        }

        species.clear();
        species.addAll(survived);
    }

    public void removeWeakSpecies() {
        final List<Species> survived = new ArrayList<Species>();

        final double sum = totalAvgGlobal();
        for (final Species species : this.species) {
            final double breed = Math
                    .floor(POPULATION * species.avgGlobal / sum);
            if (breed >= 1.0)
                survived.add(species);
        }

        species.clear();
        species.addAll(survived);
    }

    public void setEnsemble() {
        ensemble = new Ensemble();
        for (final Individual element : phenotypes)
            if (ensemble.elements.isEmpty()
                    || ensemble.elements.get(0).ranking == element.ranking)
                ensemble.elements.add(element);
    }

    public double totalAvgGlobal() {
        double total = 0.0;
        for (final Species species : this.species)
            total += species.avgGlobal;
        return total;
    }
}
