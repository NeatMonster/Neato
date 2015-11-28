package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.CROSSOVER;
import static fr.neatmonster.neato.Population.RANDOM;
import static fr.neatmonster.neato.Population.SELECTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Species {
    public Individual             founder;
    public final List<Individual> members     = new ArrayList<Individual>();
    public int                    offsprings  = 0;
    public int                    staleness   = 0;
    public boolean                best        = false;
    public double                 bestFitness = 0.0;

    public double calcAvgFitness() {
        double total = 0.0;
        for (final Individual member : members)
            total += member.fitness;
        return total / members.size();
    }

    public List<Individual> reproduce() {
        final List<Individual> offspring = new ArrayList<Individual>();

        Collections.sort(members, new Comparator<Individual>() {

            @Override
            public int compare(final Individual o1, final Individual o2) {
                final double cmp = o2.fitness - o1.fitness;
                return cmp > 0 ? 1 : cmp < 0 ? -1 : 0;
            }
        });

        if (members.size() >= 5) {
            --offsprings;
            offspring.add(members.get(0));
        }

        final List<Individual> newMembers = new ArrayList<Individual>();
        final int survivors = (int) Math.round(SELECTION * members.size());
        newMembers.addAll(members.subList(0, Math.max(1, survivors)));
        members.clear();
        members.addAll(newMembers);

        while (offsprings > 0) {
            --offsprings;
            final Individual child;
            if (RANDOM.nextDouble() < CROSSOVER) {
                final Individual mother = tournament();
                final Individual father = tournament();
                child = new Individual(mother, father);
            } else
                child = members.get(RANDOM.nextInt(members.size())).clone();
            child.mutate();
            child.generate();
            offspring.add(child);
        }

        members.clear();
        return offspring;
    }

    public Individual tournament() {
        final int winner = RANDOM.nextInt(members.size());
        final int loser = RANDOM.nextInt(members.size());
        return winner > loser ? members.get(winner) : members.get(loser);
    }
}