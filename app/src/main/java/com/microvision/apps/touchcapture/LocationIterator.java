package com.microvision.apps.touchcapture;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

public class LocationIterator {
    private int size, completed;

    public int getProgress(){
        return   Math.round((float)completed / size * 100);
    }

    ArrayList<Location> locationArrayList = new ArrayList<>();

    public LocationIterator(int width, int height){
        // build list width X height and keep in stack
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                locationArrayList.add(new Location(i, j));
            }
        }

        this.size = locationArrayList.size();

        // and shuffle
        Collections.shuffle(locationArrayList);
    }

    // can we use yield?
    protected Location nextLocation() {
        // return next location until finished
        if(locationArrayList.size() == 0) {
            return null;
        }

        this.completed++;

        Log.d("LocationIterator", "Progress: " + this.getProgress() + " Completed: " + this.completed + " Of: " + this.size);


        return locationArrayList.remove(0);
    }
}
