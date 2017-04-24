package aleksandersh.android.yandextranslate.database.cursorWrapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import aleksandersh.android.yandextranslate.database.TranslatorDbSchema;
import aleksandersh.android.yandextranslate.model.Language;

/**
 * Created by Alexander on 21.04.2017.
 *
 * Расширение заводского курсора с добавлением парсинга в реальную модель.
 */

public class LanguageCursorWrapper extends CursorWrapper {
    public LanguageCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Language getLanguage() {
        Language language =
                new Language(getLong(getColumnIndex(TranslatorDbSchema.LanguagesTable.Cols._ID)));
        language.setSign(getString(getColumnIndex(TranslatorDbSchema.LanguagesTable.Cols.SIGN)));
        language.setText(getString(getColumnIndex(TranslatorDbSchema.LanguagesTable.Cols.TEXT)));
        return language;
    }
}
