package chongxuocmanhinh.sound_plusplus;

/**
 * Created by L on 08/11/2016.
 */

import java.io.Serializable;

/**
 * Limiter is a constraint for MediaAdapter used when a row is "expanded"
 * e.g:From the artist list we expanded the artist name and limit songs for querying
 * that artist only
 */
public class Limiter implements Serializable{
    private static final long serialVersionUID = -4729694243900202614L;

    /***
     * The type of limiter.One of the MediaUtils.TYPE_ARTIST,MediaUtils.TYPE_ALBUM
     * ,MediaUtils.TYPE_GENRE
     */
    public final int type;

    /**
     * Each element will be given a separate view each representing a higher
     * different limiters.The first element is the broader-limiter,the last element
     * will be more specific.For example,an album limiter would look like :
     * { "Some Artist", "Some Album" }
     *
     */
    public final String[] names;

    /**
     * The data for the limiter. This varies according to the type of the
     * limiter.
     */
    public final Object data;

    public Limiter(int type, String[] names, Object data) {
        this.type = type;
        this.names = names;
        this.data = data;
    }
}
