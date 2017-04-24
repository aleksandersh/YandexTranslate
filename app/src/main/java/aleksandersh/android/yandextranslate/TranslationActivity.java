package aleksandersh.android.yandextranslate;

import aleksandersh.android.yandextranslate.model.TranslationState;

/**
 * Created by Alexander on 23.04.2017.
 * <p>
 * Интерфейс активности, которая занимается переводом.
 */

public interface TranslationActivity {
    /**
     * Показывает запрашиваемый перевод.
     *
     * @param translationState Параметры перевода, который необходимо показать.
     */
    void showTranslation(TranslationState translationState);
}
