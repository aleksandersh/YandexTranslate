package aleksandersh.android.yandextranslate.dto.translation;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by Alexander on 20.04.2017.
 *
 * Объект передачи данных, для получения списка языков от сервиса Яндекс переводчика.
 */

public class LanguagesDto {
    @SerializedName("langs")
    @Expose
    private Map<String, String> mLanguages;

    public Map<String, String> getLanguages() {
        return mLanguages;
    }

    public void setLanguages(Map<String, String> languages) {
        mLanguages = languages;
    }
}
