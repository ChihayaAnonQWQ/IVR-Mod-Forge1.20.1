package net.hulan.ivr.utils;

/** Client train state consulted by the shared Train.calculateCar hook. */
public interface TrainRenderSkipState {
    boolean ivr$skipDistantCars();
}
