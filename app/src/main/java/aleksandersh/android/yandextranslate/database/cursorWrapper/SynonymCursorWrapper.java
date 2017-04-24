package aleksandersh.android.yandextranslate.database.cursorWrapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import aleksandersh.android.yandextranslate.database.TranslatorDbSchema.SynonymsTable;
import aleksandersh.android.yandextranslate.model.Synonym;

/**
 * Created by Alexander on 18.04.2017.
 * <p>
 * Расширение заводского курсора с добавлением парсинга в реальную модель.
 */

public class SynonymCursorWrapper extends CursorWrapper {
    public SynonymCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * Метод формирует на основе текущего состояния курсора модель.
     *
     * @return Полученный из курсора объект {@link Synonym}
     */
    public Synonym getSynonym() {
        Synonym synonym = new Synonym(getLong(getColumnIndex(SynonymsTable.Cols._ID)));
        synonym.setText(getString(getColumnIndex(SynonymsTable.Cols.TEXT)));
        synonym.setDictionaryId(getLong(getColumnIndex(SynonymsTable.Cols.DICTIONARY)));
        return synonym;
    }
}
