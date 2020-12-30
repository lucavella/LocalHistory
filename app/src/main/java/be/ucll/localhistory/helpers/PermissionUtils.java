package be.ucll.localhistory.helpers;

import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;



public abstract class PermissionUtils {

    public enum PermissionStatus {
        GRANTED,
        DENIED,
        DONT_ASK_OR_NEVER_ASKED, // if never ask again was selected or never asked before
        UNKNOWN,
    }

    public static void requestPermission(AppCompatActivity activity, int requestId,
                                         String permission, boolean overrideRationale) {
        // only request permission if overrideRationale is true or if never asked permission before
        // (will also ask if never ask again was selected but request will be denied automatically)
        if (overrideRationale ||
                (getPermissionStatus(activity, permission) == PermissionStatus.DONT_ASK_OR_NEVER_ASKED)) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestId);
        }
    }

    public static PermissionStatus getPermissionStatus(AppCompatActivity activity, String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            return PermissionStatus.DENIED;
        } else {
            if (ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                return PermissionStatus.GRANTED;
            } else {
                return PermissionStatus.DONT_ASK_OR_NEVER_ASKED;
            }
        }
    }
}