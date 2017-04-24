package aleksandersh.android.yandextranslate.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import aleksandersh.android.yandextranslate.model.TranslationState;
import aleksandersh.android.yandextranslate.database.TranslatorDbHelper;
import aleksandersh.android.yandextranslate.database.TranslatorDbSchema;
import aleksandersh.android.yandextranslate.database.cursorWrapper.DictionaryCursorWrapper;
import aleksandersh.android.yandextranslate.database.cursorWrapper.LanguageCursorWrapper;
import aleksandersh.android.yandextranslate.database.cursorWrapper.MeanCursorWrapper;
import aleksandersh.android.yandextranslate.database.cursorWrapper.SynonymCursorWrapper;
import aleksandersh.android.yandextranslate.database.cursorWrapper.TranslationCursorWrapper;
import aleksandersh.android.yandextranslate.model.Dictionary;
import aleksandersh.android.yandextranslate.model.Language;
import aleksandersh.android.yandextranslate.model.Mean;
import aleksandersh.android.yandextranslate.model.Synonym;
import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 18.04.2017.
 * <p>
 * Класс отвечает за доступ к базе данных, чтение и запись в нее. В рамках приложения будет
 * только один экземпляр, для этого класс сделан по шаблону синглтона.
 */

public class TranslationDao {
    private static final String TAG = "TranslationDao";
    // Максимальное число записей в истории.
    private static final int MAX_HISTORY = 50;
    // Путь к файлу со списком языков.
    private static final String LANGUAGES_FILE_PATH = "languages.txt";
    private static TranslationDao sTranslationDao;

    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static TranslationDao get(Context context) {
        if (sTranslationDao == null)
            sTranslationDao = new TranslationDao(context);
        return sTranslationDao;
    }

    private TranslationDao(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new TranslatorDbHelper(context).getWritableDatabase();
    }

    /**
     * Метод возвращает объект {@link Translation}, если находит его в базе данных. Последовательно
     * делает дополнительные запросы в базу данных, для получения данных словаря.
     *
     * @param id Id, под которым в базе данных находится перевод.
     * @return Объект перевода из базы данных, если такой отсутствует, возвращается {@code null}.
     */
    public Translation getTranslationById(long id) {
        Translation translation = null;

        mDatabase.beginTransaction();
        try {
            String selection = TranslatorDbSchema.TranslationsTable.Cols._ID + " = ?";
            String[] selectionArgs = new String[]{String.valueOf(id)};
            translation = getTranslationByQuery(selection, selectionArgs, true);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
        return translation;
    }

    /**
     * Возвращает перевод из базы данных по запросу.
     *
     * @param request Запрос с данными, для получения перевода.
     * @return Перевод, найденный в базе данных. Если в бд нет подобного перевода,
     * возвращается {@code null}
     */
    public Translation getTranslationByRequest(TranslationState request) {
        Translation translation = null;

        mDatabase.beginTransaction();
        try {
            String selection = TranslatorDbSchema.TranslationsTable.Cols.ORIGINAL_TEXT +
                    " = ? AND " + TranslatorDbSchema.TranslationsTable.Cols.PRIMARY_LANGUAGE +
                    " = ? AND " + TranslatorDbSchema.TranslationsTable.Cols.TARGET_LANGUAGE +
                    " = ?";
            String[] selectionArgs = {
                    request.getText(),
                    request.getLanguage(),
                    request.getTranslationLanguage()
            };
            translation = getTranslationByQuery(selection, selectionArgs, true);
        } finally {
            mDatabase.endTransaction();
        }

        return translation;
    }

    /**
     * Получение переводов, которые есть в истории, сортированных по дате перевода.
     *
     * @return Список переводов.
     */
    public List<Translation> getHistory() {
        ArrayList<Translation> translations = new ArrayList<>();

        TranslationCursorWrapper cursorTranslation = getHistoryCursor(null, null);
        try {
            cursorTranslation.moveToFirst();
            while (!cursorTranslation.isAfterLast()) {
                translations.add(cursorTranslation.getTranslation());
                cursorTranslation.moveToNext();
            }
        } finally {
            cursorTranslation.close();
        }
        return translations;
    }

    /**
     * Получение перевода по индексу истории.
     *
     * @param index Индекс записи, которую необходимо получить. Отсчет ведется с 0.
     * @return Перевод, который находится на заданном индексе в истории. Если индекс за границами
     * истории, возвращается {@code null}
     */
    public Translation getHistoryEntry(int index) {
        TranslationCursorWrapper cursorTranslation = getHistoryCursor(null, null);
        try {
            if (cursorTranslation.moveToPosition(index))
                return cursorTranslation.getTranslation();
        } finally {
            cursorTranslation.close();
        }
        return null;
    }

    /**
     * Добавление новой записи в историю.
     *
     * @param translation Перевод, который необходимо добавить в историю.
     */
    public void addHistoryEntry(Translation translation) {
        // Если добавляемый в историю перевод и так является последним в истории, добавлять
        // новую запись не нужно.
        Translation lastTranslation = getHistoryEntry(0);
        if (lastTranslation != null && translation.modelEquals(lastTranslation))
            return;

        mDatabase.beginTransaction();
        try {
            prepareHistory();

            // Сначала следует проверить, может быть такой перевод уже содержится в базе данных.
            String selection = TranslatorDbSchema.TranslationsTable.Cols.ORIGINAL_TEXT +
                    " = ? AND " + TranslatorDbSchema.TranslationsTable.Cols.PRIMARY_LANGUAGE +
                    " = ? AND " + TranslatorDbSchema.TranslationsTable.Cols.TARGET_LANGUAGE +
                    " = ?";
            String[] selectionArgs = {
                    translation.getOriginalText(),
                    translation.getPrimaryLanguage(),
                    translation.getTargetLanguage()
            };
            Translation translationDb = getTranslationByQuery(selection, selectionArgs, false);
            long translationId;

            // Если перевод не найден, его следует добавить.
            if (translationDb == null) {
                addTranslation(translation);
                translationId = translation.getId();
            } else {
                translationId = translationDb.getId();
            }
            // Добавление записи в историю.
            try {
                mDatabase.insert(
                        TranslatorDbSchema.HistoryTable.NAME,
                        null,
                        getHistoryEntryContentValues(translationId, new Date())
                );
            } catch (SQLiteConstraintException e) {
                Log.e(TAG, "Integrity constraint was violated.");
                return;
            }

            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Получение списка избранных переводов.
     *
     * @return Список избранного.
     */
    public List<Translation> getFavorites() {
        ArrayList<Translation> translations = new ArrayList<>();
        TranslationCursorWrapper cursorTranslation = new TranslationCursorWrapper(mDatabase.query(
                TranslatorDbSchema.TranslationsTable.NAME,
                null,
                TranslatorDbSchema.TranslationsTable.Cols.FAVORITE + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        ));
        try {
            cursorTranslation.moveToFirst();
            while (!cursorTranslation.isAfterLast()) {
                translations.add(cursorTranslation.getTranslation());
                cursorTranslation.moveToNext();
            }
        } finally {
            cursorTranslation.close();
        }
        return translations;
    }

    /**
     * Установка переводу свойства избранного, если пометка снимается и перевода не будет в
     * истории, запись о нем удалится. Перевод ищется по параметрам запроса.
     *
     * @param translationState Параметры, по которым следует установить признак.
     */
    public void setFavorite(TranslationState translationState) {
        boolean favorite = translationState.isFavorite();
        mDatabase.beginTransaction();
        try {
            String selection = TranslatorDbSchema.TranslationsTable.Cols.ORIGINAL_TEXT + " = ? AND " +
                    TranslatorDbSchema.TranslationsTable.Cols.PRIMARY_LANGUAGE + " = ? AND " +
                    TranslatorDbSchema.TranslationsTable.Cols.TARGET_LANGUAGE + " = ?";
            String[] selectionArgs = new String[]{
                    translationState.getText(),
                    translationState.getLanguage(),
                    translationState.getTranslationLanguage()
            };

            boolean removeTranslation = false;
            if (!favorite) {
                TranslationCursorWrapper translationCursorWrapper =
                        getHistoryCursor(selection, selectionArgs);
                try {
                    if (translationCursorWrapper.getCount() == 0)
                        removeTranslation = true;
                } finally {
                    translationCursorWrapper.close();
                }
            }
            // Если пометка избранного снимается и перевода нет в истории, тогда он удаляется.
            if (removeTranslation)
                removeTranslation(selection, selectionArgs);
            else
                setFavoriteByQuery(selection, selectionArgs, favorite);

            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Метод снимает у заданных переводов флаг того, что они в избранном. Если переводы отсутствуют
     * в истории, они удаляются.
     *
     * @param idList Массив идентификаторов обрабатываемых переводов.
     */
    public void removeFromFavorites(List<String> idList) {
        // Подготовка параметров для условия в запросе.
        String[] selectionArgs = idList.toArray(new String[idList.size()]);
        List<String> parameters = new ArrayList<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            parameters.add("?");
        }
        String selection = TranslatorDbSchema.TranslationsTable.NAME + "." +
                TranslatorDbSchema.TranslationsTable.Cols._ID +
                " IN (" + TextUtils.join(",", parameters) + ")";

        mDatabase.beginTransaction();
        try {
            // Массив будет содержать список id, переводы по которым есть в истории.
            ArrayList<String> toUpdate = new ArrayList<>();

            // Получение переводов, которые есть в истории.
            TranslationCursorWrapper translationCursor = getHistoryCursor(selection, selectionArgs);
            try {
                translationCursor.moveToFirst();
                while (!translationCursor.isAfterLast()) {
                    String translationId = String.valueOf(translationCursor.getLong(
                            translationCursor.getColumnIndex(
                                    TranslatorDbSchema.TranslationsTable.Cols._ID)));
                    if (!toUpdate.contains(translationId))
                        toUpdate.add(translationId);
                    translationCursor.moveToNext();
                }
            } finally {
                translationCursor.close();
            }

            // Чтобы не делать лишнюю работу введен флаг, обозначающий, что условия поменялись.
            boolean reloadSelection = false;

            // Если есть записи на обновление.
            if (toUpdate.size() > 0) {
                // Из основного массива удаляются Id, по которым есть записи в истории. Тогда все
                // записи с Id из этого массива можно будет удалять.
                idList.removeAll(toUpdate);
                // Из основного массива удалены записи и условия необходимо поменять.
                reloadSelection = true;

                String[] selectionArgsToUpdate = toUpdate.toArray(new String[toUpdate.size()]);
                List<String> parametersToUpdate = new ArrayList<>(toUpdate.size());
                for (int i = 0; i < toUpdate.size(); i++) {
                    parametersToUpdate.add("?");
                }
                String selectionToUpdate = TranslatorDbSchema.TranslationsTable.NAME + "." +
                        TranslatorDbSchema.TranslationsTable.Cols._ID +
                        " IN (" + TextUtils.join(",", parametersToUpdate) + ")";
                // Выолнение запроса на обновление.
                setFavoriteByQuery(selectionToUpdate, selectionArgsToUpdate, false);
            }

            // Если есть записи на удаление.
            if (idList.size() > 0) {
                if (reloadSelection) {
                    selectionArgs = idList.toArray(new String[idList.size()]);
                    parameters.clear();
                    for (int i = 0; i < idList.size(); i++) {
                        parameters.add("?");
                    }
                    selection = TranslatorDbSchema.TranslationsTable.NAME + "." +
                            TranslatorDbSchema.TranslationsTable.Cols._ID +
                            " IN (" + TextUtils.join(",", parameters) + ")";
                }
                removeTranslation(selection, selectionArgs);
            }

            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Метод получает полный экземпляр языка по его обозначению.
     *
     * @param sign Обозначение языка.
     * @return Найденный экземпляр языка, если в бд отсутствуют языки с заданным обозначением,
     * возвращается {@code null}.
     */
    public Language getLanguageBySign(String sign) {
        Language language = null;
        String selection = TranslatorDbSchema.LanguagesTable.Cols.SIGN + " = ?";
        String[] selectionArgs = new String[]{sign};
        LanguageCursorWrapper cursorWrapper = new LanguageCursorWrapper(mDatabase.query(
                TranslatorDbSchema.LanguagesTable.NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        ));
        try {
            if (cursorWrapper.getCount() > 0) {
                cursorWrapper.moveToFirst();
                language = cursorWrapper.getLanguage();
            }
        } finally {
            cursorWrapper.close();
        }
        return language;
    }

    /**
     * Получение списка доступных языков.
     *
     * @return Список языков.
     */
    public List<Language> getLanguages() {
        ArrayList<Language> languages = new ArrayList<>();
        LanguageCursorWrapper cursorLanguage = new LanguageCursorWrapper(mDatabase.query(
                TranslatorDbSchema.LanguagesTable.NAME, null, null, null, null, null, null));
        try {
            cursorLanguage.moveToFirst();
            while (!cursorLanguage.isAfterLast()) {
                languages.add(cursorLanguage.getLanguage());
                cursorLanguage.moveToNext();
            }
        } finally {
            cursorLanguage.close();
        }
        return languages;
    }

    /**
     * Перезагрузка языков в базу данных. Перед загрузкой все предыдущие языки из базы данных
     * удаляются. Такой функционал можно использовать, если необходимо обновить список
     * языков из сервиса Яндекс переводчика.
     *
     * @param languages Список языков, который нужно загрузить.
     */
    public void reloadLanguages(List<Language> languages) {
        mDatabase.beginTransaction();

        try {
            // Удаление всех имеющихся языков.
            mDatabase.delete(TranslatorDbSchema.LanguagesTable.NAME, null, null);
            // Добавление всех языков из списка.
            for (Language language : languages) {
                mDatabase.insert(
                        TranslatorDbSchema.LanguagesTable.NAME,
                        null,
                        getLanguageContentValues(language)
                );
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Получение перевода из базы данных.
     *
     * @param selection Условие выборки перевода.
     * @param selectionArgs Аругменты условия перевода.
     * @param fillDictionary Если {@code true}, полученный экземпляр перевода будет заполнен
     *                       данными из словаря, в ином случае {@code false}.
     * @return
     */
    private Translation getTranslationByQuery(String selection,
                                              String[] selectionArgs,
                                              boolean fillDictionary) {
        Translation translation = null;
        TranslationCursorWrapper translationCursor = new TranslationCursorWrapper(
                mDatabase.query(
                        TranslatorDbSchema.TranslationsTable.NAME,
                        null,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null
                ));
        try {
            // Если переводов нет, возвращается null.
            if (translationCursor.getCount() == 0)
                return null;
            // Перемещение курсора на первую запись.
            translationCursor.moveToFirst();
            translation = translationCursor.getTranslation();
        } finally {
            // Закрытие курсора.
            translationCursor.close();
        }
        if (fillDictionary)
            fillDictionaryDefinitions(translation);

        return translation;
    }

    private void addTranslation(Translation translation) {
        // Добавление перевода.
        long translationId = mDatabase.insert(
                TranslatorDbSchema.TranslationsTable.NAME,
                null,
                getTranslationContentValues(translation)
        );
        // Если во время добавления произошла ошибка, id равен -1, SQLiteDatabase сам
        // логирует этот вариант.
        if (translationId == -1)
            return;
        translation.setId(translationId);

        // Добавление статей словаря для перевода.
        for (Dictionary dictionary : translation.getDictionaryDefinitions()) {
            // Так как на этот момент уже имеется Id перевода из базы данных, его следует
            // установить для словарей перед их добавлением в бд.
            dictionary.setTranslationId(translationId);
            long dictionaryId = mDatabase.insert(
                    TranslatorDbSchema.DictionaryTable.NAME,
                    null,
                    getDictionaryContentValues(dictionary)
            );
            if (dictionaryId == -1)
                return;
            dictionary.setId(dictionaryId);

            // Добавление синонимов.
            for (Synonym synonym : dictionary.getSynonyms()) {
                synonym.setDictionaryId(dictionaryId);
                long synonymId = mDatabase.insert(
                        TranslatorDbSchema.SynonymsTable.NAME,
                        null,
                        getSynonymContentValues(synonym)
                );
                if (synonymId == -1)
                    return;
                synonym.setId(synonymId);
            }

            // Добавление значений.
            for (Mean mean : dictionary.getMeans()) {
                mean.setDictionaryId(dictionaryId);
                long meanId = mDatabase.insert(
                        TranslatorDbSchema.MeansTable.NAME,
                        null,
                        getMeanContentValues(mean)
                );
                if (meanId == -1)
                    return;
                mean.setId(meanId);
            }
        }
    }

    private int removeTranslation(String selection, String[] selectionArgs) {
        return mDatabase.delete(
                TranslatorDbSchema.TranslationsTable.NAME,
                selection,
                selectionArgs
        );
    }

    /**
     * Заполняет экземпляр перевода данными из словаря.
     *
     * @param translation Экземпляр перевода, который нужно заполнить.
     */
    private void fillDictionaryDefinitions(Translation translation) {
        List<Dictionary> dictionaryList = translation.getDictionaryDefinitions();
        dictionaryList.clear();

        // Получение словарных статей для перевода.
        Cursor cursorDictionary = mDatabase.query(
                TranslatorDbSchema.DictionaryTable.NAME,
                null,
                TranslatorDbSchema.DictionaryTable.Cols.TRANSLATION + " = ?", // отбор по переводу
                new String[]{String.valueOf(translation.getId())},
                null,
                null,
                null
        );
        try {
            // Если словарей для текущего перевода нет, возвращается простой перевод.
            if (cursorDictionary.getCount() == 0)
                return;
            DictionaryCursorWrapper dictionaryCursorWrapper =
                    new DictionaryCursorWrapper(cursorDictionary);
            dictionaryCursorWrapper.moveToFirst();
            while (!dictionaryCursorWrapper.isAfterLast()) {
                dictionaryList.add(dictionaryCursorWrapper.getDictionary());
                dictionaryCursorWrapper.moveToNext();
            }
        } finally {
            cursorDictionary.close();
        }

        for (Dictionary dictionary : dictionaryList) {
            List<Synonym> synonymList = dictionary.getSynonyms();
            synonymList.clear();

            // Получение синонимов.
            Cursor cursorSynonym = mDatabase.query(
                    TranslatorDbSchema.SynonymsTable.NAME,
                    null,
                    TranslatorDbSchema.SynonymsTable.Cols.DICTIONARY + " = ?",
                    new String[]{String.valueOf(dictionary.getId())},
                    null,
                    null,
                    null
            );
            try {
                cursorSynonym.moveToFirst();
                while (!cursorSynonym.isAfterLast()) {
                    SynonymCursorWrapper synonymCursorWrapper =
                            new SynonymCursorWrapper(cursorSynonym);
                    synonymList.add(synonymCursorWrapper.getSynonym());
                    synonymCursorWrapper.moveToNext();
                }
            } finally {
                cursorSynonym.close();
            }

            List<Mean> meanList = dictionary.getMeans();
            meanList.clear();

            // Получение значений.
            Cursor cursorMean = mDatabase.query(
                    TranslatorDbSchema.MeansTable.NAME,
                    null,
                    TranslatorDbSchema.MeansTable.Cols.DICTIONARY + " = ?",
                    new String[]{String.valueOf(dictionary.getId())},
                    null,
                    null,
                    null
            );
            try {
                cursorMean.moveToFirst();
                while (!cursorMean.isAfterLast()) {
                    MeanCursorWrapper meanCursorWrapper = new MeanCursorWrapper(cursorMean);
                    meanList.add(meanCursorWrapper.getMean());
                    meanCursorWrapper.moveToNext();
                }
            } finally {
                cursorMean.close();
            }
        }
    }

    /**
     * Выполняет запрос на установку флага Избранного переводам.
     *
     * @param selection     Условия для определения переводов.
     * @param selectionArgs Аргументы условий.
     * @param favorite      Флаг избранного.
     * @return Количество обновленных записей.
     */
    private int setFavoriteByQuery(String selection, String[] selectionArgs, boolean favorite) {
        return mDatabase.update(
                TranslatorDbSchema.TranslationsTable.NAME,
                getFavoriteContentValues(favorite),
                selection,
                selectionArgs
        );
    }

    /**
     * Необходимо выполнять перед добавлением новой записи в историю. Метод проверяет количество
     * существующих записей и если оно превышает максимальное количество, удаляет старшие.
     */
    private void prepareHistory() {
        Cursor cursor = mDatabase.query(
                TranslatorDbSchema.HistoryTable.NAME,
                null,
                null,
                null,
                null,
                null,
                TranslatorDbSchema.HistoryTable.Cols.DATE
        );
        try {
            // Получение количества записей на удаление.
            int deleteCount = cursor.getCount() - MAX_HISTORY + 1;
            cursor.moveToFirst();
            while (deleteCount > 0 && !cursor.isAfterLast()) {
                // Флаг удаления перевода.
                boolean deleteTranslation = false;
                // Идентификатор записи истории.
                long historyEntryId = cursor.getLong(
                        cursor.getColumnIndex(TranslatorDbSchema.HistoryTable.Cols._ID));
                // Идентификатор перевода.
                long translationId = cursor.getLong(
                        cursor.getColumnIndex(TranslatorDbSchema.HistoryTable.Cols.TRANSLATION));

                // Проверка, существуют ли еще записи в истории, которые ссылаются на этот перевод.
                Cursor cursorTranslationInHistory = mDatabase.query(
                        TranslatorDbSchema.HistoryTable.NAME,
                        null,
                        TranslatorDbSchema.HistoryTable.Cols.TRANSLATION + " = ?",
                        new String[]{String.valueOf(translationId)},
                        null,
                        null,
                        null
                );
                try {
                    deleteTranslation = cursorTranslationInHistory.getCount() <= 1;
                } finally {
                    cursorTranslationInHistory.close();
                }

                // Удаление записи в истории.
                mDatabase.delete(
                        TranslatorDbSchema.HistoryTable.NAME,
                        TranslatorDbSchema.HistoryTable.Cols._ID + " = ?",
                        new String[]{String.valueOf(historyEntryId)}
                );
                // Удаление соответствующего перевода, если он не помечен, как избранный.
                if (deleteTranslation) {
                    mDatabase.delete(
                            TranslatorDbSchema.TranslationsTable.NAME,
                            TranslatorDbSchema.TranslationsTable.Cols._ID + " = ? " +
                                    " AND " + TranslatorDbSchema.TranslationsTable.Cols.FAVORITE + " = ? ",
                            new String[]{String.valueOf(translationId), "0"}
                    );
                }
                // Уменьшение количества записей на удаление.
                deleteCount--;
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
    }

    private TranslationCursorWrapper getHistoryCursor(String selection, String[] selectionArgs) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TranslatorDbSchema.HistoryTable.NAME +
                " INNER JOIN " + TranslatorDbSchema.TranslationsTable.NAME + " ON " +
                TranslatorDbSchema.HistoryTable.Cols.TRANSLATION + " = " +
                TranslatorDbSchema.TranslationsTable.NAME + "." +
                TranslatorDbSchema.TranslationsTable.Cols._ID
        );

        String[] projectionIn = new String[]{
                TranslatorDbSchema.TranslationsTable.NAME + "."
                        + TranslatorDbSchema.TranslationsTable.Cols._ID,
                TranslatorDbSchema.TranslationsTable.Cols.ORIGINAL_TEXT,
                TranslatorDbSchema.TranslationsTable.Cols.TRANSLATION_TEXT,
                TranslatorDbSchema.TranslationsTable.Cols.PRIMARY_LANGUAGE,
                TranslatorDbSchema.TranslationsTable.Cols.TARGET_LANGUAGE,
                TranslatorDbSchema.TranslationsTable.Cols.FAVORITE,
                TranslatorDbSchema.HistoryTable.Cols.DATE
        };

        return new TranslationCursorWrapper(
                mDatabase.rawQuery(queryBuilder.buildQuery(
                        projectionIn,
                        selection,
                        null,
                        null,
//                        TranslatorDbSchema.HistoryTable.Cols.DATE + " DESK",
                        TranslatorDbSchema.HistoryTable.NAME + "." + TranslatorDbSchema.HistoryTable.Cols.DATE + " DESC",
                        null
                ), selectionArgs));
    }

    private ContentValues getTranslationContentValues(Translation translation) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.TranslationsTable.Cols.PRIMARY_LANGUAGE,
                translation.getPrimaryLanguage());
        values.put(TranslatorDbSchema.TranslationsTable.Cols.TARGET_LANGUAGE,
                translation.getTargetLanguage());
        values.put(TranslatorDbSchema.TranslationsTable.Cols.ORIGINAL_TEXT,
                translation.getOriginalText());
        values.put(TranslatorDbSchema.TranslationsTable.Cols.TRANSLATION_TEXT,
                translation.getTranslationText());
        values.put(TranslatorDbSchema.TranslationsTable.Cols.FAVORITE,
                translation.isFavorite());
        return values;
    }

    private ContentValues getDictionaryContentValues(Dictionary dictionary) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.DictionaryTable.Cols.TEXT,
                dictionary.getText());
        values.put(TranslatorDbSchema.DictionaryTable.Cols.TRANSLATION_TEXT,
                dictionary.getTranslationText());
        values.put(TranslatorDbSchema.DictionaryTable.Cols.TRANSLATION,
                dictionary.getTranslationId());
        values.put(TranslatorDbSchema.DictionaryTable.Cols.PART_OF_SPEECH,
                dictionary.getPartOfSpeech());
        values.put(TranslatorDbSchema.DictionaryTable.Cols.TRANSCRIPTION,
                dictionary.getTranscription());
        return values;
    }

    private ContentValues getSynonymContentValues(Synonym synonym) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.SynonymsTable.Cols.TEXT,
                synonym.getText());
        values.put(TranslatorDbSchema.SynonymsTable.Cols.DICTIONARY,
                synonym.getDictionaryId());
        return values;
    }

    private ContentValues getMeanContentValues(Mean mean) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.MeansTable.Cols.TEXT,
                mean.getText());
        values.put(TranslatorDbSchema.MeansTable.Cols.DICTIONARY,
                mean.getDictionaryId());
        return values;
    }

    private ContentValues getHistoryEntryContentValues(long translationId, Date date) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.HistoryTable.Cols.DATE, date.getTime());
        values.put(TranslatorDbSchema.HistoryTable.Cols.TRANSLATION, translationId);
        return values;
    }

    private ContentValues getFavoriteContentValues(boolean favorite) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.TranslationsTable.Cols.FAVORITE, favorite);
        return values;
    }

    private ContentValues getLanguageContentValues(Language language) {
        ContentValues values = new ContentValues();
        values.put(TranslatorDbSchema.LanguagesTable.Cols.SIGN, language.getSign());
        values.put(TranslatorDbSchema.LanguagesTable.Cols.TEXT, language.getText());
        return values;
    }
}
