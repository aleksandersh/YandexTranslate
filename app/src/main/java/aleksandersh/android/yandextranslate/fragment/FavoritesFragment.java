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
import java.util.Iterator;
import java.util.List;

import aleksandersh.android.yandextranslate.R;
import aleksandersh.android.yandextranslate.TranslationActivity;
import aleksandersh.android.yandextranslate.model.TranslationState;
import aleksandersh.android.yandextranslate.dao.TranslationDao;
import aleksandersh.android.yandextranslate.model.Translation;

/**
 * Created by Alexander on 22.04.2017.
 *
 * Фрагмент, который показывает список избранного.
 */

public class FavoritesFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<Translation>> {
    private static final String TAG = "FavoritesFragment";
    private static final int FAVORITES_LOADER_ID = 1;

    private TranslationActivity mTranslationActivity;
    private RecyclerView mFavoritesRecyclerView;
    private FavoritesAdapter mFavoritesAdapter;

    // Используется адаптером для вывода элементов на экран.
    private List<Translation> mTranslationList = new ArrayList<>(0);

    public FavoritesFragment() {
    }

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(FAVORITES_LOADER_ID, savedInstanceState, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        mFavoritesRecyclerView = (RecyclerView) view.findViewById(R.id.favorites_recycler_view);
        mFavoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mFavoritesAdapter = new FavoritesAdapter();
        mFavoritesRecyclerView.setAdapter(mFavoritesAdapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mFavoritesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();

        // После остановки фрагмента необходимо выполнить задание по обновлению флагов избранного.
        List<String> removeFromFavoritesList = null;
        Iterator<Translation> iterator = mTranslationList.iterator();
        while (iterator.hasNext()) {
            Translation translation = iterator.next();
            if (!translation.isFavorite()) {
                if (removeFromFavoritesList == null)
                    removeFromFavoritesList = new ArrayList<>();
                removeFromFavoritesList.add(String.valueOf(translation.getId()));
                iterator.remove();
            }
        }
        if (removeFromFavoritesList != null && !removeFromFavoritesList.isEmpty())
            new RemoveFromFavoritesTask().execute(removeFromFavoritesList);
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
        if (id == FAVORITES_LOADER_ID) {
            loader = new AsyncTaskLoader<List<Translation>>(getActivity()) {
                @Override
                public List<Translation> loadInBackground() {
                    return TranslationDao.get(getActivity()).getFavorites();
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
        mFavoritesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Translation>> loader) {
        mTranslationList.clear();
        mFavoritesAdapter.notifyDataSetChanged();
    }

    private class FavoritesHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Translation mTranslation;

        private ImageButton mSetFavoriteButton;
        private TextView mOriginalTextView;
        private TextView mTranslationTextView;
        private TextView mLangsTextView;

        public FavoritesHolder(View itemView) {
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
                    // При клике только изменяется картинка, основная работа происходит в событии
                    // остановки фрагмента.
                    mTranslation.setFavorite(!mTranslation.isFavorite());
                    if (!mTranslation.isFavorite()) {
                        mSetFavoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
                    } else {
                        mSetFavoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
                    }
                }
            });
        }

        @Override
        public void onClick(View v) {
            // При клике необходимо получить текущее состояние и передать его на показ.
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
        }
    }

    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesHolder> {
        @Override
        public FavoritesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            return new FavoritesHolder(
                    inflater.inflate(R.layout.translation_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(FavoritesHolder holder, int position) {
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
