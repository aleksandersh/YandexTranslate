package aleksandersh.android.yandextranslate.dto.dictionary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Alexander on 17.04.2017.
 *
 * Объект передачи данных словарой статьи.
 */

public class DictionaryDefinitionDto {
    // Текст.
    @SerializedName("text")
    @Expose
    private String mText;
    // Транскрипция.
    @SerializedName("ts")
    @Expose
    private String mTranscription;
    // Часть речи.
    @SerializedName("pos")
    @Expose
    private String mPartOfSpeech;
    // Массив переводов.
    @SerializedName("tr")
    @Expose
    private List<DictionaryTranslationDto> mTranslations = null;

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

    public List<DictionaryTranslationDto> getTranslations() {
        return mTranslations;
    }

    public void setTranslations(List<DictionaryTranslationDto> tr) {
        this.mTranslations = tr;
    }

    public String getTranscription() {
        return mTranscription;
    }

    public void setTranscription(String transcription) {
        this.mTranscription = transcription;
    }
}
