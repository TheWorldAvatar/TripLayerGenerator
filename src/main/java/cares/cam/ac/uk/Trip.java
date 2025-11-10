package cares.cam.ac.uk;

public class Trip {
    private double maxTime;
    private double minTime;
    private int tripIndex;

    public Trip(int tripIndex, double maxTime, double minTime) {
        this.tripIndex = tripIndex;
        this.maxTime = maxTime;
        this.minTime = minTime;
    }

    public int getTripIndex() {
        return tripIndex;
    }
}
