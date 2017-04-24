package aleksandersh.android.yandextranslate.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import aleksandersh.android.yandextranslate.R;
import aleksandersh.android.yandextranslate.model.TranslationState;
import aleksandersh.android.yandextranslate.YandexTranslateActivity;
import aleksandersh.android.yandextranslate.dao.TranslationDao;
import aleksandersh.android.yandextranslate.model.Dictionary;
import aleksandersh.android.yandextranslate.model.Language;
import aleksandersh.android.yandextranslate.model.Mean;
import aleksandersh.android.yandextranslate.model.Synonym;
import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 12.04.2017.
 */

public class TranslateFragment extends Fragment {
    private static final String TAG = "TranslateFragment";
    // Время задержки начала перевода текста после окончания ввода текста.
    public static final long TRANSLATION_TIME_DELAY = 1500;
    // Константы, используемые в RecyclerView, для определения типа элемента.
    public static final int ITEM_VIEW_TYPE_TRANSLATION = 0;
    public static final int ITEM_VIEW_TYPE_PART_OF_SPEECH = 1;
    public static final int ITEM_VIEW_TYPE_DEFINITION = 2;

    // Сохранение ссылок на используемые представления.
    private TextView mTranslationTextView;
    private EditText mInputEditText;
    private Button mPrimaryLanguageButton;
    private Button mTargetLanguageButton;
    private RecyclerView mDictionaryRecyclerView;
    private ImageButton mSetFavoriteImageButton;

    // Обозначение исходного языка.
    private String mPrimaryLanguageId;
    // Обозначение целевого языка.
    private String mTargetLanguageId;
    // Флаг, обозначающий, что перевод добавлен в избранное.
    private boolean mFavorite;

    // Обработчик сообщений UI-потока. Используется для планирования перевода текста с задержкой.
    private Handler mHandler;
    // Экземпляр содержит код, который будет выполнен mHandler после задержки.
    private Runnable mTranslationDelayer;
    // Содержит объект, которому будет передан запрос на перевод.
    private TranslationRequestHandler mTranslationRequestHandler;
    // Адаптер RecyclerView для вывода данных словаря.
    private DictionaryAdapter mDictionaryAdapter;

    public TranslateFragment() {
    }

    public static TranslateFragment newInstance() {
        return new TranslateFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создание обработчика сообщений.
        mHandler = new Handler();
        // Создание объекта с кодом, выполняемым Handler.
        mTranslationDelayer = new Runnable() {
            @Override
            public void run() {
                requestTranslation(getCurrentState());
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_translate, container, false);

        mTranslationTextView = (TextView) view.findViewById(R.id.translation_text_view);
        mTranslationTextView.setMovementMethod(new ScrollingMovementMethod());

        // Ссылки становятся рабочими.
        TextView yandexTranslateRequirementTextView = (TextView)
                view.findViewById(R.id.yandex_translate_requirement_text_view);
        yandexTranslateRequirementTextView.setMovementMethod(new LinkMovementMethod());

        TextView yandexDictionaryRequirementTextView = (TextView)
                view.findViewById(R.id.yandex_dictionary_requirement_text_view);
        yandexDictionaryRequirementTextView.setMovementMethod(new LinkMovementMethod());

        mInputEditText = (EditText) view.findViewById(R.id.input_edit_text);
        mInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // Пользователь изменил текст и старые задания на перевод необходимо отменить.
                mHandler.removeCallbacks(mTranslationDelayer);
                // Планирование нового задания на перевод с определенной задержкой.
                mHandler.postDelayed(mTranslationDelayer, TRANSLATION_TIME_DELAY);
            }
        });

        mPrimaryLanguageButton = (Button) view.findViewById(R.id.primary_language_button);
        mPrimaryLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTranslationRequestHandler.selectPrimaryLanguage(mPrimaryLanguageId);
            }
        });

        mTargetLanguageButton = (Button) view.findViewById(R.id.target_language_button);
        mTargetLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTranslationRequestHandler.selectTargetLanguage(mTargetLanguageId);
            }
        });

        ImageButton swapLanguagesButton = (ImageButton) view.findViewById(R.id.swap_languages_button);
        swapLanguagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapLanguages();
            }
        });

        ImageButton clearImageButton = (ImageButton) view.findViewById(R.id.clear_image_button);
        clearImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInputEditText.setText("");
                clearTranslation();
            }
        });

        mSetFavoriteImageButton = (ImageButton) view.findViewById(R.id.set_favorite_image_button);
        mSetFavoriteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFavorite(!mFavorite);
                new SetFavoriteTask().execute(getCurrentState());
            }
        });
        // Кнопка должна появляться только тогда, когда есть что добавлять в избранное.
        mSetFavoriteImageButton.setVisibility(View.GONE);

        mDictionaryRecyclerView = (RecyclerView) view.findViewById(R.id.dictionary_recycler_view);
        mDictionaryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mDictionaryAdapter = new DictionaryAdapter(new ArrayList<Dictionary>(0));
        mDictionaryRecyclerView.setAdapter(mDictionaryAdapter);

        restoreState();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TranslationRequestHandler)
            mTranslationRequestHandler = (TranslationRequestHandler) context;
        else
            throw new ClassCastException("Context must implement TranslationRequestHandler.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTranslationRequestHandler = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
    }

    /**
     * Очистка полей переводчика и затем их восстановление.
     */
    public void updateUserInterface() {
        clearTranslation();
        restoreState();
    }

    /**
     * Устанавливает новый текст представление с переводом.
     *
     * @param translation Результат перевода.
     */
    public void setTranslation(Translation translation) {
        mTranslationTextView.setTextColor(
                ContextCompat.getColor(getActivity(), android.R.color.secondary_text_light));
        mTranslationTextView.setText(translation.getTranslationText());
        setFavorite(translation.isFavorite());

        List<Dictionary> dictionaryList = translation.getDictionaryDefinitions();
        Collections.sort(dictionaryList, new Comparator<Dictionary>() {
            @Override
            public int compare(Dictionary o1, Dictionary o2) {
                return o1.getPartOfSpeech().compareTo(o2.getPartOfSpeech());
            }
        });
        Collections.sort(dictionaryList, new Comparator<Dictionary>() {
            @Override
            public int compare(Dictionary o1, Dictionary o2) {
                return (o1.getText() + " " + o1.getTranscription())
                        .compareTo(o2.getText() + " " + o2.getTranscription());
            }
        });

        mDictionaryAdapter.setDictionaryList(translation.getDictionaryDefinitions());
        mDictionaryAdapter.notifyDataSetChanged();

        mSetFavoriteImageButton.setVisibility(View.VISIBLE);
    }

    /**
     * Установка полей исходного языка.
     *
     * @param language Язык, который нужно установить.
     */
    public void setPrimaryLanguage(Language language) {
        mPrimaryLanguageId = language.getSign();
        mPrimaryLanguageButton.setText(language.getText());
        requestTranslation(getCurrentState());
    }

    /**
     * Установка полей конечного языка.
     *
     * @param language Конечный язык.
     */
    public void setTargetLanguage(Language language) {
        mTargetLanguageId = language.getSign();
        mTargetLanguageButton.setText(language.getText());
        requestTranslation(getCurrentState());
    }

    /**
     * Метод для обработки ошибок во время перевода. Очищает поля перевода и выводит текст ошибки.
     *
     * @param error Текст ошибки.
     */
    public void setError(String error) {
        clearTranslation();
        mTranslationTextView.setText(error);
        mTranslationTextView.setTextColor(
                ContextCompat.getColor(getActivity(), R.color.colorError));

        mSetFavoriteImageButton.setVisibility(View.GONE);
    }

    /**
     * Формирует объект класса {@link TranslationState} на основе текущего состояния фрагмента.
     *
     * @return Полученный объект состояния.
     */
    private TranslationState getCurrentState() {
        TranslationState state = new TranslationState(
                mInputEditText.getText().toString(),
                mPrimaryLanguageId,
                mTargetLanguageId);
        state.setFavorite(mFavorite);
        state.setLanguageText(mPrimaryLanguageButton.getText().toString());
        state.setTranslationLanguageText(mTargetLanguageButton.getText().toString());
        return state;
    }

    /**
     * Устанавливает новое состояние флага избранного.
     *
     * @param favorite Новое значение флага.
     */
    private void setFavorite(boolean favorite) {
        mFavorite = favorite;
        int resourceId;
        if (favorite) {
            resourceId = android.R.drawable.btn_star_big_on;
        } else {
            resourceId = android.R.drawable.btn_star_big_off;
        }
        mSetFavoriteImageButton.setImageResource(resourceId);
    }

    /**
     * Очищает поля, связанные с переводом.
     */
    private void clearTranslation() {
        mTranslationTextView.setText("");
        mDictionaryAdapter.setDictionaryList(new ArrayList<Dictionary>());
        mDictionaryAdapter.notifyDataSetChanged();
        setFavorite(false);
    }

    /**
     * Поменять местами исходный и конечный языки.
     */
    private void swapLanguages() {
        String primaryLangSign = mPrimaryLanguageId;
        mPrimaryLanguageId = mTargetLanguageId;
        mTargetLanguageId = primaryLangSign;

        String primaryLangButtonText = mPrimaryLanguageButton.getText().toString();
        mPrimaryLanguageButton.setText(mTargetLanguageButton.getText().toString());
        mTargetLanguageButton.setText(primaryLangButtonText);

        requestTranslation(getCurrentState());
    }

    /**
     * Передать текущее состояние фрагмента в обработчик.
     */
    private void saveState() {
        mTranslationRequestHandler.setState(getCurrentState());
    }

    /**
     * Получить текущее состояние из обработчика.
     */
    private void restoreState() {
        TranslationState translationState = mTranslationRequestHandler.getState();

        mInputEditText.setText(translationState.getText());
        mPrimaryLanguageId = translationState.getLanguage();
        mPrimaryLanguageButton.setText(translationState.getLanguageText());
        mTargetLanguageId = translationState.getTranslationLanguage();
        mTargetLanguageButton.setText(translationState.getTranslationLanguageText());

        requestTranslation(translationState);
    }

    /**
     * Метод делает запрос к привязанному объекту-перехватчику запросов на перевод. В данном случае
     * им является {@link YandexTranslateActivity}.
     */
    private void requestTranslation(TranslationState translationState) {
        // Условие сработает, если во время ожидания фрагмент был пересоздан, в таком случае
        // перевод выполнять не следует. При создании нового фрагмента, сработает
        // обработчик изменения текста и запустится новое задание на перевод.
        if (!isAdded())
            return;
        // Если текст не задан, перевод выполнять не следует.
        if (translationState.getText().isEmpty())
            return;
        // Полученная модель перевода передается перехватчику.
        mTranslationRequestHandler.handleTranslationRequest(translationState);
    }

    /**
     * Интерфейс, необходимый объекту-обработчику запросов на перевод и выбор языков. В данном
     * случае обработчиком является {@link YandexTranslateActivity}.
     */
    public interface TranslationRequestHandler {
        void handleTranslationRequest(TranslationState request);

        void selectPrimaryLanguage(String currentLangSign);

        void selectTargetLanguage(String currentLangSign);

        void setState(TranslationState translationState);

        TranslationState getState();
    }

    /**
     * Этот интерфейс реализуют все наследники {@link RecyclerView.ViewHolder}, используемые
     * в списке словаря, так как в нем может быть несколько разных представлений.
     * Наследуется от {@link RecyclerView.ViewHolder} с добавлением метода, заполняющего
     * разметку.
     */
    private abstract class DictionaryHolder extends RecyclerView.ViewHolder {
        public DictionaryHolder(View itemView) {
            super(itemView);
        }

        /**
         * С помощью этого метода заполняется разметка представления.
         *
         * @param dictionary Словарная статья, на основе которой заполняется представление.
         */
        abstract void bindDictionary(Dictionary dictionary);
    }

    private class TranslationDictionaryHolder extends DictionaryHolder {
        private Dictionary mDictionary;
        private TextView mSynonymsTextView;
        private TextView mMeansTextView;

        public TranslationDictionaryHolder(View itemView) {
            super(itemView);
            mSynonymsTextView = (TextView)
                    itemView.findViewById(R.id.dictionary_translation_list_item_synonyms);
            mMeansTextView = (TextView)
                    itemView.findViewById(R.id.dictionary_translation_list_item_means);
        }

        @Override
        public void bindDictionary(Dictionary dictionary) {
            mDictionary = dictionary;

            // Заполнение синонимов слова.
            mSynonymsTextView.setText(getSynonymsText(dictionary));
            // Заполнение значений.
            String means = getMeansText(dictionary);
            if (means.isEmpty())
                mMeansTextView.setVisibility(View.GONE);
            else
                mMeansTextView.setText(means);
        }

        /**
         * Метод для формирования текста для представления синонимов из словаря.
         *
         * @param dictionary Словарь из которого будет формироваться список синонимов.
         * @return Строковое представление списка.
         */
        protected String getSynonymsText(Dictionary dictionary) {
            String synonymsText = dictionary.getTranslationText();
            if (synonymsText == null)
                synonymsText = "";
            List<Synonym> synonymList = dictionary.getSynonyms();
            if (!synonymList.isEmpty()) {
                StringBuilder synonymsStringBuilder = new StringBuilder(dictionary.getTranslationText());
                for (Synonym synonym : synonymList)
                    synonymsStringBuilder.append(", ").append(synonym.getText());
                synonymsText = synonymsStringBuilder.toString();
            }
            return synonymsText;
        }

        /**
         * Метод для формирования текста для представления значений из словаря.
         *
         * @param dictionary Словарь из которого будет формироваться список значений.
         * @return Строковое представление списка.
         */
        protected String getMeansText(Dictionary dictionary) {
            String meansText = "";
            List<Mean> meanList = dictionary.getMeans();
            if (!meanList.isEmpty()) {
                StringBuilder meansStringBuilder = new StringBuilder("(");
                for (int i = 0; i < meanList.size(); i++) {
                    if (i != 0)
                        meansStringBuilder.append(", ");
                    meansStringBuilder.append(meanList.get(i).getText());
                }
                meansStringBuilder.append(")");
                meansText = meansStringBuilder.toString();
            }
            return meansText;
        }
    }

    private class PartOfSpeechDictionaryHolder extends TranslationDictionaryHolder {
        private TextView mPartOfSpeechTextView;

        public PartOfSpeechDictionaryHolder(View itemView) {
            super(itemView);
            mPartOfSpeechTextView = (TextView)
                    itemView.findViewById(R.id.dictionary_part_of_speech_list_item_text);
        }

        @Override
        public void bindDictionary(Dictionary dictionary) {
            super.bindDictionary(dictionary);
            mPartOfSpeechTextView.setText(dictionary.getPartOfSpeech());
        }
    }

    private class DefinitionDictionaryHolder extends PartOfSpeechDictionaryHolder {
        private TextView mDefinitionTextView;
        private TextView mTranscriptionTextView;

        public DefinitionDictionaryHolder(View itemView) {
            super(itemView);
            mDefinitionTextView = (TextView)
                    itemView.findViewById(R.id.dictionary_definition_list_item_text);
            mTranscriptionTextView = (TextView)
                    itemView.findViewById(R.id.dictionary_definition_list_item_transcription);
        }

        @Override
        public void bindDictionary(Dictionary dictionary) {
            super.bindDictionary(dictionary);
            mDefinitionTextView.setText(dictionary.getText());
            mTranscriptionTextView.setText("[" + dictionary.getTranscription() + "]");
        }
    }

    private class DictionaryAdapter extends RecyclerView.Adapter<DictionaryHolder> {
        private List<Dictionary> mDictionaryList;

        public DictionaryAdapter(List<Dictionary> dictionaryList) {
            mDictionaryList = dictionaryList;
        }

        @Override
        public DictionaryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            DictionaryHolder holder = null;
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case ITEM_VIEW_TYPE_TRANSLATION:
                    holder = new TranslationDictionaryHolder(inflater.inflate(
                            R.layout.dictionary_translation_list_item, parent, false));
                    break;
                case ITEM_VIEW_TYPE_PART_OF_SPEECH:
                    holder = new PartOfSpeechDictionaryHolder(inflater.inflate(
                            R.layout.dictionary_part_of_speech_list_item, parent, false));
                    break;
                case ITEM_VIEW_TYPE_DEFINITION:
                    holder = new DefinitionDictionaryHolder(inflater.inflate(
                            R.layout.dictionary_definition_list_item, parent, false));
                    break;
                default:
                    holder = new TranslationDictionaryHolder(inflater.inflate(
                            R.layout.dictionary_translation_list_item, parent, false));
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(DictionaryHolder holder, int position) {
            holder.bindDictionary(mDictionaryList.get(position));
        }

        @Override
        public int getItemCount() {
            return mDictionaryList.size();
        }

        @Override
        public int getItemViewType(int position) {
            // Если это первый элемент, то выводится всегда главный заголовок.
            if (position == 0)
                return ITEM_VIEW_TYPE_DEFINITION;
            Dictionary previousItem = mDictionaryList.get(position - 1);
            Dictionary currentItem = mDictionaryList.get(position);
            // Если не совпадает текст и транскрипция.
            if (!(previousItem.getText() + previousItem.getTranscription())
                    .equals(currentItem.getText() + currentItem.getTranscription()))
                return ITEM_VIEW_TYPE_DEFINITION;
            // Если не совпадает часть речи.
            if (!(previousItem.getPartOfSpeech().equals(currentItem.getPartOfSpeech())))
                return ITEM_VIEW_TYPE_PART_OF_SPEECH;
            // Иначе это обыкновенный элемент.
            return ITEM_VIEW_TYPE_TRANSLATION;
        }

        public void setDictionaryList(List<Dictionary> dictionaryList) {
            mDictionaryList = dictionaryList;
        }
    }

    private class SetFavoriteTask extends AsyncTask<TranslationState, Void, Boolean> {
        @Override
        protected Boolean doInBackground(TranslationState... params) {
            TranslationDao translationDao = TranslationDao.get(getActivity());
            if (params.length < 1) {
                Log.e(TAG, "SetFavoriteTask: Settings not transmitted.");
                cancel(false);
                return false;
            }
            translationDao.setFavorite(params[0]);
            return true;
        }
    }
}
