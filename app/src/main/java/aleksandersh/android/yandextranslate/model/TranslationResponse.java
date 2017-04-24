package aleksandersh.android.yandextranslate.model;

import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 22.04.2017.
 * <p>
 * Используется для возврата результата из асинхронных задач на получение перевода.
 */

public class TranslationResponse {
    private Translation mTranslation;
    private String mError;

    public TranslationResponse() {
    }

    public Translation getTranslation() {
        return mTranslation;
    }

    public void setTranslation(Translation translation) {
        mTranslation = translation;
    }

    public String getError() {
        return mError;
    }

    public void setError(String error) {
        mError = error;
    }
}
