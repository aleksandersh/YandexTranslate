package aleksandersh.android.yandextranslate.database;

import android.provider.BaseColumns;

/**
 * Created by Alexander on 17.04.2017.
 */

public class TranslatorDbSchema {
    public static final class TranslationsTable {
        public static final String NAME = "translations";
        public static final class Cols implements BaseColumns {
            public static final String PRIMARY_LANGUAGE = "primary_language";
            public static final String TARGET_LANGUAGE = "target_language";
            public static final String ORIGINAL_TEXT = "original_text";
            public static final String TRANSLATION_TEXT = "translation_text";
            public static final String FAVORITE = "favorite";
        }
    }

    public static final class DictionaryTable {
        public static final String NAME = "dictionary";
        public static final class Cols implements BaseColumns {
            public static final String TRANSLATION = "translation";
            public static final String UI_LANG = "ui_lang";
            public static final String TEXT = "text";
            public static final String TRANSLATION_TEXT = "translation_text";
            public static final String TRANSCRIPTION = "transcription";
            public static final String PART_OF_SPEECH = "part_of_speech";
        }
    }

    public static final class HistoryTable {
        public static final String NAME = "history";
        public static final class Cols implements BaseColumns {
            public static final String TRANSLATION = "translation";
            public static final String DATE = "date";
        }
    }

    public static final class SynonymsTable {
        public static final String NAME = "synonyms";
        public static final class Cols implements BaseColumns {
            public static final String DICTIONARY = "dictionary";
            public static final String TEXT = "text";
        }
    }

    public static final class MeansTable {
        public static final String NAME = "means";
        public static final class Cols implements BaseColumns {
            public static final String DICTIONARY = "dictionary";
            public static final String TEXT = "text";
        }
    }

    public static final class LanguagesTable {
        public static final String NAME = "languages";
        public static final class Cols implements BaseColumns {
            public static final String SIGN = "sign";
            public static final String TEXT = "text";
        }
    }
}
