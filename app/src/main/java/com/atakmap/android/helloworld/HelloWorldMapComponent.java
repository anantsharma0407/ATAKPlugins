package com.atakmap.android.helloworld;

import android.content.Context;
import android.content.Intent;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.plugin.PluginTemplateDropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

public class HelloWorldMapComponent extends DropDownMapComponent {

    public static final String TAG = "HelloWorldMapComponent";

    private Context pluginContext;
    private PluginTemplateDropDownReceiver pluginTemplateReceiver;

    @Override
    public void onCreate(final Context context, Intent intent, final MapView view) {
        // Set the ATAK theme for consistent UI
        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        // Create and register the plugin dropdown receiver
        Log.d(TAG, "Creating PluginTemplateDropDownReceiver");
        this.pluginTemplateReceiver = new PluginTemplateDropDownReceiver(view, context);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(PluginTemplateDropDownReceiver.SHOW_PLUGIN,
                "Show the Plugin Template drop-down");

        this.registerDropDownReceiver(this.pluginTemplateReceiver, filter);
        Log.d(TAG, "Registered PluginTemplateDropDownReceiver with action: " + PluginTemplateDropDownReceiver.SHOW_PLUGIN);
    }

    @Override
    public void onStart(final Context context, final MapView view) {
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause(final Context context, final MapView view) {
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(final Context context, final MapView view) {
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop(final Context context, final MapView view) {
        Log.d(TAG, "onStop");
    }

    /**
     * Get the plugin template dropdown receiver
     */
    public PluginTemplateDropDownReceiver getPluginTemplateReceiver() {
        return pluginTemplateReceiver;
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "onDestroy - cleaning up receiver");

        if (pluginTemplateReceiver != null) {
            pluginTemplateReceiver.disposeImpl();
            pluginTemplateReceiver = null;
        }

        super.onDestroyImpl(context, view);
    }
}