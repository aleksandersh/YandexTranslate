package aleksandersh.android.yandextranslate.dto.translation;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Alexander on 14.04.2017.
 *
 * Объект передачи данных, для получения ответа от сервиса Яндекс переводчика.
 */

public class TranslationDto {
    // Код возврата.
    @SerializedName("code")
    @Expose
    private Integer mCode;
    // Направление перевода.
    @SerializedName("lang")
    @Expose
    private String mLang;
    // Текст.
    @SerializedName("text")
    @Expose
    private List<String> mTexts = null;

    public Integer getCode() {
        return mCode;
    }

    public void setCode(Integer code) {
        this.mCode = code;
    }

    public String getLang() {
        return mLang;
    }

    public void setLang(String lang) {
        this.mLang = lang;
    }

    public List<String> getText() {
        return mTexts;
    }

    public void setText(List<String> text) {
        this.mTexts = text;
    }
}
