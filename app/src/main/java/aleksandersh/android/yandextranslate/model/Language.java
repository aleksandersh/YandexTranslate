package aleksandersh.android.yandextranslate.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Alexander on 20.04.2017.
 * <p>
 * Объект представляет собой язык, реализует интерфейс {@link Parcelable} для передачи его между
 * разными активностями.
 */

public class Language implements Parcelable {
    private long mId;
    // Полное обозначение языка.
    private String mText;
    // Уникальное буквенное обозначение языка.
    private String mSign;

    public Language() {
    }

    public Language(long id) {
        mId = id;
    }

    public Language(Parcel in) {
        String[] data = new String[3];
        in.readStringArray(data);
        mId = Long.decode(data[0]);
        mText = data[1];
        mSign = data[2];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{String.valueOf(mId), mText, mSign});
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getSign() {
        return mSign;
    }

    public void setSign(String sign) {
        mSign = sign;
    }

    public static final Parcelable.Creator<Language> CREATOR = new Parcelable.Creator<Language>() {
        @Override
        public Language createFromParcel(Parcel source) {
            return new Language(source);
        }

        @Override
        public Language[] newArray(int size) {
            return new Language[size];
        }
    };
}
