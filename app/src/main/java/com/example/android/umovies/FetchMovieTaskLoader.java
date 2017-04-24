package com.example.android.umovies;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.android.umovies.utilities.DataUtils;

import java.net.URL;
import java.util.List;


public class FetchMovieTaskLoader extends AsyncTaskLoader<List<Movie>> {
    private static final int FAVORITES_POSITION = 2; // Three Tabs from 0 to 2. Last one is Favorites
    private Context context;
    private List<Movie> moviesList;
    private int fragmentPosition;
    private Bundle args;

    public FetchMovieTaskLoader(Context context, Bundle args) {
        super(context);
        this.context = context;
        this.args = args;
    }

    @Override
    protected void onStartLoading() {
        if (args == null) {
            return;
        }

        fragmentPosition = args.getInt(MoviesFragment.FRAGMENT_POSITION);

        if (moviesList != null) {
            deliverResult(moviesList);
        } else {
            forceLoad();
        }
    }

    @Override
    public List<Movie> loadInBackground() {
        if(fragmentPosition == FAVORITES_POSITION) {
            moviesList = DataUtils.getFavoriteMoviesListFromSQLite();

            return moviesList;
        }
        else {
            URL url = DataUtils.getDBUrl(context, fragmentPosition, null);
            if (url == null) {
                return null;
            }

            try {
                String response = DataUtils.getResponseFromHTTP(context, url);
                moviesList = DataUtils.getMovieListDataFromJson(response);

                return moviesList;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public void deliverResult(List<Movie> movies) {
        moviesList = movies;
        super.deliverResult(movies);
    }
}
