package com.example.android.umovies;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.umovies.Transformations.BlurTransformation;
import com.example.android.umovies.utilities.DataUtils;
import com.example.android.umovies.utilities.ImageUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailsActivity extends AppCompatActivity implements
        FetchSingleMovieTaskCompleteListener<Movie>,
        LoaderManager.LoaderCallbacks<Movie> {
    private static final int BLUR_RADIUS = 25;
    private static final int TOTAL_COUNT_RATING_STARS = 5;
    private static final String MOVIE_REVIEW_LOADER_ID = "ReviewsLoaderId";
    @BindView(R.id.ll_container) FrameLayout movieContainer;
    @BindView(R.id.iv_blur_img) ImageView blurImage;
    @BindView(R.id.iv_tumbnail_img) ImageView movieImage;
    @BindView(R.id.tv_rating) TextView ratingView;
    @BindView(R.id.tv_movie_name) TextView titleView;
    @BindView(R.id.tv_movie_release_date) TextView releaseDateView;
    @BindView(R.id.tv_movie_synopsis) TextView synopsisView;
    @BindView(R.id.tv_votes) TextView votesView;
    @BindView(R.id.tv_movie_runtime) TextView runtimeView;
    @BindView(R.id.tv_movie_revenue) TextView revenueView;
    @BindView(R.id.tv_movie_tagline) TextView taglineView;
    @BindView(R.id.tv_movie_genres) TextView genresView;
    @BindView(R.id.ll_movie_reviews) LinearLayout reviewsView;
    private Movie movie;
    private int moviePos;
    private LoaderManager loaderManager;
    private int loaderId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details);
        ButterKnife.bind(this);

        initView();
        initLoader();
        getSavedInstanceStates(savedInstanceState);
        getFromExtras();
        populateData(movie);
        populateFromNetwork();
        fetchData(movie.getId()+ "/reviews");
    }

    private void populateData(Movie movie) {
        if(movie != null) {
            String imgUrl = movie.getImageURL();
            String fullUrl = ImageUtils.getImageUrl(imgUrl);

            loadBlurBgImage(fullUrl);
            Picasso.with(this)
                    .load(fullUrl)
                    .fit()
                    .into(movieImage);
            titleView.setText(movie.getTitle());
            votesView.setText("(" + movie.getVotes() + " votes)");
            setRatingStars(movie.getRating());
            ratingView.setText(getRating(movie.getRating()));
            releaseDateView.setText(movie.getReleaseDate());
            synopsisView.setText(movie.getSynopsis());

            if(movie.isFullyUpdated()) {
                runtimeView.setText(getRuntime(movie.getRuntime()));
                revenueView.setText(getRevenue(movie.getRevenue()));
                taglineView.setText(movie.getTagline());
                genresView.setText(getGenres(movie.getGenres()));
            }
        }
    }

    private void populateFromNetwork() {
        if(DataUtils.isOnline(this)) {
            if (movie != null && !movie.isFullyUpdated()) {
                String movieId = movie.getId();
                new FetchSingleMovieTask(this, this, movieId, movie).execute();
            }
        }
        else {
            DataUtils.showSnackbarMessage(this, movieContainer, getResources().getString(R.string.no_network));
        }
    }

    private void loadBlurBgImage(String fullUrl) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Picasso.with(this)
                    .load(fullUrl)
                    .fit()
                    .transform(new BlurTransformation(this, BLUR_RADIUS))
                    .into(blurImage);
        }
    }

    private void getFromExtras() {
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Movie movie = bundle.getParcelable(MoviesFragment.MOVIE_OBJ);
            if(movie != null) {
                this.movie = movie;
                Log.v(MainActivity.TAG, "id = " + movie.getId());
            }
            else {
                this.movie = null;
            }

            moviePos = bundle.getInt(MoviesFragment.MOVIE_POS, -1);
        }
    }

    private void initView() {
        if(Build.VERSION.SDK_INT >= 21) {
            movieContainer.setPadding(0, (int)getResources().getDimension(R.dimen.padding_from_top_toolbar), 0, 0);
        }
    }

    @Override
    public void onTaskCompleted(Movie movie) {
        if(movie != null) {
            runtimeView.setText(getRuntime(movie.getRuntime()));
            revenueView.setText(getRevenue(movie.getRevenue()));
            taglineView.setText(movie.getTagline());
            genresView.setText(getGenres(movie.getGenres()));
            DataUtils.updateMovie(movie, moviePos);
        }
    }

    private String getRuntime(String original) {
        int runtimeNum = Integer.valueOf(original);
        int hours = Math.round(runtimeNum/60);
        int minutes = runtimeNum - (hours*60);

        return hours + " hr. " + minutes + " min.";
    }

    private String getRevenue(String original) {
        int billion = 1000000000;
        int million = 1000000;
        int thousand = 1000;
        int revenueAmount = Integer.valueOf(original);
        double result;
        String postfix;

        if(revenueAmount/billion > 0) {
            result = revenueAmount/(double)billion;
            postfix = "B";
        }
        else if(revenueAmount/million > 0) {
            result = revenueAmount/(double)million;
            postfix = "M";
        }
        else if(revenueAmount/thousand > 0) {
            result = revenueAmount/(double)thousand;
            postfix = "k";
        }
        else {
            result = revenueAmount;
            postfix = "";
        }

        result = (double)Math.round(result * 10d) / 10d;

        if(isZeroAfterFloatingPoint(result)){
            return "$" + (int)result + postfix;
        }

        return "$" + result + postfix;
    }

    private String getGenres(List<String> genres) {
        StringBuilder genresStr = new StringBuilder();

        for(String g : genres) {
            genresStr.append(g + "    ");
        }

        return genresStr.toString();
    }

    private String getRating(String rating) {
        double ratingVal = Double.valueOf(rating);
        double result = (double)Math.round((ratingVal/2) * 10d) / 10d;
        String ratingStr;

        if(isZeroAfterFloatingPoint(result)) {
            ratingStr = String.valueOf((int) result);
        }
        else {
            ratingStr = String.valueOf(result);
        }

        return ratingStr + "/" + TOTAL_COUNT_RATING_STARS;
    }

    private void setRatingStars(String rating) {
        double ratingNumValue = Double.valueOf(rating);

        inflateRatingStars(ratingNumValue);
    }

    private void inflateRatingStars(double ratingStarsCount) {
        int total = TOTAL_COUNT_RATING_STARS;
        LinearLayout ratingStarsContainer = (LinearLayout) findViewById(R.id.ll_stars_container);
        int fullStarsCount = ((int)ratingStarsCount)/2;

        for(int i = 0; i < fullStarsCount; i++) {
            addImageViewToLayout(ratingStarsContainer, R.mipmap.full_star);
            total--;
        }

        double halfStarsCount = ratingStarsCount - (double)fullStarsCount/2;
        if(halfStarsCount > 0){
            addImageViewToLayout(ratingStarsContainer, R.mipmap.half_star);
            total--;
        }

        for(int i = 0; i < total; i++) {
            addImageViewToLayout(ratingStarsContainer, R.mipmap.empty_star);
        }

        ratingStarsContainer.invalidate();
    }

    private void addImageViewToLayout(LinearLayout container, int resId) {
        float starSize = getResources().getDimension(R.dimen.star_size);
        float paddingRight = getResources().getDimension(R.dimen.star_padding);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)starSize, (int)starSize);

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(resId);
        imageView.setPadding(0, 0, (int)paddingRight, 0);
        imageView.setLayoutParams(layoutParams);
        container.addView(imageView);
    }

    private boolean isZeroAfterFloatingPoint(double val) {
        double res = val - Math.floor(val);
        res = (res%1.0)*10;

        if(res == 0)
            return true;

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(MOVIE_REVIEW_LOADER_ID, loaderId);
        super.onSaveInstanceState(outState);
    }

    private void getSavedInstanceStates(Bundle savedInstanceState) {
        updateLoaderId(savedInstanceState);
    }

    private void updateLoaderId(Bundle savedInstanceState) {
        if(savedInstanceState != null && savedInstanceState.containsKey(MOVIE_REVIEW_LOADER_ID)) {
            loaderId = savedInstanceState.getInt(MOVIE_REVIEW_LOADER_ID);
        }
        else {
            int id = getIntBundleExtra(MOVIE_REVIEW_LOADER_ID);
            loaderId = id;
        }
    }

    private int getIntBundleExtra(String extraName) {
        Bundle bundle = getIntent().getExtras();
        return bundle.getInt(extraName);
    }

    private void initLoader() {
        loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(loaderId, null, this);
    }

    private void fetchData(String path) {
        loadFetchedMovieReviews(path);
    }

    private void loadFetchedMovieReviews(String path) {
        Bundle bundle = new Bundle();
        bundle.putString("path", path);
        bundle.putParcelable("movie", movie);

        Loader<Movie> loader = getLoader();
        if(loader == null) {
            loaderManager.initLoader(loaderId, bundle, this);
        }
        else {
            loaderManager.restartLoader(loaderId, bundle, this);
        }
    }

    private Loader<Movie> getLoader() {
        return loaderManager.getLoader(loaderId);
    }

    @Override
    public Loader<Movie> onCreateLoader(int id, Bundle args) {
        return new FetchMovieReviewsTaskLoader(this, args);
    }

    @Override
    public void onLoadFinished(Loader<Movie> loader, Movie movie) {
        int size = movie.getReviewAuthor().size();
        int i = 0;
        if(size == 0) {
            TextView tv = createTextView("", false);
            reviewsView.addView(tv);
        }
        else {
            while (i < size) {
                String author = movie.getReviewAuthor().get(i);
                String content = movie.getReviewContent().get(i);
                String rating = movie.getReviewRating().get(i);

                TextView tv = createTextView(author, true);
                reviewsView.addView(tv);

                TextView tv1 = createTextView(content + "\n" + rating, false);
                reviewsView.addView(tv1);

                i++;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Movie> loader) {
    }

    private TextView createTextView(String text, boolean isTitle) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView tv = new TextView(this);
        tv.setLayoutParams(lparams);
        if(isTitle) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.txt_large_size));
            tv.setTextColor(getResources().getColor(R.color.secondary_color));
        }
        else {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.txt_regular_size));
            tv.setTextColor(getResources().getColor(R.color.main_text_color));
        }
        tv.setText(text);

        return tv;
    }
}
