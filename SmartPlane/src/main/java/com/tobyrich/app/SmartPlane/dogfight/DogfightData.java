package com.tobyrich.app.SmartPlane.dogfight;

import java.io.Serializable;

/**
 * @author Radu Hambasan
 * @date 31 Jul 2014
 */
public class DogfightData implements Serializable{
    public boolean hit;
    public DogfightData() {
        hit = false;
    }
}
