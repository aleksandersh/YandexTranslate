package aleksandersh.android.yandextranslate;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import aleksandersh.android.yandextranslate.fragment.FavoritesFragment;
import aleksandersh.android.yandextranslate.fragment.HistoryFragment;
import aleksandersh.android.yandextranslate.fragment.TranslateFragment;
import aleksandersh.android.yandextranslate.fragment.TranslationManagerFragment;
import aleksandersh.android.yandextranslate.model.Language;
import aleksandersh.android.yandextranslate.model.Translation;
import aleksandersh.android.yandextranslate.model.TranslationState;

public class YandexTranslateActivity extends AppCompatActivity
        implements TranslationManagerFragment.TranslationResponseHandler,
        TranslationActivity,
        TranslateFragment.TranslationRequestHandler {
    private static final String TRANSLATION_MANAGER_TAG = "TranslationManager";
    private static final String TRANSLATE_FRAGMENT_TAG = "TranslateFragment";
    // Коды запросов для Intent выбора языков.
    private static final int REQUEST_PRIMARY_LANGUAGE = 1;
    private static final int REQUEST_TARGET_LANGUAGE = 2;
    // Данные shared preferences.
    private static final String PREFERENCES_TEXT = "text";
    private static final String PREFERENCES_PRIMARY_LANG = "primary_lang";
    private static final String PREFERENCES_PRIMARY_LANG_TEXT = "primary_lang_text";
    private static final String PREFERENCES_TARGET_LANG = "target_lang";
    private static final String PREFERENCES_TARGET_LANG_TEXT = "target_lang_text";
    private static final String CURRENT_POSITION_KEY = "CurrentPosition";

    // Фрагмент менеджера переводов.
    private TranslationManagerFragment mTranslationManagerFragment;
    private TabLayout mTabLayout;
    private SharedPreferences mPreferences;
    private int currentPosition = 0;
    private TranslationState mTranslationState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yandex_translate);

        // Получение текущей позиции в навигации.
        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(CURRENT_POSITION_KEY, 0);
        }

        mPreferences = getPreferences(MODE_PRIVATE);
        restoreState();

        // Получение менеджера фрагментов и библиотеки поддержкия.
        final FragmentManager fm = getSupportFragmentManager();

        // Поиск в менеджере уже созданного фрагмента менеджера переводов.
        mTranslationManagerFragment = (TranslationManagerFragment) fm
                .findFragmentByTag(TRANSLATION_MANAGER_TAG);
        // Если фрагмент не найден - создается новый.
        if (mTranslationManagerFragment == null) {
            mTranslationManagerFragment = new TranslationManagerFragment();
            fm.beginTransaction()
                    .add(mTranslationManagerFragment, TRANSLATION_MANAGER_TAG)
                    .commit();
        }

        // Панель навигации.
        mTabLayout = (TabLayout) findViewById(R.id.navigation_tab_layout);
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPosition = tab.getPosition();
                switch (currentPosition) {
                    case 0:
                        fm.beginTransaction()
                                .replace(R.id.fragment_container,
                                        TranslateFragment.newInstance(),
                                        TRANSLATE_FRAGMENT_TAG)
                                .commit();
                        break;
                    case 1:
                        fm.beginTransaction()
                                .replace(R.id.fragment_container,
                                        FavoritesFragment.newInstance())
                                .commit();
                        break;
                    case 2:
                        fm.beginTransaction()
                                .replace(R.id.fragment_container,
                                        HistoryFragment.newInstance())
                                .commit();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Если позиция нулевая, то при вызове метода select() у нулевой вкладки, перелистывание
        // не будет осуществлено и OnTabSelectedListener не будет отработан, в таком
        // случае необходимо загрузить фрагмент по-умолчанию.
        boolean loadDefaultFragment = true;
        if (currentPosition != 0) {
            // Загрузить используемый фрагмент.
            TabLayout.Tab currentTab = mTabLayout.getTabAt(currentPosition);
            if (currentTab != null) {
                currentTab.select();
                loadDefaultFragment = false;
            }
        }
        if (loadDefaultFragment) {
            fm.beginTransaction()
                    .replace(R.id.fragment_container,
                            TranslateFragment.newInstance(),
                            TRANSLATE_FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_POSITION_KEY, currentPosition);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == REQUEST_PRIMARY_LANGUAGE) {
            TranslateFragment fragment = (TranslateFragment) getSupportFragmentManager()
                    .findFragmentByTag(TRANSLATE_FRAGMENT_TAG);
            if (fragment != null)
                fragment.setPrimaryLanguage((Language)
                        data.getParcelableExtra(LanguageSelectionActivity.EXTRA_LANG));
        } else if (requestCode == REQUEST_TARGET_LANGUAGE) {
            TranslateFragment fragment = (TranslateFragment) getSupportFragmentManager()
                    .findFragmentByTag(TRANSLATE_FRAGMENT_TAG);
            if (fragment != null)
                fragment.setTargetLanguage((Language)
                        data.getParcelableExtra(LanguageSelectionActivity.EXTRA_LANG));
        }
    }

    @Override
    public void showTranslation(TranslationState translationState) {
        mTranslationState = translationState;
        FragmentManager fm = getSupportFragmentManager();
        TranslateFragment translateFragment = (TranslateFragment)
                fm.findFragmentByTag(TRANSLATE_FRAGMENT_TAG);
        if (translateFragment == null) {
            TabLayout.Tab currentTab = mTabLayout.getTabAt(0);
            if (currentTab != null) {
                currentTab.select();
            }
        } else {
            translateFragment.updateUserInterface();
        }
    }

    @Override
    public void handleTranslationRequest(TranslationState model) {
        mTranslationManagerFragment.translate(model);
    }

    @Override
    public void handleTranslationResponse(Translation result) {
        TranslateFragment translateFragment = (TranslateFragment) getSupportFragmentManager()
                .findFragmentByTag(TRANSLATE_FRAGMENT_TAG);
        if (translateFragment != null)
            translateFragment.setTranslation(result);
    }

    @Override
    public void handleTranslationError(String error) {
        TranslateFragment translateFragment = (TranslateFragment) getSupportFragmentManager()
                .findFragmentByTag(TRANSLATE_FRAGMENT_TAG);
        if (translateFragment != null)
            translateFragment.setError(error);
    }

    @Override
    public void selectPrimaryLanguage(String currentLangSign) {
        Intent intent = LanguageSelectionActivity.newIntent(this, currentLangSign);
        startActivityForResult(intent, REQUEST_PRIMARY_LANGUAGE);
    }

    @Override
    public void selectTargetLanguage(String currentLangSign) {
        Intent intent = LanguageSelectionActivity.newIntent(this, currentLangSign);
        startActivityForResult(intent, REQUEST_TARGET_LANGUAGE);
    }

    @Override
    public void setState(TranslationState translationState) {
        mTranslationState = translationState;
    }

    @Override
    public TranslationState getState() {
        return mTranslationState;
    }

    private void saveState() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREFERENCES_TEXT, mTranslationState.getText());
        editor.putString(PREFERENCES_PRIMARY_LANG, mTranslationState.getLanguage());
        editor.putString(PREFERENCES_PRIMARY_LANG_TEXT, mTranslationState.getLanguageText());
        editor.putString(PREFERENCES_TARGET_LANG, mTranslationState.getTranslationLanguage());
        editor.putString(PREFERENCES_TARGET_LANG_TEXT, mTranslationState.getTranslationLanguageText());
        editor.apply();
    }

    private void restoreState() {
        TranslationState translationState = new TranslationState(
                mPreferences.getString(PREFERENCES_TEXT, ""),
                mPreferences.getString(PREFERENCES_PRIMARY_LANG, "ru"),
                mPreferences.getString(PREFERENCES_TARGET_LANG, "en")
        );
        translationState.setLanguageText(
                mPreferences.getString(PREFERENCES_PRIMARY_LANG_TEXT, "Русский"));
        translationState.setTranslationLanguageText(
                mPreferences.getString(PREFERENCES_TARGET_LANG_TEXT, "Английский"));
        mTranslationState = translationState;
    }
}
