package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

public class StockWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_stock_list_item);
                String symbol = data.getString(1);
                views.setTextViewText(R.id.widget_symbol, symbol);
                views.setContentDescription(R.id.widget_symbol,symbol);
                
                if (data.getInt(data.getColumnIndex(getString(R.string.is_up))) == 1) {
                    views.setInt(R.id.widget_change, getString(R.string.background_resource), R.drawable.percent_change_pill_green);
                } else {
                    views.setInt(R.id.widget_change, getString(R.string.background_resource), R.drawable.percent_change_pill_red);
                }
                if (Utils.showPercent) {
                    views.setTextViewText(R.id.widget_change, data.getString(data.getColumnIndex(getString(R.string.percent_change))));
                    if (data.getInt(data.getColumnIndex(getString(R.string.is_up))) == 1) {
                        views.setContentDescription(R.id.widget_change,getString(R.string.change_up) + data.getString(data.getColumnIndex(getString(R.string.percent_change))));
                    } else {
                        views.setContentDescription(R.id.widget_change,getString(R.string.change_down) + data.getString(data.getColumnIndex(getString(R.string.percent_change))));
                    }
                } else {
                    views.setTextViewText(R.id.widget_change, data.getString(data.getColumnIndex(getString(R.string.change))));
                    if (data.getInt(data.getColumnIndex(getString(R.string.is_up))) == 1) {
                        views.setContentDescription(R.id.widget_change,getString(R.string.change_up) + data.getString(data.getColumnIndex(getString(R.string.change))));
                    } else {
                        views.setContentDescription(R.id.widget_change,getString(R.string.change_down) + data.getString(data.getColumnIndex(getString(R.string.change))));
                    }
                }

                Bundle extras = new Bundle();
                extras.putString(getString(R.string.intent_extra_symbol), symbol);

                Intent fillInIntent = new Intent();
                fillInIntent.putExtras(extras);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_stock_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(0);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
