package at.inwegoproject.inwego.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Waypoint domain class
 */
public class Waypoint implements Parcelable {

    private int id;
    private String firstName;
    private String lastName;
    private String number;
    private double latitude;
    private double longitude;
    private String address;
    private int routeId;

    public Waypoint(int id, String firstName, String lastName, String number, double latitude, double longitude, String address, int routeId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.number = number;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.routeId = routeId;
    }

    public Waypoint() {
    }

    public Waypoint(Parcel source) {
        id = source.readInt();
        firstName = source.readString();
        lastName = source.readString();
        number = source.readString();
        latitude = source.readDouble();
        longitude = source.readDouble();
        address = source.readString();
        routeId = source.readInt();
    }

    public static final Creator<Waypoint> CREATOR = new Creator<Waypoint>() {
        @Override
        public Waypoint createFromParcel(Parcel in) {
            return new Waypoint(in);
        }

        @Override
        public Waypoint[] newArray(int size) {
            return new Waypoint[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Need to be parcable to send between activities as intent extra
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(number);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(address);
        dest.writeInt(routeId);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
