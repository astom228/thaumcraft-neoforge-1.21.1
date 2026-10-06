package thaumcraft.api.research.theorycraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Реестр помощников и карточек теоретизирования (ключ — имя класса, как в оригинале). Заполняется при запуске. */
public class TheorycraftManager {
    public static final Map<String, ITheorycraftAid> aids = new ConcurrentHashMap<>();
    public static final Map<String, Class<? extends TheorycraftCard>> cards = new ConcurrentHashMap<>();

    public static void registerAid(ITheorycraftAid aid) {
        aids.putIfAbsent(aid.getClass().getName(), aid);
    }

    public static void registerCard(Class<? extends TheorycraftCard> cardClass) {
        cards.putIfAbsent(cardClass.getName(), cardClass);
    }
}
