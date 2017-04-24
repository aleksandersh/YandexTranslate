package aleksandersh.android.yandextranslate.httpService;

import aleksandersh.android.yandextranslate.dto.dictionary.DictionaryDto;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by Alexander on 17.04.2017.
 */

public interface DictionaryHttpService {
    /**
     * Осуществляет поиск слова или фразы в Яндекс словаре.
     *
     * @param apiKey API-ключ Яндекс словаря.
     * @param lang Направление перевода.
     * @param ui Язык пользовательского интерфейса.
     * @param text Искомые слово или фраза.
     */
    @FormUrlEncoded
    @POST("lookup")
    Call<DictionaryDto> getDictionaryEntry(
            @Field("key") String apiKey,
            @Field("lang") String lang,
            @Field("ui") String ui,
            @Field("text") String text
    );
}
