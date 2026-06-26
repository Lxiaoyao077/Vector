package org.lsposed.manager;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.lsposed.manager.ui.activity.MainActivity;
import org.lsposed.manager.util.ModuleUtil;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        try {
            var moduleUtil = ModuleUtil.getInstance();
            if (moduleUtil != null) {
                var modules = moduleUtil.getModules();
                int count = modules != null ? modules.size() : 0;
                tile.setLabel(getString(R.string.app_name));
                tile.setSubtitle(String.format(Locale.getDefault(),
                        "%d module%s loaded", count, count != 1 ? "s" : ""));
            } else {
                tile.setLabel(getString(R.string.app_name));
                tile.setSubtitle("Not available");
            }
        } catch (Exception e) {
            tile.setLabel(getString(R.string.app_name));
            tile.setSubtitle("Not available");
        }

        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
