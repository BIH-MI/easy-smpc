package de.tu_darmstadt.cbs.emailsmpc;

import de.tu_darmstadt.cbs.secretshare.*;
import java.math.BigInteger;

public class Bin {
    public final String name;
    private ArithmeticShare[] inShares;
    private ArithmeticShare[] outShares;

    public Bin(String name, int numParties) {
        this.name = name;
        inShares = new ArithmeticShare[numParties];
        outShares = new ArithmeticShare[numParties];
    }

    public Bin(String name) {
        this.name = name;
        inShares = null;
        outShares = null;
    }

    public void initialize(int numParties) throws IllegalStateException {
        if (inShares != null)
            throw new IllegalStateException("Unable to initialize already initialized bin");
        inShares = new ArithmeticShare[numParties];
        outShares = new ArithmeticShare[numParties];
    }

    public boolean isInitialized() {
        return inShares == null;
    }

    public ArithmeticShare getOutShare(int participant) {
        return outShares[participant];
    }

    public void setInShares(ArithmeticShare[] shares) throws IllegalArgumentException {
        if (shares.length != inShares.length) {
            throw new IllegalArgumentException("Number of shares not compatible with number of parties");
        }
        inShares = shares;
    }

    public void setInShare(ArithmeticShare share, int participant) {
        inShares[participant] = share;
    }

    public ArithmeticShare getSumShare() throws IllegalStateException {
        ArithmeticShare sum = inShares[0];
        for (int i = 0; i < inShares.length; i++) {
            if (inShares[i] == null)
                throw new IllegalStateException("Can not create sum with incomplete shares");
            if (i != 0) { // sum is already initialized as share 0
                sum = sum.add(inShares[i]);
            }
        }
        return sum;
    }

    public BigInteger reconstructBin() throws IllegalStateException {
        for (ArithmeticShare b : inShares) {
            if (b == null)
                throw new IllegalStateException("Can not reconstruct incomplete shares");
        }
        return ArithmeticSharing.reconstruct(inShares);
    }
}
