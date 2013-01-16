package com.lagodiuk.nn;

import java.util.ArrayList;
import java.util.List;

public class Neuron implements Cloneable {

    private double inputSignal;

    private double afterActivationSignal;

    private ThresholdFunction thresholdFunction;

    private List<Double> params;

    public Neuron( ThresholdFunction function, List<Double> params ) {
        this.setFunctionAndParams( function, params );
    }

    public void setFunctionAndParams( ThresholdFunction function, List<Double> params ) {
        if ( params.size() != function.getDefaultParams().size() ) {
            throw new IllegalArgumentException( "Function needs " + function.getDefaultParams().size()
                    + " parameters. But params count is " + params.size() );
        }
        this.thresholdFunction = function;
        this.params = params;
    }

    public void addSignal( double value ) {
        this.inputSignal += value;
    }

    public void activate() {
        this.afterActivationSignal = this.thresholdFunction.calculate( this.inputSignal, this.params );
        this.inputSignal = 0;
    }

    public double getAfterActivationSignal() {
        return this.afterActivationSignal;
    }

    public ThresholdFunction getFunction() {
        return this.thresholdFunction;
    }

    public List<Double> getParams() {
        List<Double> ret = new ArrayList<Double>( this.params.size() );
        for ( Double d : this.params ) {
            ret.add( d );
        }
        return ret;
    }

    @Override
    public Neuron clone() {
        List<Double> cloneParams = new ArrayList<Double>( this.params.size() );
        for ( double d : this.params ) {
            cloneParams.add( d );
        }
        Neuron clone = new Neuron( this.thresholdFunction, cloneParams );
        clone.inputSignal = 0;
        clone.afterActivationSignal = 0;
        return clone;
    }

    @Override
    public String toString() {
        return "Neuron [thresholdFunction=" + this.thresholdFunction + ", params=" + this.params + "]";
    }

}