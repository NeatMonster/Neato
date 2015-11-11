package fr.neatmonster.neato;

import java.util.ArrayList;
import java.util.List;

public class Neuron {
    public static double sigmoid(final double x) {
        return 2.0 / (1.0 + Math.exp(-4.9 * x)) - 1.0;
    }

    public final int           neuronId;
    public final List<Synapse> inputs  = new ArrayList<Synapse>();
    public final List<Synapse> outputs = new ArrayList<Synapse>();
    public double              value   = 0.0;
    public boolean             updated = false;

    public Neuron(final int neuronId) {
        this.neuronId = neuronId;
    }

    public void feedForward() {
        if (updated)
            return;
        updated = true;

        if (!inputs.isEmpty()) {
            value = 0.0;
            for (final Synapse connect : inputs)
                value += connect.weight * connect.input.value;
            value = sigmoid(value);
        }

        for (final Synapse connect : outputs)
            connect.output.feedForward();
    }
}
