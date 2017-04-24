package aleksandersh.android.yandextranslate.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.MalformedJsonException;

import java.io.IOException;
import java.util.List;

import aleksandersh.android.yandextranslate.model.TranslationResponse;
import aleksandersh.android.yandextranslate.model.TranslationState;
import aleksandersh.android.yandextranslate.YandexTranslateActivity;
import aleksandersh.android.yandextranslate.dao.TranslationDao;
import aleksandersh.android.yandextranslate.dto.ErrorDto;
import aleksandersh.android.yandextranslate.dto.dictionary.DictionaryDto;
import aleksandersh.android.yandextranslate.dto.translation.TranslationDto;
import aleksandersh.android.yandextranslate.httpService.DictionaryHttpService;
import aleksandersh.android.yandextranslate.httpService.TranslationHttpService;
import aleksandersh.android.yandextranslate.model.Translation;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alexander on 13.04.2017.
 * <p>
 * Объект данного класса содержит текущий контекст переводчика.
 * Наследуется от {@link android.support.v4.app.Fragment} с включением удержания. Принимает
 * запросы на перевод текста, сохраняя состояние перевода, и передает их соответсвующим
 * обработчикам. Управляет потоками для перевода.
 */

public class TranslationManagerFragment extends Fragment {
    private static final String TAG = "TranslationManager";
    private static final String TRANSL_API_KEY =
            "trnsl.1.1.20170414T010508Z.1c59e6c06af66f8e.fedbff1fb81686cbaa667dd5ae57a86d64238eb7";
    private static final String DICT_API_KEY =
            "dict.1.1.20170417T123635Z.460526203e93fc85.bc0821f159f38564a8aad3dfc746bff4b60d6cc2";
    // Язык пользовательского интерфейса.
    private static final String UI_LANG = "ru";

    // Http сервис, с помощью которого происходит взаимодействие с Яндекс переводчиком.
    private TranslationHttpService mTranslationHttpService;
    // Http сервис, с помощью которого происходит взаимодействие с Яндекс словарем.
    private DictionaryHttpService mDictionaryHttpService;
    // Конвертирует ответ от сервиса с ошибкой в прикладной объект.
    private Converter<ResponseBody, ErrorDto> mErrorConverter;
    // Объект для работы с базой данных.
    private TranslationDao mTranslationDao;
    // Модель текущего перевода.
    private TranslationState mTranslationState;
    // Готовый перевод.
    private Translation mTranslation;
    // Текущее состояние менеджера.
    private Status mStatus = Status.WAITING;
    // Текущий экземпляр задачи.
    private TranslationTask mTranslationTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Удержание фрагмента.
        setRetainInstance(true);

        // Создание Http клиента для работы с Яндекс переводчиком с помощью Retrofit 2.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://translate.yandex.net/api/v1.5/tr.json/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mTranslationHttpService = retrofit.create(TranslationHttpService.class);

        mErrorConverter =
                retrofit.responseBodyConverter(ErrorDto.class, ErrorDto.class.getAnnotations());

        // Создание Http клиента для работы с Яндекс словарем с помощью Retrofit 2.
        mDictionaryHttpService = new Retrofit.Builder()
                .baseUrl("https://dictionary.yandex.net/api/v1/dicservice.json/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DictionaryHttpService.class);

        mTranslationDao = TranslationDao.get(getActivity());
    }

    /**
     * Выполняет перевод текста.
     * Заменяет текущую модель перевода на новую и создает новое задание на перевод.
     *
     * @param translationState Новая модель перевода.
     */
    public void translate(TranslationState translationState) {
        // Проверка на то, что задание на подобный перевод уже есть.
        if (translationState.equals(mTranslationState)) {
            // Если задание завершено, то публикуем результат.
            if (mStatus == Status.FINISHED) {
                onTranslationFinished(mTranslation);
                return;
            }
            // Если задание запущено, создавать новое нет необходимости.
            else if (mStatus == Status.RUNNING) {
                return;
            }
        }

        // Если предыдущее задание еще выполняется и оно не соответствует новому запросу, его
        // необходимо отменить.
        if (mStatus == Status.RUNNING)
            mTranslationTask.cancel(true);

        mStatus = TranslationManagerFragment.Status.RUNNING;
        mTranslationState = translationState;
        mTranslationTask = new TranslationTask();
        mTranslationTask.execute(translationState);
    }

    /**
     * Вызывается из задания перевода при успеном завершении перевода.
     *
     * @param translation Полученный в задании перевод.
     */
    private void onTranslationFinished(Translation translation) {
        mTranslation = translation;
        mStatus = TranslationManagerFragment.Status.FINISHED;
        publishResult(translation);

        // Сохранение результатов в базу данных.
        PreservingResultTask preservingResultTask = new PreservingResultTask();
        preservingResultTask.execute(translation);
    }

    /**
     * Публикация успешного результата перевода.
     *
     * @param result Результат, который следует распространить.
     */
    private void publishResult(Translation result) {
        FragmentActivity activity = getActivity();
        if (activity != null && (activity instanceof TranslationResponseHandler))
            ((TranslationResponseHandler) activity).handleTranslationResponse(result);
    }

    /**
     * Публикация ошибки перевода.
     *
     * @param error Текст ошибки.
     */
    private void publishError(String error) {
        FragmentActivity activity = getActivity();
        if (activity != null && (activity instanceof TranslationResponseHandler))
            ((TranslationResponseHandler) activity).handleTranslationError(error);
    }

    /**
     * Интерфейс, необходимый объекту-обработчику перевода. В данном случае обработчиком является
     * {@link YandexTranslateActivity}, перенаправляющая его в пользовательский фрагмент.
     */
    public interface TranslationResponseHandler {
        void handleTranslationResponse(Translation result);

        void handleTranslationError(String error);
    }

    /**
     * Статусы, отражающие состояние менеджера.
     */
    public enum Status {
        // Перевод не выполнялся, изначальное состояние менеджера.
        WAITING,
        // В данное время осуществляется перевод.
        RUNNING,
        // Перевод выполнен, результат может быть получен.
        FINISHED,
        // Перевод был отменен.
        CANCELLED
    }

    /**
     * Задание на перевод текста.
     */
    private class TranslationTask extends AsyncTask<TranslationState, Void, TranslationResponse> {
        @Override
        protected TranslationResponse doInBackground(TranslationState... params) {
            // Возвращаемый объект.
            TranslationResponse result = new TranslationResponse();
            // Переведенный текст.
            Translation translation;

            // Перевод осуществляется только для одной модели.
            if (params.length == 0) {
                result.setError("Некорректно получена модель для перевода.");
                cancel(false);
                return null;
            }

            // Получение модели перевода.
            TranslationState translationState = params[0];

            // 1 этап: Проверка наличия перевода в базе данных.
            translation = mTranslationDao.getTranslationByRequest(translationState);
            if (translation != null) {
                result.setTranslation(translation);
                return result;
            }

            // 2 этап: Выполнение запроса к сервису Яндекс переводчика.
            translation = new Translation();
            translation.setOriginalText(translationState.getText());
            translation.setPrimaryLanguage(translationState.getLanguage());
            translation.setTargetLanguage(translationState.getTranslationLanguage());
            result.setTranslation(translation);

            try {
                // Выполнение запроса к Яндекс переводчику.
                Response<TranslationDto> response = mTranslationHttpService.getTranslation(
                        TRANSL_API_KEY,
                        translationState.getLanguage() + "-" +
                                translationState.getTranslationLanguage(),
                        translationState.getText()
                ).execute();
                if (response.isSuccessful()) {
                    // Случай успешного выполнения запроса (коды 200-300).
                    List<String> textList = response.body().getText();
                    if (textList.size() != 0)
                        // Запрос делается только на один текст.
                        translation.setTranslationText(textList.get(0));
                    else {
                        // Что-то пошло не так, сервис гарантирует хотя бы один элемент.
                        Log.w(TAG, "Service respond an empty translation");
                        result.setError("Яндекс переводчик: Ошибка получения перевода");
                        cancel(false);
                        return null;
                    }
                } else {
                    // Случай неверного выполнения запроса.
                    try {
                        ErrorDto errorDto = mErrorConverter.convert(response.errorBody());
                        switch (errorDto.getCode()) {
                            case 401:
                                result.setError("Яндекс переводчик: Неправильный API-ключ");
                                break;
                            case 402:
                                result.setError("Яндекс переводчик: API-ключ заблокирован");
                                break;
                            case 404:
                                result.setError("Яндекс переводчик: Превышено суточное ограничение " +
                                        "на объем переведенного текста");
                                break;
                            case 413:
                                result.setError("Яндекс переводчик: Превышен максимально " +
                                        "допустимый размер текста");
                                break;
                            case 422:
                                result.setError("Яндекс переводчик: Текст не может быть переведен");
                                break;
                            case 501:
                                result.setError("Яндекс переводчик: Заданное направление перевода " +
                                        "не поддерживается");
                                break;
                            default:
                                result.setError("Яндекс переводчик: Ошибка при работе с сервисом");
                        }
                    } catch (MalformedJsonException e) {
                        result.setError("Яндекс переводчик: Ошибка при работе с сетью");
                    }
                    cancel(false);
                    return result;
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Failed request to Yandex Translator service");
                result.setError("Яндекс переводчик: Ошибка при работе с сетью");
                cancel(false);
                return result;
            }

            // 3 этап: Выполнение запроса к Яндекс словарю.
            try {
                // Выполнение запроса к Яндекс словарю.
                Response<DictionaryDto> response = mDictionaryHttpService.getDictionaryEntry(
                        DICT_API_KEY,
                        translationState.getLanguage() + "-" +
                                translationState.getTranslationLanguage(),
                        UI_LANG,
                        translationState.getText()
                ).execute();
                if (response.isSuccessful()) {
                    // Заполнение модели перевода данными словаря.
                    response.body().fillTranlationModel(translation);
                } else {
                    // Некоторые ошибки словаря могут быть нормальным поведением, поэтому
                    // введена дополнительная переменная.
                    boolean isCorrect = false;
                    try {
                        ErrorDto errorDto = mErrorConverter.convert(response.errorBody());
                        switch (errorDto.getCode()) {
                            case 401:
                                result.setError("Яндекс словарь: Ключ API невалиден");
                                break;
                            case 402:
                                result.setError("Яндекс словарь: Ключ API заблокирован");
                                break;
                            case 403:
                                result.setError("Яндекс словарь: Превышено суточное ограничение " +
                                        "на количество запросов");
                                break;
                            case 413:
                                result.setError("Яндекс словарь: Превышен максимальный размер текста");
                                // В данном случае поведение корректно, так как если программа
                                // дошла до этого кода, значит Яндекс переводчик смог перевести
                                // текст данной длины и если словарь не поддерживает такую длину,
                                // то его ответ может не учитываться.
                                isCorrect = true;
                                break;
                            case 501:
                                result.setError("Яндекс словарь: Заданное направление перевода " +
                                        "не поддерживается");
                                // В данном случае поведение корректно, так как не все направления
                                // перевода Яндекс переводчика и Яндекс словаря могут совпадать.
                                // Если программа дошла до этого кода, значит переводчик со своей
                                // задачей справился и ответ словаря можно проигнорировать.
                                isCorrect = true;
                                break;
                            default:
                                result.setError("Яндекс словарь: Ошибка при работе с сервисом");
                        }
                    } catch (MalformedJsonException e) {
                        result.setError("Яндекс словарь: Ошибка при работе с сетью");
                    }
                    if (!isCorrect) {
                        cancel(false);
                        return result;
                    }
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Failed request to Yandex Dictionary service");
                result.setError("Яндекс словарь: Ошибка при работе с сетью");
                cancel(false);
                return result;
            }

            return result;
        }

        @Override
        protected void onPostExecute(TranslationResponse translationResponse) {
            onTranslationFinished(translationResponse.getTranslation());
        }

        @Override
        protected void onCancelled(TranslationResponse translationResponse) {
            mStatus = TranslationManagerFragment.Status.CANCELLED;
            publishError(translationResponse.getError());
        }
    }

    /**
     * Задание на сохранение перевода текста.
     */
    private class PreservingResultTask extends AsyncTask<Translation, Void, Void> {
        @Override
        protected Void doInBackground(Translation... params) {
            for (Translation translation : params) {
                mTranslationDao.addHistoryEntry(translation);
            }
            return null;
        }
    }
}
