package com.rahuljanagouda.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.rahuljanagouda.xyzreader.R;
import com.rahuljanagouda.xyzreader.data.ArticleLoader;
import com.rahuljanagouda.xyzreader.data.ItemsContract;
import com.rahuljanagouda.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends ActionBarActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private boolean mIsRefreshing = false;
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mContext = this;


        final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView itemCard;
        public com.rahuljanagouda.xyzreader.ui.DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (com.rahuljanagouda.xyzreader.ui.DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            itemCard = (CardView) view.findViewById(R.id.itemCard);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.itemCard.setVisibility(View.GONE);

            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));


            final Animation myFadeInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fadein);

            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    com.rahuljanagouda.xyzreader.ui.ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());

            com.rahuljanagouda.xyzreader.ui.ImageLoaderHelper.getInstance(mContext).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.THUMB_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {


                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                int mMutedColor = p.getDarkMutedColor(0xFF202020);
//                                int mMutedColor = p.getDarkMutedColor(0x000000);
                                holder.itemCard.startAnimation(myFadeInAnimation);
                                holder.itemCard.setBackgroundColor(mMutedColor);
                                holder.itemCard.setVisibility(View.VISIBLE);

                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            holder.itemCard.startAnimation(myFadeInAnimation);
                            holder.itemCard.setBackgroundColor(getResources().getColor(R.color.white));
                            holder.itemCard.setVisibility(View.VISIBLE);

                        }
                    });


            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }
}
