package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.OUTPUTS;

import java.util.ArrayList;
import java.util.List;

public class Ensemble extends Individual {
    public final List<Individual> elements = new ArrayList<Individual>();

    @Override
    public void feedForward() {
        for (final Individual element : elements)
            element.feedForward();
    }

    @Override
    public double[] getOutput() {
        final double[] output = new double[OUTPUTS];
        for (final Individual element : elements) {
            final double[] vote = element.getOutput();

            for (int i = 0; i < output.length; ++i)
                if (vote[i] > 0.5)
                    output[i] += 1.0;
        }

        for (int i = 0; i < output.length; ++i)
            output[i] /= elements.size();

        return output;
    }

    @Override
    public void setInput(final double[] input) {
        for (final Individual element : elements)
            element.setInput(input);
    }
}
