package aleksandersh.android.yandextranslate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import aleksandersh.android.yandextranslate.fragment.LanguageSelectionFragment;
import aleksandersh.android.yandextranslate.model.Language;

public class LanguageSelectionActivity extends AppCompatActivity
        implements LanguageSelectionFragment.LanguageHandler {
    // Идентификатор текущего выбранного языка в Intent.
    public static final String EXTRA_LANG_SIGN =
            "aleksandersh.android.yandextranslate.lang_sign";
    public static final String EXTRA_LANG =
            "aleksandersh.android.yandextranslate.lang";
    private static final String LANGUAGE_SELECTION_FRAGMENT_TAG = "LangSelectionFragment";

    public static Intent newIntent(Context packageContext, String langSign) {
        // Формируется новый Intent на запуск активности с передачей обозначения языка.
        Intent intent = new Intent(packageContext, LanguageSelectionActivity.class);
        intent.putExtra(EXTRA_LANG_SIGN, langSign);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        // Получение идентификатора текущего языка.
        String langSign = (String) getIntent().getSerializableExtra(EXTRA_LANG_SIGN);

        // Включение фрагмента с пользовательским интерфейсом.
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fm.beginTransaction()
                    .add(R.id.languages_fragment_container,
                            LanguageSelectionFragment.newInstance(langSign),
                            LANGUAGE_SELECTION_FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void handleLanguage(Language language) {
        Intent intent = new Intent();
        // Тот, кто запросил результат может ожидать его получение с помощью того же идентификатора.
        intent.putExtra(EXTRA_LANG_SIGN, language.getSign());
        // Однако результат будет более полным, если в дополнение вернуть объект Language, а не его
        // обозначение.
        intent.putExtra(EXTRA_LANG, language);
        setResult(RESULT_OK, intent);
        finish();
    }
}
