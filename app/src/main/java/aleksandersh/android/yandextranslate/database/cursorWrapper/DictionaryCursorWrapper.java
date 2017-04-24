package aleksandersh.android.yandextranslate.database.cursorWrapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import aleksandersh.android.yandextranslate.database.TranslatorDbSchema.DictionaryTable;
import aleksandersh.android.yandextranslate.model.Dictionary;

/**
 * Created by Alexander on 18.04.2017.
 *
 * Расширение заводского курсора с добавлением парсинга в реальную модель.
 */

public class DictionaryCursorWrapper extends CursorWrapper {
    public DictionaryCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * Метод формирует на основе текущего состояния курсора модель.
     *
     * @return Полученный из курсора объект {@link Dictionary}
     */
    public Dictionary getDictionary() {
        Dictionary dictionary = new Dictionary(getLong(getColumnIndex(DictionaryTable.Cols._ID)));
        dictionary.setText(getString(getColumnIndex(DictionaryTable.Cols.TEXT)));
        dictionary.setTranslationText(getString(getColumnIndex(DictionaryTable.Cols.TRANSLATION_TEXT)));
        dictionary.setTranscription(getString(getColumnIndex(DictionaryTable.Cols.TRANSCRIPTION)));
        dictionary.setPartOfSpeech(getString(getColumnIndex(DictionaryTable.Cols.PART_OF_SPEECH)));
        dictionary.setTranslationId(getLong(getColumnIndex(DictionaryTable.Cols.TRANSLATION)));
        return dictionary;
    }
}
