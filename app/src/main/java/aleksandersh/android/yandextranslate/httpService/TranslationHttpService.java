package aleksandersh.android.yandextranslate.httpService;

import java.util.Map;

import aleksandersh.android.yandextranslate.dto.translation.LanguagesDto;
import aleksandersh.android.yandextranslate.dto.translation.TranslationDto;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Alexander on 14.04.2017.
 * <p>
 * Интерфейс, используемый в библиотеке Retrofit 2 для отправки запросов к сервису
 * Яндекс переводчика.
 */

public interface TranslationHttpService {
    /**
     * Метод для отправки POST запроса к сервису Яндекс переводчика на перевод текста.
     *
     * @param apiKey API-ключ для использования сервиса.
     * @param lang   Направление перевода.
     * @param text   Текст, который необходимо перевести.
     */
    @FormUrlEncoded
    @POST("translate")
    Call<TranslationDto> getTranslation(
            @Query("key") String apiKey,
            @Query("lang") String lang,
            @Field("text") String text
    );

    /**
     * Метод для отправки POST запроса к сервису Яндекс переводчика на получение списка языков.
     *
     * @param apiKey API-ключ для использования сервиса.
     * @param uiLang Язык пользовательского интерфейса.
     */
    @FormUrlEncoded
    @POST("getLangs")
    Call<LanguagesDto> getLanguages(
            @Field("key") String apiKey,
            @Field("ui") String uiLang
    );
}
