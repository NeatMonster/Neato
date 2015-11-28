package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.INPUTS;
import static fr.neatmonster.neato.Population.OUTPUTS;

import java.util.HashMap;
import java.util.Map;

public class Innovations {
    public static class Innovation {
        public int id;
        public int from, to;
        public int neuron;
    }

    private static final Map<Integer, Innovation> links          = new HashMap<Integer, Innovation>();
    private static final Map<Integer, Innovation> nodes          = new HashMap<Integer, Innovation>();
    private static int                            nextInnovation = OUTPUTS;
    private static int                            nextNeuron     = INPUTS
            + OUTPUTS;

    public static int addLink(final int in, final int out) {
        final Innovation innov = new Innovation();
        innov.id = nextInnovation++;
        innov.from = in;
        innov.to = out;
        links.put(pair(in, out), innov);
        return innov.id;
    }

    public static int addNeuron(final int in, final int out) {
        final Innovation innov = new Innovation();
        innov.id = nextInnovation++;
        innov.from = in;
        innov.to = out;
        innov.neuron = nextNeuron++;
        nodes.put(pair(in, out), innov);
        return innov.neuron;
    }

    public static int checkInnovation(final int in, final int out,
            final boolean neuron) {
        final Innovation innov;
        if (neuron)
            innov = nodes.get(pair(in, out));
        else
            innov = links.get(pair(in, out));
        return innov == null ? -1 : innov.id;
    }

    public static int findNeuron(final int in, final int out) {
        final Innovation innov = nodes.get(pair(in, out));
        return innov == null ? -1 : innov.neuron;
    }

    public static int pair(final int in, final int out) {
        return (in + out) * (in + out + 1) / 2 + out;
    }
}
