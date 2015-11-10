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
    public static final int INPUTS     = 62;
    public static final int OUTPUTS    = 6;
    public static final int FITNESS    = 5;

    public static final double TOURNAMENT = 0.20;
    public static final double CROSSOVER  = 0.75;

    public static final double CONNECT_MUT  = 0.80;
    public static final double CONNECT_PERT = 0.90;
    public static final double CONNECT_STEP = 0.10;
    public static final double ENABLE_MUT   = 0.05;
    public static final double DISABLE_MUT  = 0.10;
    public static final double LINK_MUT     = 0.50;
    public static final double BIAS_MUT     = 0.05;
    public static final double NODE_MUT     = 0.05;

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

            final double cmpDist = o1.distance - o2.distance;
            return cmpDist > 0.0 ? -1 : cmpDist < 0.0 ? 1 : 0;
        }
    };

    public static int nextInnovation = OUTPUTS;

    public static boolean isInput(final int neuronId) {
        return neuronId < INPUTS;
    }

    public static boolean isOutput(final int neuronId) {
        return neuronId >= INPUTS && neuronId < INPUTS + OUTPUTS;
    }

    public final List<Individual> phenotypes = new ArrayList<Individual>();
    public Ensemble               ensemble   = null;

    public Population() {
        for (int i = 0; i < POPULATION; ++i)
            phenotypes.add(new Individual());
    }

    public void addChildren() {
        final List<Individual> children = new ArrayList<Individual>();

        final int crossovers = (int) (POPULATION * CROSSOVER);
        for (int i = 0; i < crossovers; ++i) {
            Individual mother = tournament(), father;
            do
                father = tournament();
            while (mother.equals(father));

            if (mother.ranking < father.ranking) {
                final Individual tmp = mother;
                mother = father;
                father = tmp;
            }

            children.add(new Individual(mother, father));
        }
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

                for (int k = 1; k < front.size() - 2; ++k) {
                    final Individual sol = front.get(k);
                    if (sol.distance == Double.MAX_VALUE)
                        continue;

                    final double next = front.get(k + 1).fitness[index];
                    final double prev = front.get(k - 1).fitness[index];
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

    public void cullPop() {
        final List<Individual> newPhenotypes = new ArrayList<Individual>(
                phenotypes.subList(0, POPULATION));
        phenotypes.clear();
        phenotypes.addAll(newPhenotypes);
    }

    public void firstEvol() {
        calcRank();
        Collections.sort(phenotypes, RANK_CMP);
        addChildren();
    }

    public void secondEvol() {
        calcRank();
        Collections.sort(phenotypes, RANK_CMP);
        calcDist();
        Collections.sort(phenotypes, DIST_CMP);
        cullPop();
        setEnsemble();
    }

    public void setEnsemble() {
        ensemble = new Ensemble();
        for (final Individual element : phenotypes)
            if (ensemble.elements.isEmpty()
                    || ensemble.elements.get(0).ranking == element.ranking)
                ensemble.elements.add(element);
    }

    public Individual tournament() {
        for (final Individual creature : phenotypes)
            if (RANDOM.nextDouble() < TOURNAMENT)
                return creature;

        return phenotypes.get(0);
    }
}
