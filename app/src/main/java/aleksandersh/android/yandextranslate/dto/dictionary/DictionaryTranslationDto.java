package aleksandersh.android.yandextranslate.dto.dictionary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Alexander on 17.04.2017.
 *
 * Объект передачи данных перевода.
 */

public class DictionaryTranslationDto {
    // Текст.
    @SerializedName("text")
    @Expose
    private String mText;
    // Часть речи.
    @SerializedName("pos")
    @Expose
    private String mPartOfSpeech;
    // Синонимы.
    @SerializedName("syn")
    @Expose
    private List<DictionarySynonymDto> mSynonyms = null;
    // Смысловые значения.
    @SerializedName("mean")
    @Expose
    private List<DictionaryMeanDto> mMeans = null;
    // Примеры.
    @SerializedName("ex")
    @Expose
    private List<DictionaryExampleDto> mExamples = null;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public String getPartOfSpeech() {
        return mPartOfSpeech;
    }

    public void setPartOfSpeech(String pos) {
        this.mPartOfSpeech = pos;
    }

    public List<DictionarySynonymDto> getSynonyms() {
        return mSynonyms;
    }

    public void setSynonyms(List<DictionarySynonymDto> syn) {
        this.mSynonyms = syn;
    }

    public List<DictionaryMeanDto> getMeans() {
        return mMeans;
    }

    public void setMeans(List<DictionaryMeanDto> mean) {
        this.mMeans = mean;
    }

    public List<DictionaryExampleDto> getExamples() {
        return mExamples;
    }

    public void setExamples(List<DictionaryExampleDto> ex) {
        this.mExamples = ex;
    }
}
