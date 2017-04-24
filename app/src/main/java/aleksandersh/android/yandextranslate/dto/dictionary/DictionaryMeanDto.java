package aleksandersh.android.yandextranslate.dto.dictionary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Alexander on 17.04.2017.
 *
 * Объект передачи данных смысловых значений.
 */

public class DictionaryMeanDto {
    @SerializedName("text")
    @Expose
    private String mText;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }
}
