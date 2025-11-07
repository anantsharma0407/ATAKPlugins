package com.atakmap.android.helloworld.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;

public class PluginTemplateDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    private static final String TAG = "PluginDropDownReceiver";
    public static final String SHOW_PLUGIN = "com.atakmap.android.helloworld.SHOW_PLUGIN";

    private final Context pluginContext;
    private final PluginTemplatePane pane;

    public PluginTemplateDropDownReceiver(MapView mapView, Context context) {
        super(mapView);
        this.pluginContext = context;
        this.pane = new PluginTemplatePane(mapView);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.w(TAG, "onReceive: intent is null");
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "onReceive: intent=" + intent + " action=" + action);

        if (action == null) {
            Log.w(TAG, "onReceive: action is null, ignoring intent");
            return;
        }

        if (action.equals(SHOW_PLUGIN)) {
            Log.i(TAG, "onReceive: SHOW_PLUGIN received, showing drop-down");
            showDropDown(
                    pane.onCreateView(LayoutInflater.from(pluginContext)),
                    THREE_EIGHTHS_WIDTH,
                    FULL_HEIGHT,
                    FULL_WIDTH,
                    HALF_HEIGHT,
                    false,
                    this
            );
            Log.d(TAG, "onReceive: showDropDown called");
        } else {
            Log.d(TAG, "onReceive: unhandled action=" + action);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean visible) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
        pane.onDestroyView();
    }

    @Override
    public void disposeImpl() {
        pane.onDestroyView();
    }

    public PluginTemplatePane getPane() {
        return pane;
    }
}