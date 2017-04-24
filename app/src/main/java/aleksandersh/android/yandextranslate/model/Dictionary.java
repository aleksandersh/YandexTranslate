package aleksandersh.android.yandextranslate.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander on 18.04.2017.
 */

public class Dictionary {
    private long mId;
    private long mTranslationId;
    private String mText;
    private String mTranslationText;
    private String mTranscription;
    private String mPartOfSpeech;
    private List<Synonym> mSynonyms = new ArrayList<>();
    private List<Mean> mMeans = new ArrayList<>();

    public Dictionary() {
    }

    public Dictionary(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public long getTranslationId() {
        return mTranslationId;
    }

    public void setTranslationId(long translationId) {
        mTranslationId = translationId;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getTranslationText() {
        return mTranslationText;
    }

    public void setTranslationText(String translation) {
        mTranslationText = translation;
    }

    public String getTranscription() {
        return mTranscription;
    }

    public void setTranscription(String transcription) {
        mTranscription = transcription;
    }

    public String getPartOfSpeech() {
        return mPartOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        mPartOfSpeech = partOfSpeech;
    }

    public List<Synonym> getSynonyms() {
        return mSynonyms;
    }

    public List<Mean> getMeans() {
        return mMeans;
    }
}
