package aleksandersh.android.yandextranslate.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander on 18.04.2017.
 */

public class Translation {
    private long mId;
    private String mPrimaryLanguage;
    private String mTargetLanguage;
    private String mOriginalText;
    private String mTranslationText;
    private boolean mFavorite;
    private List<Dictionary> mDictionaryDefinitions = new ArrayList<>();

    public Translation() {
    }

    public Translation(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getPrimaryLanguage() {
        return mPrimaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.mPrimaryLanguage = primaryLanguage;
    }

    public String getTargetLanguage() {
        return mTargetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.mTargetLanguage = targetLanguage;
    }

    public String getOriginalText() {
        return mOriginalText;
    }

    public void setOriginalText(String originalText) {
        this.mOriginalText = originalText;
    }

    public String getTranslationText() {
        return mTranslationText;
    }

    public void setTranslationText(String translationText) {
        this.mTranslationText = translationText;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.mFavorite = favorite;
    }

    public List<Dictionary> getDictionaryDefinitions() {
        return mDictionaryDefinitions;
    }

    /**
     * Метод сравнивает изначальную модель по которой делался перевод.
     * Под моделью подразумевается направление перевода и переводимый текст.
     *
     * @param translation Перевод, с которым происходит сравнение.
     * @return {@code true}, если модели равны и {@code false} в ином случае.
     */
    public boolean modelEquals(Translation translation) {
        return mOriginalText.equals(translation.getOriginalText()) &&
                mPrimaryLanguage.equals(translation.getPrimaryLanguage()) &&
                mTargetLanguage.equals(translation.getTargetLanguage());
    }
}
