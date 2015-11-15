package fr.neatmonster.neato;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Population {
    public static final int       POPULATION = 50;
    public static final int       INPUTS     = 61;
    public static final int       OUTPUTS    = 6;
    public static final boolean[] FITNESS    = new boolean[] {
            false, false, false, true, true
    };

    public static final double ELITISM      = 0.50;
    public static final double TOURNAMENT   = 0.90;
    public static final double CONNECT_MUT  = 0.80;
    public static final double CONNECT_PERT = 0.05;
    public static final double ENABLE_MUT   = 0.05;
    public static final double DISABLE_MUT  = 0.10;
    public static final double LINK_MUT     = 0.30;
    public static final double NODE_MUT     = 0.20;

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
            final double cmpDist = o2.distance - o1.distance;
            return cmpDist > 0.0 ? 1 : cmpDist < 0.0 ? -1 : 0;
        }
    };

    public static Comparator<Individual> ALL_CMP = new Comparator<Individual>() {

        @Override
        public int compare(final Individual o1, final Individual o2) {
            final int cmpRank = o1.ranking - o2.ranking;
            if (cmpRank != 0)
                return cmpRank;

            final double cmpDist = o2.distance - o1.distance;
            return cmpDist > 0.0 ? 1 : cmpDist < 0.0 ? -1 : 0;
        }
    };

    public static int nextInnovation = OUTPUTS;

    public static boolean isInput(final int neuronId) {
        return neuronId < INPUTS;
    }

    public static boolean isOutput(final int neuronId) {
        return neuronId >= INPUTS && neuronId < INPUTS + OUTPUTS;
    }

    public final List<Individual> parents  = new ArrayList<Individual>();
    public final List<Individual> children = new ArrayList<Individual>();

    public Population() {
        for (int i = 0; i < POPULATION; ++i) {
            final Individual creature = new Individual();
            creature.mutate();
            creature.generate();
            parents.add(creature);
        }
    }

    public void calcDist(final List<Individual> front) {
        for (final Individual creature : front)
            creature.distance = 0.0;

        for (int i = 0; i < FITNESS.length; ++i) {
            final int index = i;
            Collections.sort(front, new Comparator<Individual>() {

                @Override
                public int compare(final Individual o1, final Individual o2) {
                    final double cmp = o1.fitness[index] - o2.fitness[index];
                    return cmp > 0.0 ? 1 : cmp < 0.0 ? -1 : 0;
                }
            });

            final Individual minSol = front.get(0);
            minSol.distance = Double.MAX_VALUE;
            final double min = minSol.fitness[index];

            final Individual maxSol = front.get(front.size() - 1);
            maxSol.distance = Double.MAX_VALUE;
            final double max = maxSol.fitness[index];

            for (int j = 1; j < front.size() - 1; ++j) {
                final Individual sol = front.get(j);
                if (sol.distance == Double.MAX_VALUE)
                    continue;

                final double next = front.get(j + 1).fitness[index];
                final double prev = front.get(j - 1).fitness[index];
                sol.distance += (next - prev) / (max - min);
            }
        }

        Collections.sort(front, DIST_CMP);
    }

    public Map<Integer, List<Individual>> calcRank(
            final List<Individual> phenotypes) {
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

        Collections.sort(phenotypes, RANK_CMP);

        final Map<Integer, List<Individual>> fronts = new HashMap<Integer, List<Individual>>();

        for (final Individual creature : phenotypes) {
            final int rank = creature.ranking;
            if (!fronts.containsKey(rank))
                fronts.put(rank, new ArrayList<Individual>());
            fronts.get(rank).add(creature);
        }

        return fronts;
    }

    public void firstGeneration() {
        calcRank(parents);
        children.addAll(getChildren());
    }

    public List<Individual> getChildren() {
        final List<Individual> children = new ArrayList<Individual>();

        children.addAll(parents.subList(0, (int) (ELITISM * POPULATION)));

        for (int i = children.size(); i < POPULATION; ++i) {
            final Individual mutation = tournament().clone();
            mutation.mutate();
            mutation.generate();
            children.add(mutation);
        }

        return children;
    }

    public void nextGeneration() {
        final List<Individual> combined = new ArrayList<Individual>();
        combined.addAll(parents);
        combined.addAll(children);

        final Map<Integer, List<Individual>> fronts = calcRank(combined);

        final List<Individual> newParents = new ArrayList<Individual>();
        for (final List<Individual> front : fronts.values()) {
            if (newParents.size() >= POPULATION)
                break;

            calcDist(front);
            newParents.addAll(front);
        }

        Collections.sort(newParents, ALL_CMP);

        while (newParents.size() > POPULATION)
            newParents.remove(newParents.size() - 1);

        parents.clear();
        parents.addAll(newParents);

        children.clear();
        children.addAll(getChildren());
    }

    public Individual tournament() {
        final int winner = RANDOM.nextInt(parents.size());
        final int loser = RANDOM.nextInt(parents.size());

        if (winner > loser)
            return parents.get(winner);
        else
            return parents.get(loser);
    }
}
