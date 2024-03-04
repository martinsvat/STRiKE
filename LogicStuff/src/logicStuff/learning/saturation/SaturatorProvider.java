package logicStuff.learning.saturation;


/**
 * Created by martin.svatos on 1. 2. 2018.
 */
public interface SaturatorProvider<T, K extends Saturator<T>> {

    public K getSaturator();

    public boolean isUpdatable();
}
