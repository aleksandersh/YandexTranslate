package aleksandersh.android.yandextranslate.dto.dictionary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import aleksandersh.android.yandextranslate.model.Dictionary;
import aleksandersh.android.yandextranslate.model.Mean;
import aleksandersh.android.yandextranslate.model.Synonym;
import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 17.04.2017.
 * <p>
 * Объект передачи данных, для получения ответа от сервиса Яндекс словаря.
 */

public class DictionaryDto {
    // Заголовок результата (не используется).
    @SerializedName("head")
    @Expose
    private DictionaryHeadDto mHead;
    // Словарные статьи.
    @SerializedName("def")
    @Expose
    private List<DictionaryDefinitionDto> mDefinitions = null;

    public DictionaryHeadDto getHead() {
        return mHead;
    }

    public void setHead(DictionaryHeadDto head) {
        this.mHead = head;
    }

    public List<DictionaryDefinitionDto> getDefinitions() {
        return mDefinitions;
    }

    public void setDefinitions(List<DictionaryDefinitionDto> def) {
        this.mDefinitions = def;
    }

    /**
     * Загружает данные в модель перевода.
     *
     * @param translation Объект, в который загружаются данные.
     */
    public void fillTranlationModel(Translation translation) {
        List<Dictionary> dictionaryList = translation.getDictionaryDefinitions();
        dictionaryList.clear();

        if (mDefinitions != null) {
            for (DictionaryDefinitionDto definitionDto : mDefinitions) {
                List<DictionaryTranslationDto> translationDtoList = definitionDto.getTranslations();
                if (translationDtoList != null) {
                    for (DictionaryTranslationDto translationDto : translationDtoList) {
                        Dictionary dictionary = new Dictionary();
                        dictionary.setText(definitionDto.getText());
                        dictionary.setTranscription(definitionDto.getTranscription());
                        dictionary.setPartOfSpeech(definitionDto.getPartOfSpeech());
                        dictionary.setTranslationText(translationDto.getText());

                        List<DictionarySynonymDto> synonymDtoList = translationDto.getSynonyms();
                        if (synonymDtoList != null) {
                            List<Synonym> synonymList = dictionary.getSynonyms();
                            for (DictionarySynonymDto synonymDto : synonymDtoList) {
                                Synonym synonym = new Synonym();
                                synonym.setText(synonymDto.getText());
                                synonymList.add(synonym);
                            }
                        }

                        List<DictionaryMeanDto> meanDtoList = translationDto.getMeans();
                        if (meanDtoList != null) {
                            List<Mean> meanList = dictionary.getMeans();
                            for (DictionaryMeanDto meanDto : meanDtoList) {
                                Mean mean = new Mean();
                                mean.setText(meanDto.getText());
                                meanList.add(mean);
                            }
                        }

                        dictionaryList.add(dictionary);
                    }
                }
            }
        }
    }
}
