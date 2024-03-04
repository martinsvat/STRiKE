package logicStuff.learning.languageBias;


import java.util.function.Predicate;

/**
 * Created by martin.svatos on 19. 12. 2017.
 */
public class NoneBias<T> implements LanguageBias<T> {
    @Override
    public Predicate<T> predicate() {
        return (t) -> true;
    }

    public static LanguageBias create() {
        return new NoneBias();
    }
}
