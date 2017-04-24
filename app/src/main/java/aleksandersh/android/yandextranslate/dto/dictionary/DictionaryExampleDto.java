package aleksandersh.android.yandextranslate.dto.dictionary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Alexander on 17.04.2017.
 *
 * Объект передачи данных примера.
 */

public class DictionaryExampleDto {
    @SerializedName("text")
    @Expose
    private String mText;
    @SerializedName("tr")
    @Expose
    private List<DictionaryTranslationDto> mTranslations = null;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public List<DictionaryTranslationDto> getTranslations() {
        return mTranslations;
    }

    public void setTranslations(List<DictionaryTranslationDto> tr) {
        this.mTranslations = tr;
    }
}
