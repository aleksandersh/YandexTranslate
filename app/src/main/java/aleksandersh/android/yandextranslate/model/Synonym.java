package aleksandersh.android.yandextranslate.model;

/**
 * Created by Alexander on 18.04.2017.
 */

public class Synonym {
    private long mId;
    private long mDictionaryId;
    private String mText;

    public Synonym() {
    }

    public Synonym(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public long getDictionaryId() {
        return mDictionaryId;
    }

    public void setDictionaryId(long dictionaryId) {
        mDictionaryId = dictionaryId;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }
}
