package aleksandersh.android.yandextranslate.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import aleksandersh.android.yandextranslate.model.Language;

/**
 * Created by Alexander on 17.04.2017.
 */

public class TranslatorDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "TranslatorDbHelper";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "translationDatabase.db";
    private static final String LANGUAGES_FILE_PATH = "languages.txt";

    // Экземпляр контекста сохраняется в родительском объекте, однако он им не делится и
    // я не нашел способов его получить, поэтому дублирую переменную здесь. Контекст необходим для
    // получения Assets, где хранится список языков по-умолчанию. При создании базы данных их
    // необходимо загрузить в нее.
    protected Context mContext;

    public TranslatorDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TranslatorDbSchema.TranslationsTable.NAME + "(" +
                TranslatorDbSchema.TranslationsTable.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TranslatorDbSchema.TranslationsTable.Cols.PRIMARY_LANGUAGE + " TEXT, " +
                TranslatorDbSchema.TranslationsTable.Cols.TARGET_LANGUAGE + " TEXT, " +
                TranslatorDbSchema.TranslationsTable.Cols.ORIGINAL_TEXT + " TEXT, " +
                TranslatorDbSchema.TranslationsTable.Cols.TRANSLATION_TEXT + " TEXT, " +
                TranslatorDbSchema.TranslationsTable.Cols.FAVORITE + " INTEGER" +
                ")"
        );
        db.execSQL("CREATE TABLE " + TranslatorDbSchema.DictionaryTable.NAME + "(" +
                TranslatorDbSchema.DictionaryTable.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TranslatorDbSchema.DictionaryTable.Cols.TEXT + " TEXT, " +
                TranslatorDbSchema.DictionaryTable.Cols.TRANSLATION_TEXT + " TEXT, " +
                TranslatorDbSchema.DictionaryTable.Cols.TRANSCRIPTION + " TEXT, " +
                TranslatorDbSchema.DictionaryTable.Cols.PART_OF_SPEECH + " TEXT, " +
                TranslatorDbSchema.DictionaryTable.Cols.UI_LANG + " TEXT, " +
                TranslatorDbSchema.DictionaryTable.Cols.TRANSLATION +
                " INTEGER REFERENCES " + TranslatorDbSchema.TranslationsTable.NAME +
                "(" + TranslatorDbSchema.TranslationsTable.Cols._ID + ") ON DELETE CASCADE" +
                ")"
        );
        db.execSQL("CREATE TABLE " + TranslatorDbSchema.HistoryTable.NAME + "(" +
                TranslatorDbSchema.HistoryTable.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TranslatorDbSchema.HistoryTable.Cols.DATE + " INTEGER, " +
                TranslatorDbSchema.HistoryTable.Cols.TRANSLATION +
                " INTEGER REFERENCES " + TranslatorDbSchema.TranslationsTable.NAME +
                "(" + TranslatorDbSchema.TranslationsTable.Cols._ID + ")" +
                ")"
        );
        db.execSQL("CREATE TABLE " + TranslatorDbSchema.MeansTable.NAME + "(" +
                TranslatorDbSchema.MeansTable.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TranslatorDbSchema.MeansTable.Cols.TEXT + " TEXT, " +
                TranslatorDbSchema.MeansTable.Cols.DICTIONARY +
                " INTEGER REFERENCES " + TranslatorDbSchema.DictionaryTable.NAME +
                "(" + TranslatorDbSchema.DictionaryTable.Cols._ID + ") ON DELETE CASCADE" +
                ")"
        );
        db.execSQL("CREATE TABLE " + TranslatorDbSchema.SynonymsTable.NAME + "(" +
                TranslatorDbSchema.SynonymsTable.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TranslatorDbSchema.SynonymsTable.Cols.TEXT + " TEXT, " +
                TranslatorDbSchema.SynonymsTable.Cols.DICTIONARY +
                " INTEGER REFERENCES " + TranslatorDbSchema.DictionaryTable.NAME +
                "(" + TranslatorDbSchema.DictionaryTable.Cols._ID + ") ON DELETE CASCADE" +
                ")"
        );
        db.execSQL("CREATE TABLE " + TranslatorDbSchema.LanguagesTable.NAME + "(" +
                TranslatorDbSchema.LanguagesTable.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TranslatorDbSchema.LanguagesTable.Cols.TEXT + " TEXT, " +
                TranslatorDbSchema.LanguagesTable.Cols.SIGN + " TEXT" +
                ")"
        );
        initializeLanguages(db);
        Log.d(TAG, "Database created.");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // По-умолчанию использование вешних ключей отключено, поэтому для каждой новой сессии
        // необходимо выставлять флаг true. Без этого флага не будут выполнять каскадные операции.
        db.setForeignKeyConstraintsEnabled(true);

        Log.d(TAG, "Foreign key constraints enabled.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // При обновлении базы данных никакие действия не заданы.
        Log.d(TAG, "Database on upgrade.");
    }

    /**
     * Загрузка языков из файла Asset.
     *
     * @param db Экземпляр базы данных, в которую выполняется загрузка.
     */
    private void initializeLanguages(SQLiteDatabase db) {
        ArrayList<ContentValues> contentValuesList = new ArrayList<>();
        AssetManager assetManager = mContext.getAssets();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(assetManager.open(LANGUAGES_FILE_PATH)));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    int signStart = line.indexOf('"');
                    if (signStart == -1)
                        continue;
                    int signEnd = line.indexOf('"', signStart + 1);
                    if (signEnd == -1)
                        continue;
                    int textStart = line.indexOf('"', signEnd + 1);
                    if (textStart == -1)
                        continue;
                    int textEnd = line.indexOf('"', textStart + 1);
                    if (textEnd == -1)
                        continue;

                    ContentValues values = new ContentValues();
                    values.put(TranslatorDbSchema.LanguagesTable.Cols.SIGN,
                            line.substring(signStart + 1, signEnd));
                    values.put(TranslatorDbSchema.LanguagesTable.Cols.TEXT,
                            line.substring(textStart + 1, textEnd));
                    contentValuesList.add(values);
                }
            } finally {
                reader.close();
            }

            Collections.sort(contentValuesList, new Comparator<ContentValues>() {
                @Override
                public int compare(ContentValues o1, ContentValues o2) {
                    return o1.getAsString(TranslatorDbSchema.LanguagesTable.Cols.TEXT)
                            .compareTo(o2.getAsString(TranslatorDbSchema.LanguagesTable.Cols.TEXT));
                }
            });

            // Добавление всех языков из списка.
            for (ContentValues values : contentValuesList) {
                db.insert(
                        TranslatorDbSchema.LanguagesTable.NAME,
                        null,
                        values
                );
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: Languages are not loaded.");
        }
        Log.d(TAG, "Languages loaded from assets: " + contentValuesList.size());
    }
}
