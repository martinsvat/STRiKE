package logicStuff.learning.languageBias;


import java.util.function.Predicate;

/**
 * Created by martin.svatos on 19. 12. 2017.
 */
public interface LanguageBias<T> {

    public Predicate<T> predicate();
}
