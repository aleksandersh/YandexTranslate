package aleksandersh.android.yandextranslate.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Alexander on 24.04.2017.
 */

public class ErrorDto {
    // Код возврата.
    @SerializedName("code")
    @Expose
    private Integer mCode;

    public Integer getCode() {
        return mCode;
    }

    public void setCode(Integer code) {
        mCode = code;
    }
}
