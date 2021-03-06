package com.example.ray.popularmovies;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.ray.popularmovies.Data.Movie;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import okhttp3.OkHttpClient;

/**
 * Created by Ray on 12/29/2016.
 */

public class MovieDetailsFragment extends Fragment {

    private OkHttpClient mClient;
    private ArrayList<String> mTrailersURLs;
    private ListView mTrailers;
    private ArrayList<String> mReviewsList;
    private ListView mReviews;
    private final Activity a = getActivity();
    private String movieIdGlobal = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.movie_detail, container, false);

    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Utils utils = new Utils();
        View v = getView();
        final Activity a = getActivity();

        final Movie movie = utils.getMovieFromDB(movieIdGlobal, a);

        if (movie.getTitle() != a.getString(R.string.stub_movie_title)) {

            ImageView imageView = (ImageView) v.findViewById(R.id.movie_detail_poster);
            File imageFile = utils.getImageFromInternalStorage(a.getApplicationContext(), "", movie.getmPosterPath());
            Picasso.with(a.getApplicationContext()).load(imageFile).into(imageView);

            final ToggleButton isFavoriteButton = (ToggleButton) v.findViewById(R.id.isFavorite);

            int currentState = movie.ismInFavorites();
            int x = movie.ismInFavorites() == 1 ? R.drawable.star_on : R.drawable.star_off;
            isFavoriteButton.setBackgroundResource(x);
            isFavoriteButton.setVisibility(View.VISIBLE);
            isFavoriteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    MoviesProvider.DatabaseHelper y = new MoviesProvider.DatabaseHelper(a.getApplicationContext());
                    SQLiteDatabase db = y.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    String uri = MoviesProvider.MOVIE_URI + movie.getmId();
                    if (isChecked) {
                        isFavoriteButton.setBackgroundResource(R.drawable.star_on);
                        values.put(MoviesProvider.IS_FAVORITE, 1);
                        a.getContentResolver().update(Uri.parse(uri), values, null, null);
                    } else {
                        isFavoriteButton.setBackgroundResource(R.drawable.star_off);
                        values.put(MoviesProvider.IS_FAVORITE, 0);
                        a.getContentResolver().update(Uri.parse(uri), values, null, null);
                    }
                }
            });

            TextView titleView = (TextView) v.findViewById(R.id.movie_detail_title);
            titleView.setText(movie.getTitle());

            TextView releaseDateView = (TextView) v.findViewById(R.id.movie_detail_release_date);
            releaseDateView.setText(movie.getmReleaseDate());

            TextView voteView = (TextView) v.findViewById(R.id.movie_detail_vote_average);
            voteView.setText(Double.toString(movie.getmPopularity()));

            TextView synopsisView = (TextView) v.findViewById(R.id.movie_detail_plot_synopsis);
            synopsisView.setText(movie.getmOverview());

            mTrailersURLs = new ArrayList<>();
            mClient = new OkHttpClient();
            new GetTrailersFromDB().execute(movie.getmId().toString());

            mTrailers = (ListView) v.findViewById(R.id.movie_detail_trailers_list);
            mReviews = (ListView) v.findViewById(R.id.movie_detail_reviews_list);

            mTrailers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v,
                                        int position, long id) {
                    String movieId = mTrailersURLs.get(position);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + movieId)));
                }
            });

            mReviewsList = new ArrayList<String >();
            new GetReviewsFromDB().execute(movie.getmId().toString());


        }

    }

    public void setMovieIdGlobal(String incomingMovieId) {
        movieIdGlobal = incomingMovieId;
    }

    class GetTrailersFromDB extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(String content) {
            mTrailersURLs.clear();
            Uri trailers = Uri.parse(MoviesProvider.GET_TRAILERS_URI + content);
            Cursor c = getActivity().getApplicationContext().getContentResolver().query(
                    trailers, null, null, null, null);

            if (c != null && c.moveToFirst()) {
                do {
                    mTrailersURLs.add(c.getString(c.getColumnIndex(MoviesProvider.KEY)));
                } while (c.moveToNext());
            }

            TrailersListAdapter adapter = new TrailersListAdapter(
                    getActivity().getApplicationContext(), R.id.movie_detail_trailers_list, mTrailersURLs
            );
            mTrailers.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    class GetReviewsFromDB extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(String content) {
            mReviewsList.clear();
            Uri reviews = Uri.parse(MoviesProvider.GET_REVIEWS_URI + content);
            Cursor c = getActivity().getApplicationContext().getContentResolver().query(
                    reviews, null, null, null, null);

            if (c != null && c.moveToFirst()) {
                do {
                    mReviewsList.add(c.getString(c.getColumnIndex(MoviesProvider.CONTENT)) + "\n \n"
                            + c.getString(c.getColumnIndex(MoviesProvider.AUTHOR)));
                } while (c.moveToNext());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.reviews_list_item, mReviewsList);
            mReviews.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }
}
