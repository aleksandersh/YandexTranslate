package aleksandersh.android.yandextranslate.fragment;

import android.content.Context;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import aleksandersh.android.yandextranslate.R;
import aleksandersh.android.yandextranslate.dao.TranslationDao;
import aleksandersh.android.yandextranslate.model.Language;

/**
 * Created by Alexander on 21.04.2017.
 */

public class LanguageSelectionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<Language>> {
    private static final int LANGUAGE_LOADER_ID = 1;
    private static final String ARG_CURRENT_LANG_SIGN = "current_lang_sign";

    private static final int ITEM_VIEW_TYPE_REGULAR = 1;
    private static final int ITEM_VIEW_TYPE_CURRENT = 2;

    private RecyclerView mLanguagesRecyclerView;
    private LanguageSelectionAdapter mLanguageSelectionAdapter;
    private LanguageHandler mLanguageHandler;
    private List<Language> mLanguageList = new ArrayList<>(0);
    private String mCurrentLanguage;

    public LanguageSelectionFragment() {
    }

    public static LanguageSelectionFragment newInstance(String currentLanguage) {
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_LANG_SIGN, currentLanguage);
        LanguageSelectionFragment fragment = new LanguageSelectionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentLanguage = getArguments().getString(ARG_CURRENT_LANG_SIGN);
        getLoaderManager().initLoader(LANGUAGE_LOADER_ID, savedInstanceState, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_language_selection, container, false);
        mLanguagesRecyclerView = (RecyclerView)
                view.findViewById(R.id.language_selection_recycler_view);
        mLanguagesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (mLanguageSelectionAdapter == null) {
            mLanguageSelectionAdapter = new LanguageSelectionAdapter();
            mLanguagesRecyclerView.setAdapter(mLanguageSelectionAdapter);
        } else {
            mLanguageSelectionAdapter.notifyDataSetChanged();
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LanguageHandler)
            mLanguageHandler = (LanguageHandler) context;
        else
            throw new ClassCastException("Context must implement LanguageHandler.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLanguageHandler = null;
    }

    @Override
    public Loader<List<Language>> onCreateLoader(int id, Bundle args) {
        Loader<List<Language>> loader = null;
        if (id == LANGUAGE_LOADER_ID) {
            loader = new AsyncTaskLoader<List<Language>>(getActivity()) {
                @Override
                public List<Language> loadInBackground() {
                    return TranslationDao.get(getActivity()).getLanguages();
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
    public void onLoadFinished(Loader<List<Language>> loader, List<Language> data) {
        mLanguageList = data;
        mLanguageSelectionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mLanguageList.clear();
        mLanguageSelectionAdapter.notifyDataSetChanged();
    }

    public interface LanguageHandler {
        void handleLanguage(Language language);
    }

    private class LanguageSelectionHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private Language mLanguage;
        private TextView mLanguageTextView;

        public LanguageSelectionHolder(View itemView) {
            super(itemView);
            mLanguageTextView = (TextView) itemView.findViewById(R.id.language_selection_text_view);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mLanguageHandler.handleLanguage(mLanguage);
        }

        public void bindLanguage(Language language) {
            mLanguage = language;
            mLanguageTextView.setText(language.getText());
        }
    }

    private class LanguageSelectionAdapter extends RecyclerView.Adapter<LanguageSelectionHolder> {
        @Override
        public LanguageSelectionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int resourceId;
            if (viewType == ITEM_VIEW_TYPE_CURRENT)
                resourceId = R.layout.language_selection_current_list_item;
            else
                resourceId = R.layout.language_selection_regular_list_item;
            return new LanguageSelectionHolder(inflater.inflate(resourceId, parent, false));
        }

        @Override
        public void onBindViewHolder(LanguageSelectionHolder holder, int position) {
            holder.bindLanguage(mLanguageList.get(position));
        }

        @Override
        public int getItemCount() {
            return mLanguageList.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (mLanguageList.get(position).getSign().equals(mCurrentLanguage))
                return ITEM_VIEW_TYPE_CURRENT;
            return ITEM_VIEW_TYPE_REGULAR;
        }
    }
}
