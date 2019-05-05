package at.inwegoproject.inwego.domain;

/**
 * Route details domain class
 */
public class RouteDetails {
    public String readableDistance;
    public long distanceMeters;
    public String readableDuration;
    public long durationSecounds;

    public RouteDetails(String readableDistance, long distanceMeters, String readableDuration, long durationSecounds) {
        this.readableDistance = readableDistance;
        this.distanceMeters = distanceMeters;
        this.readableDuration = readableDuration;
        this.durationSecounds = durationSecounds;
    }

    public RouteDetails() {
    }

    public String getReadableDistance() {
        return readableDistance;
    }

    public void setReadableDistance(String readableDistance) {
        this.readableDistance = readableDistance;
    }

    public long getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(long distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getReadableDuration() {
        return readableDuration;
    }

    public void setReadableDuration(String readableDuration) {
        this.readableDuration = readableDuration;
    }

    public long getDurationSecounds() {
        return durationSecounds;
    }

    public void setDurationSecounds(long durationSecounds) {
        this.durationSecounds = durationSecounds;
    }
}
