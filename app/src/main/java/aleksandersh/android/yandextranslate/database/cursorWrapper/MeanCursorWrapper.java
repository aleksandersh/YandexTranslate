package aleksandersh.android.yandextranslate.database.cursorWrapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import aleksandersh.android.yandextranslate.database.TranslatorDbSchema.MeansTable;
import aleksandersh.android.yandextranslate.model.Mean;

/**
 * Created by Alexander on 18.04.2017.
 *
 * Расширение заводского курсора с добавлением парсинга в реальную модель.
 */

public class MeanCursorWrapper extends CursorWrapper {
    public MeanCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * Метод формирует на основе текущего состояния курсора модель.
     *
     * @return Полученный из курсора объект {@link Mean}
     */
    public Mean getMean() {
        Mean mean = new Mean(getLong(getColumnIndex(MeansTable.Cols._ID)));
        mean.setText(getString(getColumnIndex(MeansTable.Cols.TEXT)));
        mean.setDictionaryId(getLong(getColumnIndex(MeansTable.Cols.DICTIONARY)));
        return mean;
    }
}
