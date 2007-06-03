package org.apache.log4j.net;

/**
 * A wrapper class that allows modifiable values, totally <b>not</b> Thread-safe.
 * 
 * Useful if you want to hold a reference to a value in a final block for inner classes
 * and flip the value for later testing.
 * @author paulsmith
 *
 */
public class ModifiableBoolean {

    private boolean value = false;
    
    public void setValue(boolean value) {
        this.value = value;
    }
    
    public boolean isSet() {
        return value;
    }
    
    public void flip() {
        this.value = !this.value;
    }
}
