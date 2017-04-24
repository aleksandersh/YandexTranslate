package aleksandersh.android.yandextranslate.database.cursorWrapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import aleksandersh.android.yandextranslate.database.TranslatorDbSchema.TranslationsTable;
import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 18.04.2017.
 *
 * Расширение заводского курсора с добавлением парсинга в реальную модель.
 */

public class TranslationCursorWrapper extends CursorWrapper {
    public TranslationCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * Метод формирует на основе текущего состояния курсора модель.
     *
     * @return Полученный из курсора объект {@link Translation}
     */
    public Translation getTranslation() {
        Translation translation = new Translation(getLong(getColumnIndex(
                TranslationsTable.Cols._ID)));
        translation.setPrimaryLanguage(getString(getColumnIndex(
                TranslationsTable.Cols.PRIMARY_LANGUAGE)));
        translation.setTargetLanguage(getString(getColumnIndex(
                TranslationsTable.Cols.TARGET_LANGUAGE)));
        translation.setOriginalText(getString(getColumnIndex(
                TranslationsTable.Cols.ORIGINAL_TEXT)));
        translation.setTranslationText(getString(getColumnIndex(
                TranslationsTable.Cols.TRANSLATION_TEXT)));
        // Хранение типа boolean в SQLite не поддерживается, поэтому значение хранится как 0 или 1.
        translation.setFavorite(getLong(getColumnIndex(
                TranslationsTable.Cols.FAVORITE)) == 1);

        return translation;
    }
}
