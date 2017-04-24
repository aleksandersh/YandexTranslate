package aleksandersh.android.yandextranslate.model;

/**
 * Created by Alexander on 13.04.2017.
 *
 * Объект данного класса содержит данные о переводе.
 */

public class TranslationState {
    // Исходный текст.
    private String mText;
    // Исходный язык.
    private String mLanguage;
    // Язык перевода.
    private String mTranslationLanguage;
    // Показатель, что перевод в избранном.
    private boolean mFavorite;
    // Исходный язык.
    private String mLanguageText;
    // Язык перевода.
    private String mTranslationLanguageText;

    public TranslationState(String text, String language, String translationLanguage) {
        mText = text;
        mLanguage = language;
        mTranslationLanguage = translationLanguage;
    }

    public String getText() {
        return mText;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public String getTranslationLanguage() {
        return mTranslationLanguage;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean favorite) {
        mFavorite = favorite;
    }

    public String getLanguageText() {
        return mLanguageText;
    }

    public void setLanguageText(String languageText) {
        mLanguageText = languageText;
    }

    public String getTranslationLanguageText() {
        return mTranslationLanguageText;
    }

    public void setTranslationLanguageText(String translationLanguageText) {
        mTranslationLanguageText = translationLanguageText;
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof TranslationState) {
            return mText.equals(((TranslationState) anObject).getText())
                    && mLanguage.equals(((TranslationState) anObject).getLanguage())
                    && mTranslationLanguage.equals(
                            ((TranslationState) anObject).getTranslationLanguage());
        }
        return false;
    }
}
