package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.isInput;
import static fr.neatmonster.neato.Population.isOutput;

import java.util.ArrayList;
import java.util.List;

public class Neuron {
    public static double sigmoid(final double x) {
        return 2.0 / (1.0 + Math.exp(-4.9 * x)) - 1.0;
    }

    public final int           neuronId;
    public final List<Synapse> inputs      = new ArrayList<Synapse>();
    public final List<Synapse> outputs     = new ArrayList<Synapse>();
    public double              value       = 0.0;
    public double              bias        = 0.0;
    public boolean             updated     = false;
    public boolean             evalInput   = false;
    public boolean             evalOutput  = false;
    public boolean             cacheInput  = false;
    public boolean             cacheOutput = false;

    public Neuron(final int neuronId) {
        this.neuronId = neuronId;
    }

    public boolean connectedInput() {
        if (isInput(neuronId))
            return true;

        if (evalInput)
            return cacheInput;
        evalInput = true;

        boolean connectInput = false;
        for (final Synapse connect : inputs)
            if (connect.enabled && connect.input.connectedInput())
                connectInput = true;
        cacheInput = connectInput;

        return connectInput;
    }

    public boolean connectedOutput() {
        if (isOutput(neuronId))
            return true;

        if (evalOutput)
            return cacheOutput;
        evalOutput = true;

        boolean connectOutput = false;
        for (final Synapse connect : outputs)
            if (connect.enabled && connect.output.connectedOutput())
                connectOutput = true;
        cacheOutput = connectOutput;

        return connectOutput;
    }

    public void feedForward() {
        if (updated)
            return;
        updated = true;

        if (!inputs.isEmpty()) {
            // Highly experimental
            value = bias;
            for (final Synapse connect : inputs)
                if (connect.enabled)
                    value += connect.weight * connect.input.value;
            value = sigmoid(value);
        }

        for (final Synapse connect : outputs)
            if (connect.enabled)
                connect.output.feedForward();
    }

    public boolean shouldDisplay() {
        return connectedInput() && connectedOutput();
    }
}
