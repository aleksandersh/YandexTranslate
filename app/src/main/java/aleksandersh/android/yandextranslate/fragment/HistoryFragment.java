package aleksandersh.android.yandextranslate.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import aleksandersh.android.yandextranslate.R;
import aleksandersh.android.yandextranslate.TranslationActivity;
import aleksandersh.android.yandextranslate.model.TranslationState;
import aleksandersh.android.yandextranslate.dao.TranslationDao;
import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 23.04.2017.
 */

public class HistoryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<Translation>> {
    private static final String TAG = "HistoryFragment";
    private static final int HISTORY_LOADER_ID = 2;

    private TranslationActivity mTranslationActivity;
    private RecyclerView mHistoryRecyclerView;
    private HistoryAdapter mHistoryAdapter;
    private List<Translation> mTranslationList = new ArrayList<>(0);
    private List<String> mRemoveFromFavoritesList = new ArrayList<>();

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(HISTORY_LOADER_ID, savedInstanceState, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        mHistoryRecyclerView = (RecyclerView) view.findViewById(R.id.history_recycler_view);
        mHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mHistoryAdapter = new HistoryAdapter();
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mRemoveFromFavoritesList.isEmpty()) {
            new RemoveFromFavoritesTask().execute(mRemoveFromFavoritesList);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TranslationActivity)
            mTranslationActivity = (TranslationActivity) context;
        else
            throw new ClassCastException("Context must implement TranslationActivity.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTranslationActivity = null;
    }

    @Override
    public Loader<List<Translation>> onCreateLoader(int id, Bundle args) {
        Loader<List<Translation>> loader = null;
        if (id == HISTORY_LOADER_ID) {
            loader = new AsyncTaskLoader<List<Translation>>(getActivity()) {
                @Override
                public List<Translation> loadInBackground() {
                    return TranslationDao.get(getActivity()).getHistory();
                }

                @Override
                protected void onStartLoading() {
                    forceLoad();
                }

                @Override
                protected void onStopLoading() {
                    cancelLoad();
                }
            };
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<List<Translation>> loader, List<Translation> data) {
        mTranslationList = data;
        mHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Translation>> loader) {
        mTranslationList.clear();
        mHistoryAdapter.notifyDataSetChanged();
    }

    private class HistoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Translation mTranslation;

        private ImageButton mSetFavoriteButton;
        private TextView mOriginalTextView;
        private TextView mTranslationTextView;
        private TextView mLangsTextView;

        public HistoryHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            mOriginalTextView = (TextView)
                    itemView.findViewById(R.id.translation_list_item_original_text_view);
            mTranslationTextView = (TextView)
                    itemView.findViewById(R.id.translation_list_item_translation_text_view);
            mLangsTextView = (TextView)
                    itemView.findViewById(R.id.translation_list_item_langs_text_view);

            mSetFavoriteButton = (ImageButton)
                    itemView.findViewById(R.id.translation_list_item_set_favorite_image_button);
            mSetFavoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTranslation.setFavorite(!mTranslation.isFavorite());
                    TranslationState translationState = new TranslationState(
                            mTranslation.getOriginalText(),
                            mTranslation.getPrimaryLanguage(),
                            mTranslation.getTargetLanguage()
                    );
                    translationState.setFavorite(mTranslation.isFavorite());
                    if (!mTranslation.isFavorite()) {
                        // Если элемент помечается как не избранный, его Id необходимо добавить в
                        // соответствующий список.
                        mSetFavoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
                        TranslationDao.get(getActivity()).setFavorite(translationState);
                    } else {
                        // Иначе элемент из списка удаляется.
                        mSetFavoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
                        TranslationDao.get(getActivity()).setFavorite(translationState);
                    }
                }
            });
        }

        @Override
        public void onClick(View v) {
            TranslationState translationState = new TranslationState(
                    mTranslation.getOriginalText(),
                    mTranslation.getPrimaryLanguage(),
                    mTranslation.getTargetLanguage()
            );
            translationState.setFavorite(mTranslation.isFavorite());
            translationState.setLanguageText(TranslationDao.get(getActivity())
                    .getLanguageBySign(translationState.getLanguage()).getText());
            translationState.setTranslationLanguageText(TranslationDao.get(getActivity())
                    .getLanguageBySign(translationState.getTranslationLanguage()).getText());
            mTranslationActivity.showTranslation(translationState);
        }

        public void bindTranslation(Translation translation) {
            mTranslation = translation;
            mOriginalTextView.setText(translation.getOriginalText());
            mTranslationTextView.setText(translation.getTranslationText());
            mLangsTextView.setText(translation.getPrimaryLanguage() + " - "
                    + translation.getTargetLanguage());
            if (translation.isFavorite())
                mSetFavoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
            else
                mSetFavoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {
        @Override
        public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            return new HistoryHolder(
                    inflater.inflate(R.layout.translation_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(HistoryHolder holder, int position) {
            holder.bindTranslation(mTranslationList.get(position));
        }

        @Override
        public int getItemCount() {
            return mTranslationList.size();
        }
    }

    private class RemoveFromFavoritesTask extends AsyncTask<List<String>, Void, Void> {
        @Override
        protected Void doInBackground(List<String>... params) {
            if (params.length < 0) {
                cancel(false);
                return null;
            }
            TranslationDao.get(getActivity()).removeFromFavorites(params[0]);
            return null;
        }
    }
}
