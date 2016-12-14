package chongxuocmanhinh.sound_plusplus;

/**
 * Created by L on 14/12/2016.
 */
public class TimestampedObject {
    public long uptime;
    public Object object;

    /**
     * Encapsulates given object and marks the creation timestamp
     * in nanoseconds
     */
    public TimestampedObject(Object object) {
        this.object = object;
        this.uptime = System.nanoTime();
    }
}
