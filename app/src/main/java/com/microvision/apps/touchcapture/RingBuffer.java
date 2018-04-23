package com.microvision.apps.touchcapture;

public class RingBuffer {
    public FrameWithMetadata[] elements;

    private int capacity  = 0;
    private int writePos  = 0;
    private int available = 0;

    RingBuffer(int desiredCapacity) {
        this.capacity = desiredCapacity;
        this.elements = new FrameWithMetadata[desiredCapacity];
    }

    public void reset() {
        this.writePos = 0;
        this.available = 0;
    }

    public int capacity() { return this.capacity; }
    public int available(){ return this.available; }

    public int remainingCapacity() {
        return this.capacity - this.available;
    }

    public boolean put(FrameWithMetadata element){
        if(writePos >= capacity){
            writePos = 0;
        }

        elements[writePos] = element;
        writePos++;

        if(available < capacity){
            available++;
        }

        return true;
    }

    public FrameWithMetadata take() {
        if(available == 0){
            return null;
        }
        int nextSlot = writePos - available;
        if(nextSlot < 0){
            nextSlot += capacity;
        }
        FrameWithMetadata nextObj = elements[nextSlot];
        available--;
        return nextObj;
    }
}
